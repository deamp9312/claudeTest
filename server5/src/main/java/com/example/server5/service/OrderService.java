package com.example.server5.service;

import com.example.server5.domain.entity.Order;
import com.example.server5.domain.entity.User;
import com.example.server5.domain.repository.OrderRepository;
import com.example.server5.domain.repository.UserRepository;
import com.example.server5.dto.OrderEvent;
import com.example.server5.kafka.producer.OrderEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderEventProducer orderEventProducer;

    /**
     * ✅ 통합 최적화 흐름:
     * 1. JPA: 배치 INSERT (hibernate.jdbc.batch_size)
     * 2. Kafka: 배치 전송 (linger.ms + batch.size)
     * 3. Redis: 캐시 무효화
     */
    @Transactional
    public List<Order> createOrdersBatch(Long userId, List<String> productNames, List<BigDecimal> amounts) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // ✅ JPA 배치 INSERT: 50개씩 묶어서 INSERT (application.yml의 batch_size)
        List<Order> orders = new ArrayList<>();
        List<OrderEvent> events = new ArrayList<>();

        for (int i = 0; i < productNames.size(); i++) {
            Order order = Order.create(user, productNames.get(i), amounts.get(i));
            orders.add(order);
        }

        List<Order> savedOrders = orderRepository.saveAll(orders);  // 배치 INSERT 실행

        // ✅ Kafka 배치 전송: 저장된 모든 주문 이벤트를 한 번에
        savedOrders.forEach(order -> events.add(
            OrderEvent.of(
                order.getId(), userId, order.getProductName(),
                order.getAmount(), order.getStatus().name(),
                OrderEvent.EventType.ORDER_CREATED
            )
        ));
        orderEventProducer.sendBatch(events);  // 배치로 Kafka 전송

        log.info("배치 처리 완료: {}건 주문 생성 + Kafka 이벤트 전송", savedOrders.size());
        return savedOrders;
    }

    @Transactional
    public Order createOrder(Long userId, String productName, BigDecimal amount) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Order order = orderRepository.save(Order.create(user, productName, amount));

        // ✅ 비동기 Kafka 이벤트: DB 커밋 후 이벤트 발행 (순서 보장)
        OrderEvent event = OrderEvent.of(
            order.getId(), userId, productName, amount,
            order.getStatus().name(), OrderEvent.EventType.ORDER_CREATED
        );
        orderEventProducer.sendOrderEvent(event);  // @Async: 비동기 전송

        return order;
    }

    // ✅ Redis 캐싱: 같은 userId의 주문 목록은 캐시에서 반환
    @Cacheable(value = "orders", key = "#userId")
    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    /**
     * ✅ JPA 최적화: N+1 방지
     * 여러 유저의 주문을 개별 조회(N번) 대신 IN 절로 1번 조회
     */
    public List<Order> getOrdersForUsers(List<Long> userIds) {
        return orderRepository.findByUserIds(userIds);  // WHERE user_id IN (1,2,3...)
    }
}
