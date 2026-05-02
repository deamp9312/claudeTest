package com.example.server1.member.controller;

import com.example.server1.member.dto.MemberRegisterRequest;
import com.example.server1.member.dto.MemberRegisterResponse;
import com.example.server1.member.exception.DuplicateMemberException;
import com.example.server1.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * CompletableFuture<ResponseEntity<T>> 를 반환하면
 * Spring MVC가 서블릿 비동기 처리(DeferredResult)로 자동 처리.
 * → 요청 스레드(Tomcat 스레드)는 즉시 반환되어 다른 요청을 받을 수 있음.
 * → 실제 응답은 asyncExecutor 스레드가 CompletableFuture 완료 시 전송.
 */
@Slf4j
@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 회원가입
     * POST /members/register
     *
     * 요청 예시:
     * {
     *   "loginId": "hong123",
     *   "password": "P@ssw0rd!",
     *   "name": "홍길동",
     *   "email": "hong@example.com",
     *   "phone": "010-1234-5678"
     * }
     */
    @PostMapping("/register")
    public CompletableFuture<ResponseEntity<Object>> register(
            @RequestBody MemberRegisterRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = httpRequest.getRemoteAddr();
        log.info("[Controller] 회원가입 요청 수신, thread={}", Thread.currentThread().getName());

        return memberService.register(request, ipAddress)
                .<ResponseEntity<Object>>thenApply(response -> {
                    log.info("[Controller] 응답 전송, thread={}", Thread.currentThread().getName());
                    return ResponseEntity.ok(response);
                })
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof DuplicateMemberException) {
                        return ResponseEntity.badRequest().body(Map.of("error", cause.getMessage()));
                    }
                    return ResponseEntity.internalServerError().body(Map.of("error", "서버 오류가 발생했습니다."));
                });
    }

    /**
     * 회원 조회 (단순 확인용)
     * GET /members/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<MemberRegisterResponse> findById(@PathVariable Long id) {
        // 단순 조회는 비동기 불필요 - 동기 처리
        return memberService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
