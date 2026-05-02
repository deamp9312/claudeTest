package com.example.server1.member.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// 금융권 필수: 모든 주요 행위에 대한 감사 로그
@Entity
@Table(name = "audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 행위 유형: MEMBER_REGISTER, LOGIN, PASSWORD_CHANGE 등
    @Column(nullable = false, length = 50)
    private String actionType;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 45)
    private String ipAddress;

    // SUCCESS / FAIL
    @Column(nullable = false, length = 20)
    private String result;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
