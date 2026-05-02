package com.example.server1.member.entity;

public enum MemberStatus {
    ACTIVE,    // 정상
    DORMANT,   // 휴면 (1년 미접속 시 자동 전환 - 금융권 의무)
    SUSPENDED  // 계정 정지
}
