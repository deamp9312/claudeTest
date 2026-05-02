package com.example.server1.member.dto;

import com.example.server1.member.entity.Member;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MemberRegisterResponse {
    private Long id;
    private String loginId;
    private String name;
    private String email;
    private String status;
    private LocalDateTime createdAt;

    public static MemberRegisterResponse from(Member member) {
        return MemberRegisterResponse.builder()
                .id(member.getId())
                .loginId(member.getLoginId())
                .name(member.getName())
                .email(member.getEmail())
                .status(member.getStatus().name())
                .createdAt(member.getCreatedAt())
                .build();
    }
}
