package com.example.server1.member.service;

import com.example.server1.member.dto.MemberRegisterRequest;
import com.example.server1.member.dto.MemberRegisterResponse;
import com.example.server1.member.entity.Member;
import com.example.server1.member.exception.DuplicateMemberException;
import com.example.server1.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 금융권 회원가입 - CompletableFuture(Step5) 비동기 파이프라인
 *
 * 전체 흐름:
 *
 *   [1단계] 아이디 중복 체크  ──┐
 *           (runAsync)        ├─ allOf ──▶ [2단계] 회원 저장(thenApply)
 *   [1단계] 이메일 중복 체크  ──┘                     │
 *           (runAsync)                               │ thenCompose
 *                                                    ▼
 *                                        감사로그 ──┐
 *                                        FDS 알림  ├─ allOf ──▶ [최종] 응답 반환
 *                                        이메일    ──┘
 *
 * 병렬 처리 효과:
 *   - 중복 체크 2개: 순차 처리 시 ~40ms → 병렬 처리 시 ~20ms
 *   - 후처리 3개:   순차 처리 시 ~250ms → 병렬 처리 시 ~100ms (가장 느린 것 기준)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberPersistenceService persistenceService;

    // AsyncConfig에서 등록한 스레드 풀 (core=3, max=10)
    @Qualifier("threadPoolTaskExecutor")
    private final Executor executor;

    public CompletableFuture<MemberRegisterResponse> register(MemberRegisterRequest request, String ipAddress) {
        log.info("[회원가입 시작] loginId={}, ip={}, thread={}", request.getLoginId(), ipAddress, Thread.currentThread().getName());

        // ── 1단계: 중복 검증 (runAsync × 2 → 병렬 실행) ─────────────────────────
        // Step5 패턴: runAsync - 결과 반환 없이 비동기 실행, 실패 시 CompletionException 전파
        CompletableFuture<Void> idCheck = CompletableFuture.runAsync(() -> {
            log.info("[중복 체크] 아이디 조회 중... thread={}", Thread.currentThread().getName());
            if (memberRepository.existsByLoginId(request.getLoginId())) {
                throw new DuplicateMemberException("이미 사용 중인 아이디입니다: " + request.getLoginId());
            }
        }, executor);

        CompletableFuture<Void> emailCheck = CompletableFuture.runAsync(() -> {
            log.info("[중복 체크] 이메일 조회 중... thread={}", Thread.currentThread().getName());
            if (memberRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateMemberException("이미 등록된 이메일입니다: " + request.getEmail());
            }
        }, executor);

        // ── 2단계: 두 검증 완료 후 저장 → 후처리 ────────────────────────────────
        return CompletableFuture
                // Step5 패턴: allOf - 두 비동기 작업이 모두 완료될 때까지 대기
                // 둘 중 하나라도 실패하면 전체가 exceptionally로 떨어짐
                .allOf(idCheck, emailCheck)

                // Step5 패턴: thenApply - 검증 완료 후 동기적으로 회원 저장
                // persistenceService.saveMember()는 @Transactional 메서드 → 별도 빈이므로 프록시 정상 동작
                .thenApply(ignored -> {
                    log.info("[저장] 검증 통과 → 회원 저장 시작, thread={}", Thread.currentThread().getName());
                    return persistenceService.saveMember(request, ipAddress);
                })

                // Step5 패턴: thenCompose - 저장 결과를 받아 새로운 CompletableFuture 체인 연결
                // thenApply와 달리 내부에서 CompletableFuture를 반환할 때 사용 (flatMap 개념)
                .thenCompose(savedMember -> runPostProcessing(savedMember, ipAddress))

                // Step5 패턴: exceptionally - 체인 어디서든 예외 발생 시 여기서 처리
                // CompletionException으로 래핑되므로 getCause()로 원본 예외 추출 필요
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    log.error("[회원가입 실패] loginId={}, reason={}", request.getLoginId(), cause.getMessage());

                    if (cause instanceof DuplicateMemberException dup) {
                        throw dup; // 중복 예외는 그대로 re-throw → 컨트롤러에서 400 처리
                    }
                    throw new RuntimeException("회원가입 처리 중 오류가 발생했습니다.", cause);
                });
    }

    /**
     * 저장 완료 후 병렬 후처리.
     * 후처리 실패는 각각 exceptionally로 자체 흡수 → 메인 흐름 중단 없음.
     * 금융권 실무 원칙: 감사로그·FDS는 최선 처리(best-effort), 실패 시 별도 알람으로 보완.
     */
    private CompletableFuture<MemberRegisterResponse> runPostProcessing(Member savedMember, String ipAddress) {

        // Step5 패턴: 후처리 3개를 각각 runAsync로 비동기 시작
        CompletableFuture<Void> auditLog = CompletableFuture.runAsync(
                () -> {
                    persistenceService.saveAuditLog(savedMember.getId(), ipAddress, "SUCCESS");
                    log.info("[감사로그] 저장 완료: memberId={}, thread={}", savedMember.getId(), Thread.currentThread().getName());
                },
                executor
        ).exceptionally(ex -> {
            log.error("[감사로그 실패] memberId={}, error={}", savedMember.getId(), ex.getMessage());
            return null; // 실패해도 null 반환으로 흐름 유지
        });

        CompletableFuture<Void> fdsNotify = CompletableFuture.runAsync(
                () -> {
                    // 실제 환경: FDS(이상거래탐지시스템) 외부 API 호출
                    simulateApiCall("FDS", 50);
                    log.info("[FDS] 신규 회원 등록 알림 완료: memberId={}", savedMember.getId());
                },
                executor
        ).exceptionally(ex -> {
            log.warn("[FDS 알림 실패] memberId={}, error={}", savedMember.getId(), ex.getMessage());
            return null;
        });

        CompletableFuture<Void> welcomeEmail = CompletableFuture.runAsync(
                () -> {
                    // 실제 환경: JavaMailSender 또는 외부 이메일 서비스 API 호출
                    simulateApiCall("EMAIL", 100);
                    log.info("[이메일] 환영 메일 발송 완료: to={}", savedMember.getEmail());
                },
                executor
        ).exceptionally(ex -> {
            log.warn("[이메일 발송 실패] email={}, error={}", savedMember.getEmail(), ex.getMessage());
            return null;
        });

        // Step5 패턴: allOf - 3개 후처리가 모두 끝난 뒤 thenApply로 응답 객체 생성
        return CompletableFuture.allOf(auditLog, fdsNotify, welcomeEmail)
                .thenApply(v -> {
                    log.info("[회원가입 완료] memberId={}", savedMember.getId());
                    return MemberRegisterResponse.from(savedMember);
                });
    }

    public Optional<MemberRegisterResponse> findById(Long id) {
        return memberRepository.findById(id).map(MemberRegisterResponse::from);
    }

    private void simulateApiCall(String system, long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("[" + system + "] 외부 API 호출 중단");
        }
    }
}
