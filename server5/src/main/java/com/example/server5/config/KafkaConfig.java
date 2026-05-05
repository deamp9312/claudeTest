package com.example.server5.config;

import com.example.server5.dto.OrderEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ===================== PRODUCER 설정 =====================

    @Bean
    public ProducerFactory<String, OrderEvent> orderEventProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // ✅ 메시지 압축: LZ4 (빠름), snappy (균형), gzip (높은 압축률)
        //    평균 30~70% 크기 절감
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");

        // ✅ 배치 전송: 16KB 쌓이면 한 번에 전송 (개별 전송 대비 처리량 대폭 증가)
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);

        // ✅ linger.ms: 배치가 안 차도 1ms 후 전송 (지연 vs 처리량 트레이드오프)
        config.put(ProducerConfig.LINGER_MS_CONFIG, 1);

        // ✅ 버퍼 메모리: 32MB (프로듀서 내부 버퍼)
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        // ✅ 멱등성: 네트워크 재시도 시 중복 메시지 방지
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // ✅ acks=all: 모든 복제본이 확인해야 성공 (데이터 유실 방지)
        config.put(ProducerConfig.ACKS_CONFIG, "all");

        // ✅ 재시도: 일시적 오류 시 자동 재시도
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, OrderEvent> orderEventKafkaTemplate() {
        return new KafkaTemplate<>(orderEventProducerFactory());
    }

    // ===================== CONSUMER 설정 =====================

    @Bean
    public ConsumerFactory<String, OrderEvent> orderEventConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "server5-order-group");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // ✅ 한 번에 처리할 최대 레코드 수 (배치 처리 최적화)
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);

        // ✅ fetch.min.bytes: 1KB 이상 쌓일 때까지 대기 (불필요한 빈 폴 방지)
        config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);

        // ✅ fetch.max.wait.ms: 최대 500ms 대기
        config.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);

        // ✅ 수동 커밋: 처리 완료 후 오프셋 커밋 (at-least-once 보장)
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        JsonDeserializer<OrderEvent> deserializer = new JsonDeserializer<>(OrderEvent.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
            config,
            new StringDeserializer(),
            deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderEventConsumerFactory());

        // ✅ 병렬 처리: 파티션 수만큼 스레드 생성 (처리량 향상)
        factory.setConcurrency(3);

        // ✅ 배치 리스너: 개별 처리 대신 한 번에 묶어서 처리
        factory.setBatchListener(true);

        // ✅ 수동 오프셋 커밋 모드
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }
}
