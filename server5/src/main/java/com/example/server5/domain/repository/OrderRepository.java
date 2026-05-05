package com.example.server5.domain.repository;

import com.example.server5.domain.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // ✅ JPA 최적화: IN 절로 여러 유저의 주문을 한 번에 조회 (N+1 방지)
    @Query("SELECT o FROM Order o WHERE o.user.id IN :userIds ORDER BY o.orderedAt DESC")
    List<Order> findByUserIds(@Param("userIds") List<Long> userIds);

    List<Order> findByUserId(Long userId);
}
