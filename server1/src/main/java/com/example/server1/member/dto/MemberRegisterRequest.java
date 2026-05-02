package com.example.server1.member.dto;

import lombok.Getter;

@Getter
public class MemberRegisterRequest {
    private String loginId;
    private String password;
    private String name;
    private String email;
    private String phone;
}
