package com.example.server5.controller;

import com.example.server5.domain.entity.Order;
import com.example.server5.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 단건 주문 생성 → Kafka 이벤트 비동기 발행
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String productName = body.get("productName").toString();
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        return ResponseEntity.ok(orderService.createOrder(userId, productName, amount));
    }

    // 배치 주문 생성 → JPA 배치 INSERT + Kafka 배치 전송
    @PostMapping("/batch")
    public ResponseEntity<List<Order>> createBatch(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        List<String> products = (List<String>) body.get("productNames");
        List<BigDecimal> amounts = ((List<String>) body.get("amounts"))
            .stream().map(BigDecimal::new).toList();
        return ResponseEntity.ok(orderService.createOrdersBatch(userId, products, amounts));
    }

    // 유저 주문 목록 조회 → Redis 캐시
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrders(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUser(userId));
    }
}
