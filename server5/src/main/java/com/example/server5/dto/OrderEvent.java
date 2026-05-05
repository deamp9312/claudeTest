package com.example.server5.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Kafka 메시지로 전송되는 이벤트 객체
 *
 * ✅ 프로토콜 최적화 포인트:
 *  - @JsonProperty로 짧은 필드명 사용 → JSON 크기 감소
 *  - 불필요한 null 필드 제거 (@JsonInclude)
 *  - timestamp를 epoch millis로 → 문자열 파싱 비용 제거
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonInclude(
    com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL  // null 필드 직렬화 생략
)
public class OrderEvent {

    // ✅ 짧은 키 이름 사용 (orderId → oi): JSON 페이로드 크기 절감
    @JsonProperty("oi")
    private Long orderId;

    @JsonProperty("ui")
    private Long userId;

    @JsonProperty("pn")
    private String productName;

    @JsonProperty("am")
    private BigDecimal amount;

    @JsonProperty("st")
    private String status;

    // ✅ epoch millis 사용 (문자열 날짜보다 30~40% 작음)
    @JsonProperty("ts")
    private long timestamp;

    // ✅ 이벤트 타입으로 컨슈머가 빠르게 라우팅 가능
    @JsonProperty("et")
    private EventType eventType;

    public static OrderEvent of(Long orderId, Long userId, String productName,
                                BigDecimal amount, String status, EventType type) {
        return OrderEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .productName(productName)
                .amount(amount)
                .status(status)
                .timestamp(System.currentTimeMillis())  // epoch millis
                .eventType(type)
                .build();
    }

    public enum EventType {
        ORDER_CREATED, ORDER_COMPLETED, ORDER_CANCELLED
    }
}
