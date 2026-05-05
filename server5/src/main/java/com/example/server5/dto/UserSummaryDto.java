package com.example.server5.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// ✅ JPA DTO Projection용: 엔티티 대신 필요한 필드만 담는 가벼운 객체
@Getter
@AllArgsConstructor
public class UserSummaryDto {
    private Long id;
    private String name;
    private String email;
}
