package com.example.server5.kafka.producer;

import com.example.server5.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Value("${spring.kafka.topics.order-events}")
    private String orderEventsTopic;

    /**
     * ✅ 최적화 1: 파티션 키 설계
     * userId를 키로 사용 → 같은 유저의 이벤트는 항상 같은 파티션으로
     * → 순서 보장 + 컨슈머 로컬 캐시 활용 가능
     */
    @Async
    public CompletableFuture<SendResult<String, OrderEvent>> sendOrderEvent(OrderEvent event) {
        String partitionKey = String.valueOf(event.getUserId());

        // ✅ 최적화 2: 헤더에 메타데이터 → 페이로드 크기 절감
        //    컨슈머가 헤더만 보고 처리 여부 결정 가능 (페이로드 역직렬화 불필요)
        ProducerRecord<String, OrderEvent> record = new ProducerRecord<>(
            orderEventsTopic,
            null,           // 파티션 직접 지정 안 함 (키 기반 자동 배정)
            partitionKey,   // 파티션 키
            event
        );
        record.headers().add(new RecordHeader("eventType",
            event.getEventType().name().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("version",
            "v1".getBytes(StandardCharsets.UTF_8)));

        CompletableFuture<SendResult<String, OrderEvent>> future =
            kafkaTemplate.send(record).toCompletableFuture();

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Event sent: topic={}, partition={}, offset={}, key={}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(),
                    partitionKey);
            } else {
                log.error("Failed to send event: {}", ex.getMessage());
            }
        });

        return future;
    }

    /**
     * ✅ 최적화 3: 배치 전송
     * 여러 이벤트를 한 번에 전송 → 네트워크 왕복 횟수 감소
     * Kafka 프로듀서 내부 배치 버퍼와 결합하면 효율 극대화
     */
    @Async
    public void sendBatch(List<OrderEvent> events) {
        events.forEach(event -> kafkaTemplate.send(
            orderEventsTopic,
            String.valueOf(event.getUserId()),
            event
        ));
        // ✅ flush: 버퍼에 남은 메시지 즉시 전송
        kafkaTemplate.flush();
        log.info("Batch sent: {} events", events.size());
    }
}
