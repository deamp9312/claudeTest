package com.example.server5.kafka.consumer;

import com.example.server5.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    /**
     * ✅ 최적화 1: 배치 컨슈밍
     * List<ConsumerRecord> 로 받아 한 번에 처리
     * → DB 배치 INSERT/UPDATE, 개별 처리 대비 10배 이상 처리량 향상
     */
    @KafkaListener(
        topics = "${spring.kafka.topics.order-events}",
        groupId = "server5-order-group",
        containerFactory = "kafkaListenerContainerFactory",
        // ✅ 최적화 2: concurrency로 병렬 컨슈머 (파티션 수와 맞춤)
        concurrency = "3"
    )
    @Transactional
    public void consumeOrderEvents(
        List<ConsumerRecord<String, OrderEvent>> records,
        Acknowledgment ack  // 수동 커밋
    ) {
        log.info("Batch received: {} records", records.size());

        try {
            // ✅ 최적화 3: 헤더로 빠른 필터링 (페이로드 처리 전에 헤더만 검사)
            List<ConsumerRecord<String, OrderEvent>> filtered = records.stream()
                .filter(record -> {
                    var header = record.headers().lastHeader("eventType");
                    if (header == null) return true;
                    String eventType = new String(header.value(), StandardCharsets.UTF_8);
                    // ORDER_CANCELLED 이벤트는 별도 토픽으로 라우팅하므로 여기서 스킵
                    return !eventType.equals("ORDER_CANCELLED");
                })
                .toList();

            // ✅ 최적화 4: 배치 단위로 비즈니스 처리
            processBatch(filtered);

            // ✅ 최적화 5: 배치 전체 처리 완료 후 한 번에 커밋
            //    개별 커밋 대비 브로커 부하 대폭 감소
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Batch processing failed: {}", e.getMessage(), e);
            // 실패 시 커밋하지 않음 → 재처리 (at-least-once 보장)
            // 실제 운영에서는 DLQ(Dead Letter Queue)로 이동 권장
        }
    }

    private void processBatch(List<ConsumerRecord<String, OrderEvent>> records) {
        records.forEach(record -> {
            OrderEvent event = record.value();
            log.info("Processing: orderId={}, type={}, partition={}, offset={}",
                event.getOrderId(),
                event.getEventType(),
                record.partition(),
                record.offset()
            );
            // 실제 비즈니스 로직 처리 (예: DB 업데이트, 알림 발송 등)
        });
    }

    /**
     * ✅ 최적화 6: 단건 처리용 리스너 (컨슈머 그룹 분리)
     * 배치 컨슈머와 다른 그룹 → 독립적으로 오프셋 관리
     */
    @KafkaListener(
        topics = "${spring.kafka.topics.order-events}",
        groupId = "server5-notification-group"  // 알림 전용 컨슈머 그룹
    )
    public void consumeForNotification(
        ConsumerRecord<String, OrderEvent> record,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition
    ) {
        OrderEvent event = record.value();
        log.info("Notification group - orderId={}, partition={}", event.getOrderId(), partition);
        // 알림 발송 로직
    }
}
