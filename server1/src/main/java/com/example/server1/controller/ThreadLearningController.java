package com.example.server1.controller;

import com.example.server1.thread.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * 멀티스레드 학습 컨트롤러
 *  curl http://localhost:8080/thread/1
 *
 * 각 단계별 학습 API:
 *   GET /thread/1  → 기본 스레드 생성 (Thread, Runnable, Lambda)
 *   GET /thread/2  → 동기화 (synchronized, AtomicInteger)
 *   GET /thread/3  → ExecutorService (스레드 풀)
 *   GET /thread/4  → Callable & Future (결과 반환)
 *   GET /thread/5  → CompletableFuture (비동기 파이프라인)
 *   GET /thread/6  → Spring @Async
 *   GET /thread/7  → java.util.concurrent (Latch/Barrier/Semaphore/BlockingQueue/ConcurrentHashMap/ReadWriteLock)
 *   GET /thread/all → 모든 단계 순차 실행
 */
@Slf4j
@RestController
@RequestMapping("/thread")
@RequiredArgsConstructor
public class ThreadLearningController {

    // Step6_SpringAsync는 Spring Bean (@Service)이므로 DI
    private final Step6_SpringAsync step6SpringAsync;

    @GetMapping("/1")
    public String step1() throws InterruptedException {
        log.info("=== API 호출: 단계1 - 기본 스레드 생성 ===");
        return Step1_BasicThread.run();
    }

    @GetMapping("/2")
    public String step2() throws InterruptedException {
        log.info("=== API 호출: 단계2 - 동기화 ===");
        return Step2_Synchronized.run();
    }

    @GetMapping("/3")
    public String step3() throws InterruptedException {
        log.info("=== API 호출: 단계3 - ExecutorService ===");
        return Step3_ExecutorService.run();
    }

    @GetMapping("/4")
    public String step4() throws InterruptedException, ExecutionException {
        log.info("=== API 호출: 단계4 - Callable & Future ===");
        return Step4_CallableFuture.run();
    }

    @GetMapping("/5")
    public String step5() throws InterruptedException, ExecutionException {
        log.info("=== API 호출: 단계5 - CompletableFuture ===");
        return Step5_CompletableFuture.run();
    }

    @GetMapping("/6")
    public String step6() throws ExecutionException, InterruptedException {
        log.info("=== API 호출: 단계6 - Spring @Async ===");

        // ① void 비동기 (fire-and-forget) - 결과 기다리지 않음
        step6SpringAsync.sendEmailAsync("user@example.com");
        log.info("[단계6] sendEmailAsync 호출 완료 → 즉시 반환됨");

        // ② CompletableFuture 반환 - 체이닝 가능
        CompletableFuture<String> userFuture = step6SpringAsync.fetchUserDataAsync("user-123");
        CompletableFuture<String> orderFuture = step6SpringAsync.fetchOrderAsync("order-456");

        // 두 비동기 작업 병렬 대기
        CompletableFuture.allOf(userFuture, orderFuture).join();

        String userData = userFuture.get();
        String orderData = orderFuture.get();
        log.info("[단계6] 결과 수령 → 유저: {}, 주문: {}", userData, orderData);

        // ③ 커스텀 스레드 풀로 실행
        String heavyResult = step6SpringAsync.heavyProcessingAsync("복잡한 데이터").get();
        log.info("[단계6] heavyProcessing 결과: {}", heavyResult);

        return """
                [단계6 완료] Spring @Async
                - @EnableAsync   : @Async 활성화 (AsyncConfig에 설정)
                - @Async void    : fire-and-forget (결과 불필요 작업)
                - @Async CompletableFuture<T>: 결과 반환 + 체이닝 가능 (권장)
                - @Async("poolName"): 특정 스레드 풀 지정 실행
                - 주의: 같은 클래스 내부 호출은 프록시 미경유 → @Async 무효
                - 유저 데이터: %s
                - 주문 데이터: %s
                """.formatted(userData, orderData);
    }

    @GetMapping("/7")
    public String step7() throws InterruptedException, BrokenBarrierException {
        log.info("=== API 호출: 단계7 - java.util.concurrent ===");
        return Step7_ConcurrentPackage.run();
    }

    @GetMapping("/all")
    public String allSteps() throws InterruptedException, ExecutionException, BrokenBarrierException {
        log.info("=== API 호출: 전체 단계 실행 ===");
        StringBuilder sb = new StringBuilder();
        sb.append(Step1_BasicThread.run()).append("\n");
        sb.append(Step2_Synchronized.run()).append("\n");
        sb.append(Step3_ExecutorService.run()).append("\n");
        sb.append(Step4_CallableFuture.run()).append("\n");
        sb.append(Step5_CompletableFuture.run()).append("\n");
        sb.append(step6()).append("\n");
        sb.append(Step7_ConcurrentPackage.run()).append("\n");
        return sb.toString();
    }
}
