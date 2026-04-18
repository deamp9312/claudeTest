package com.example.server1.thread;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.*;

/**
 * [단계 3] ExecutorService - 스레드 풀(Thread Pool) 관리
 *
 * 핵심 개념:
 *   - 스레드 생성 비용: new Thread() 마다 OS 레벨 스레드를 생성 → 비싼 연산
 *   - Thread Pool : 미리 스레드를 만들어 놓고 재사용 → 생성/소멸 비용 절감
 *   - ExecutorService: Java의 표준 스레드 풀 인터페이스
 *   - Executors 팩토리: 다양한 종류의 스레드 풀을 쉽게 생성
 *
 * 스레드 풀 종류:
 *   newFixedThreadPool(n)  : 고정 크기 (n개 스레드 항상 유지)
 *   newCachedThreadPool()  : 유동 크기 (필요 시 생성, 60초 후 미사용 스레드 제거)
 *   newSingleThreadExecutor(): 스레드 1개 (순서 보장)
 *   newScheduledThreadPool(n): 주기적/지연 실행 지원
 */
@Slf4j
public class Step3_ExecutorService {

    public static String run() throws InterruptedException {
        log.info("=== [단계3] ExecutorService 스레드 풀 시작 ===");

        // ── 예제 1: newFixedThreadPool ─────────────────────────────────────────
        // 스레드 3개 고정. 작업이 3개 초과하면 큐에서 대기
        ExecutorService fixedPool = Executors.newFixedThreadPool(3);
        log.info("--- [FixedThreadPool] 스레드 3개, 작업 6개 제출 ---");

        for (int i = 1; i <= 6; i++) {
            final int taskId = i;
            // execute(): 반환값 없는 Runnable 작업 제출
            fixedPool.execute(() -> {
                log.info("  [Fixed] 작업{} 시작 | 실행 스레드: {}", taskId, Thread.currentThread().getName());
                try {
                    Thread.sleep(300); // 작업 처리 시뮬레이션
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                log.info("  [Fixed] 작업{} 완료", taskId);
            });
        }
        // shutdown(): 더 이상 새 작업을 받지 않음. 기존 작업은 모두 완료 후 종료
        // shutdownNow(): 즉시 중단 시도 (실행 중인 작업에 interrupt 전송)
        fixedPool.shutdown();
        // awaitTermination(): 지정 시간 동안 모든 작업 완료를 기다림
        fixedPool.awaitTermination(5, TimeUnit.SECONDS);
        log.info("--- [FixedThreadPool] 완료 ---");

        // ── 예제 2: newCachedThreadPool ───────────────────────────────────────
        // 스레드 수 제한 없음. 유휴 스레드 재사용, 없으면 새로 생성
        // 단기 작업이 폭발적으로 많을 때 적합. 장기 작업이 많으면 OOM 위험
        ExecutorService cachedPool = Executors.newCachedThreadPool();
        log.info("--- [CachedThreadPool] 작업 4개 동시 제출 ---");

        for (int i = 1; i <= 4; i++) {
            final int taskId = i;
            cachedPool.execute(() -> {
                log.info("  [Cached] 작업{} | 스레드: {}", taskId, Thread.currentThread().getName());
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        cachedPool.shutdown();
        cachedPool.awaitTermination(3, TimeUnit.SECONDS);
        log.info("--- [CachedThreadPool] 완료 ---");

        // ── 예제 3: newSingleThreadExecutor ──────────────────────────────────
        // 스레드 1개만 사용 → 작업이 제출된 순서대로 순차 실행 보장
        // 순서가 중요한 작업(로그 기록, DB 순차 처리 등)에 사용
        ExecutorService singlePool = Executors.newSingleThreadExecutor();
        log.info("--- [SingleThread] 순서 보장 실행 ---");

        for (int i = 1; i <= 4; i++) {
            final int taskId = i;
            singlePool.execute(() -> {
                // 항상 같은 스레드 이름이 출력됨 → 순서 보장
                log.info("  [Single] 작업{} | 스레드: {}", taskId, Thread.currentThread().getName());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        singlePool.shutdown();
        singlePool.awaitTermination(3, TimeUnit.SECONDS);
        log.info("--- [SingleThread] 완료 ---");

        // ── 예제 4: 직접 ThreadPoolExecutor 커스텀 ──────────────────────────
        // 실무에서는 Executors 대신 직접 파라미터를 제어해 스레드 풀 생성
        ThreadPoolExecutor customPool = new ThreadPoolExecutor(
                2,                          // corePoolSize: 기본 유지 스레드 수
                5,                          // maximumPoolSize: 최대 스레드 수
                30, TimeUnit.SECONDS,       // keepAliveTime: 초과 스레드 유휴 대기 시간
                new LinkedBlockingQueue<>(10), // workQueue: 대기 작업 큐 (용량 10)
                new ThreadFactory() {
                    // 스레드 이름을 커스텀 (로그/모니터링에서 구분 용이)
                    private int count = 0;
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setName("custom-worker-" + (++count));
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // 큐 초과 시 호출 스레드가 직접 실행
        );

        log.info("--- [CustomPool] core={}, max={} ---",
                customPool.getCorePoolSize(), customPool.getMaximumPoolSize());

        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            customPool.execute(() -> {
                log.info("  [Custom] 작업{} | 스레드: {}", taskId, Thread.currentThread().getName());
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        customPool.shutdown();
        customPool.awaitTermination(5, TimeUnit.SECONDS);
        log.info("=== [단계3] 완료 ===");

        return """
                [단계3 완료] ExecutorService 스레드 풀
                - FixedThreadPool  : 고정 크기, 초과 작업은 큐 대기
                - CachedThreadPool : 유동 크기, 단기 병렬 작업에 적합
                - SingleThread     : 1개 스레드, 순서 보장
                - ThreadPoolExecutor: 직접 파라미터 제어 (실무 권장)
                - shutdown() 후 awaitTermination()으로 정상 종료 보장
                """;
    }
}
