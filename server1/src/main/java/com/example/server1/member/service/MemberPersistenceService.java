package com.example.server1.member.service;

import com.example.server1.member.dto.MemberRegisterRequest;
import com.example.server1.member.entity.AuditLog;
import com.example.server1.member.entity.Member;
import com.example.server1.member.entity.MemberStatus;
import com.example.server1.member.repository.AuditLogRepository;
import com.example.server1.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA 트랜잭션이 필요한 DB 쓰기 작업만 담당.
 *
 * MemberService(오케스트레이터)에서 CompletableFuture 체인으로 호출할 때
 * @Transactional이 Spring AOP 프록시를 통해 정상 동작하려면
 * 같은 클래스가 아닌 별도 빈(bean)으로 분리해야 한다.
 * 이것이 금융권 코드에서 서비스 레이어를 얇게 분리하는 주된 이유 중 하나다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberPersistenceService {

    private final MemberRepository memberRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional
    public Member saveMember(MemberRegisterRequest request, String ipAddress) {
        Member member = Member.builder()
                .loginId(request.getLoginId())
                .password(encodePassword(request.getPassword()))
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .status(MemberStatus.ACTIVE)
                .ipAddress(ipAddress)
                .build();

        Member saved = memberRepository.save(member);
        log.info("[DB 저장] 회원 저장 완료: memberId={}, thread={}", saved.getId(), Thread.currentThread().getName());
        return saved;
    }

    // 감사 로그는 메인 트랜잭션과 분리 (메인 롤백과 무관하게 기록 유지)
    @Transactional
    public void saveAuditLog(Long memberId, String ipAddress, String result) {
        AuditLog log = AuditLog.builder()
                .actionType("MEMBER_REGISTER")
                .memberId(memberId)
                .ipAddress(ipAddress)
                .result(result)
                .build();
        auditLogRepository.save(log);
    }

    private String encodePassword(String rawPassword) {
        // 실제 환경: BCryptPasswordEncoder.encode() 사용
        // 금융권 요구사항: PBKDF2, BCrypt, Argon2 등 단방향 해시 의무화
        return "{bcrypt}" + Integer.toHexString(rawPassword.hashCode());
    }
}
