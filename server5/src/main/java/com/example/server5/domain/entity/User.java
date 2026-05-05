package com.example.server5.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email")  // 조회 최적화
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = "orders")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ✅ JPA 최적화: LAZY 로딩 (필요할 때만 ORDER 조회)
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    // ✅ N+1 방지: @BatchSize로 IN 쿼리로 묶어서 조회
    @org.hibernate.annotations.BatchSize(size = 30)
    private List<Order> orders = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static User create(String email, String name) {
        User user = new User();
        user.email = email;
        user.name = name;
        return user;
    }

    public void updateName(String name) {
        this.name = name;
    }
}
