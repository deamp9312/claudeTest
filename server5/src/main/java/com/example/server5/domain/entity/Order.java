package com.example.server5.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_user_id", columnList = "user_id"),
    @Index(name = "idx_order_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)  // ✅ LAZY: User 전체를 끌어오지 않음
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime orderedAt;

    @PrePersist
    protected void onCreate() {
        orderedAt = LocalDateTime.now();
        status = OrderStatus.PENDING;
    }

    public static Order create(User user, String productName, BigDecimal amount) {
        Order order = new Order();
        order.user = user;
        order.productName = productName;
        order.amount = amount;
        return order;
    }

    public void complete() {
        this.status = OrderStatus.COMPLETED;
    }

    public enum OrderStatus {
        PENDING, COMPLETED, CANCELLED
    }
}
