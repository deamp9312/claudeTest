package com.example.server1.thread;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * [단계 6] Spring @Async - 스프링의 비동기 처리
 *
 * 핵심 개념:
 *   - @Async: Spring AOP 기반으로 메서드를 별도 스레드에서 자동 실행
 *   - @EnableAsync: 애플리케이션에서 @Async 기능을 활성화 (Server1Application에 추가 필요)
 *
 * @Async 동작 원리:
 *   1. Spring이 @Async 메서드를 가진 빈을 프록시(Proxy)로 감쌈
 *   2. 외부에서 호출 시 → 프록시가 메서드를 ThreadPool에 제출하고 즉시 반환
 *   3. 실제 메서드는 별도 스레드에서 실행
 *
 * 주의 사항:
 *   ① 같은 클래스 내부에서 @Async 메서드를 직접 호출하면 AOP 프록시를 거치지 않아 동작 안 함
 *   ② @Async는 반드시 public 메서드에만 사용
 *   ③ 커스텀 스레드 풀(ThreadPoolTaskExecutor) 설정 권장
 *
 * 반환 타입:
 *   void                    : 결과 필요 없을 때 (fire-and-forget)
 *   Future<T>               : 레거시 방식
 *   CompletableFuture<T>    : 권장 방식 (체이닝, 조합 가능)
 */
@Slf4j
@Service
public class Step6_SpringAsync {

    // ──────────────────────────────────────────────────────────────────────────
    // @Async void - 반환값 없는 비동기 메서드 (fire-and-forget)
    //   - 호출 즉시 반환, 메서드 실행은 별도 스레드에서 계속됨
    //   - 이메일 발송, 알림 전송 등 결과를 기다릴 필요 없는 작업에 사용
    // ──────────────────────────────────────────────────────────────────────────
    @Async // Spring이 이 메서드를 비동기로 실행하도록 프록시 생성
    public void sendEmailAsync(String to) {
        log.info("  [sendEmailAsync] 이메일 발송 시작: {} | 스레드: {}", to, Thread.currentThread().getName());
        try {
            Thread.sleep(800); // 이메일 발송 시뮬레이션
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("  [sendEmailAsync] 이메일 발송 완료: {}", to);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // @Async CompletableFuture<T> - 결과를 반환하는 비동기 메서드 (권장)
    //   - 호출자가 필요한 시점에 .get()이나 .join()으로 결과를 받을 수 있음
    // ──────────────────────────────────────────────────────────────────────────
    @Async
    public CompletableFuture<String> fetchUserDataAsync(String userId) {
        log.info("  [fetchUserDataAsync] userId={} 조회 시작 | 스레드: {}", userId, Thread.currentThread().getName());
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // 예외 발생 시 CompletableFuture를 실패 상태로 완료
            return CompletableFuture.failedFuture(e);
        }
        String result = "User{id=" + userId + ", name=홍길동}";
        log.info("  [fetchUserDataAsync] 조회 완료: {}", result);
        // CompletableFuture.completedFuture(): 이미 완료된 결과를 CompletableFuture로 감쌈
        return CompletableFuture.completedFuture(result);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // @Async Future<T> - 레거시 방식 (Spring 4.x 호환성 목적)
    //   - 새 코드에서는 CompletableFuture 사용 권장
    //   - AsyncResult.forValue(): Spring이 제공하는 Future 구현체
    // ──────────────────────────────────────────────────────────────────────────
    @Async
    public Future<String> fetchOrderAsync(String orderId) {
        log.info("  [fetchOrderAsync] 주문 조회: {} | 스레드: {}", orderId, Thread.currentThread().getName());
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // AsyncResult: Spring의 Future 구현체
        return AsyncResult.forValue("Order{id=" + orderId + ", status=배송중}");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 특정 스레드 풀 지정: @Async("poolName")
    //   - AsyncConfig에서 정의한 스레드 풀 이름으로 실행
    //   - 작업 성격별로 별도 풀 운영 가능 (CPU 집약 vs IO 집약 분리)
    // ──────────────────────────────────────────────────────────────────────────
    @Async("threadPoolTaskExecutor") // AsyncConfig에 정의된 커스텀 풀 이름
    public CompletableFuture<String> heavyProcessingAsync(String data) {
        log.info("  [heavyProcessingAsync] 무거운 처리 시작 | 스레드: {}", Thread.currentThread().getName());
        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return CompletableFuture.completedFuture("처리 완료: " + data);
    }
}
