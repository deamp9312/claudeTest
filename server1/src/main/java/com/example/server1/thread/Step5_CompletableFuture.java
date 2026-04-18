package com.example.server1.thread;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.*;

/**
 * [단계 5] CompletableFuture - 비동기 파이프라인
 *
 * 핵심 개념:
 *   - Future의 한계: get()이 블로킹 → 콜백 불가, 예외 처리 불편, 조합 어려움
 *   - CompletableFuture: Java 8+ 비동기 논블로킹 프로그래밍 API
 *     → 작업 완료 시 콜백 실행 (논블로킹)
 *     → 여러 비동기 작업 조합 (체이닝)
 *     → 예외 처리 내장
 *
 * 주요 메서드:
 *   supplyAsync() : 값을 반환하는 비동기 작업 시작
 *   runAsync()    : 반환값 없는 비동기 작업 시작
 *   thenApply()   : 결과를 변환 (map과 유사)
 *   thenAccept()  : 결과를 소비 (반환값 없음)
 *   thenCompose() : 다른 CompletableFuture를 반환하는 작업에 체이닝 (flatMap)
 *   thenCombine() : 두 작업의 결과를 합침
 *   allOf()       : 모든 CompletableFuture 완료 대기
 *   exceptionally(): 예외 발생 시 복구 처리
 */
@Slf4j
public class Step5_CompletableFuture {

    public static String run() throws InterruptedException, ExecutionException {
        log.info("=== [단계5] CompletableFuture 시작 ===");

        ExecutorService executor = Executors.newFixedThreadPool(4);

        // ── 예제 1: supplyAsync + thenApply 체이닝 ───────────────────────────
        log.info("--- [예제1] 비동기 체이닝 ---");

        CompletableFuture<String> cf1 = CompletableFuture
                // supplyAsync: 별도 스레드에서 값을 반환하는 비동기 작업 시작
                // executor 지정 없으면 ForkJoinPool.commonPool() 사용
                .supplyAsync(() -> {
                    log.info("  [supplyAsync] DB에서 userId 조회 | 스레드: {}", Thread.currentThread().getName());
                    simulateWork(200);
                    return "userId=42"; // 조회 결과
                }, executor)

                // thenApply: 이전 결과를 받아 변환 (별도 스레드 또는 완료 스레드에서 실행)
                .thenApply(userId -> {
                    log.info("  [thenApply] userId로 이름 조회: {} | 스레드: {}", userId, Thread.currentThread().getName());
                    simulateWork(100);
                    return "이름: 홍길동 (from " + userId + ")";
                })

                // thenApply 연속 체이닝 가능
                .thenApply(name -> {
                    log.info("  [thenApply2] 이름 대문자화: {}", name);
                    return name.toUpperCase();
                });

        log.info("  CompletableFuture 생성 직후 → 논블로킹 (비동기 실행 중)");
        String result1 = cf1.get(); // 결과가 필요한 시점에만 블로킹
        log.info("  최종 결과: {}", result1);

        // ── 예제 2: thenCompose - 비동기 작업을 연속으로 ───────────────────────
        log.info("--- [예제2] thenCompose (flatMap) ---");

        CompletableFuture<String> cf2 = CompletableFuture
                .supplyAsync(() -> {
                    log.info("  [1단계] 주문번호 조회");
                    simulateWork(150);
                    return "ORDER-001";
                }, executor)

                // thenCompose: 결과로 또 다른 CompletableFuture를 만들 때 사용
                // thenApply: A → B
                // thenCompose: A → CompletableFuture<B> → B (중첩 방지)
                .thenCompose(orderId ->
                        CompletableFuture.supplyAsync(() -> {
                            log.info("  [2단계] {}의 배송 상태 조회", orderId);
                            simulateWork(200);
                            return orderId + " → 배송중";
                        }, executor)
                );

        log.info("  thenCompose 결과: {}", cf2.get());

        // ── 예제 3: thenCombine - 두 비동기 작업 병렬 실행 후 합치기 ──────────
        log.info("--- [예제3] thenCombine - 두 작업 병렬 실행 ---");

        CompletableFuture<String> stockFuture = CompletableFuture.supplyAsync(() -> {
            log.info("  [재고 조회] 시작");
            simulateWork(300);
            return "재고: 5개";
        }, executor);

        CompletableFuture<String> priceFuture = CompletableFuture.supplyAsync(() -> {
            log.info("  [가격 조회] 시작");
            simulateWork(200);
            return "가격: 10,000원";
        }, executor);

        // thenCombine: 두 CompletableFuture가 모두 완료되면 결과를 합침
        // stockFuture와 priceFuture는 병렬로 실행됨
        CompletableFuture<String> combined = stockFuture.thenCombine(
                priceFuture,
                (stock, price) -> stock + ", " + price // BiFunction: 두 결과 합치기
        );

        log.info("  병렬 조합 결과: {}", combined.get());

        // ── 예제 4: allOf - 여러 작업 모두 완료 대기 ──────────────────────────
        log.info("--- [예제4] allOf - 모든 작업 완료 대기 ---");

        CompletableFuture<String> task1 = CompletableFuture.supplyAsync(() -> {
            simulateWork(100); return "작업1 완료";
        }, executor);
        CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> {
            simulateWork(200); return "작업2 완료";
        }, executor);
        CompletableFuture<String> task3 = CompletableFuture.supplyAsync(() -> {
            simulateWork(150); return "작업3 완료";
        }, executor);

        // allOf(): 모든 CompletableFuture가 완료될 때까지 대기
        // 반환 타입이 Void라 get()으로 결과를 직접 못 가져옴 → join()으로 각각 수집
        CompletableFuture<Void> all = CompletableFuture.allOf(task1, task2, task3);
        all.get(); // 모든 완료 대기
        log.info("  all 결과: {}, {}, {}", task1.join(), task2.join(), task3.join());

        // ── 예제 5: exceptionally - 예외 복구 ──────────────────────────────────
        log.info("--- [예제5] 예외 처리 ---");

        CompletableFuture<String> errorCf = CompletableFuture
                .supplyAsync(() -> {
                    simulateWork(100);
                    if (true) throw new RuntimeException("외부 API 오류!"); // 의도적 예외
                    return "정상 응답";
                }, executor)

                // exceptionally: 예외 발생 시 대체값 반환 (복구)
                // Future.get()에서 예외를 던지지 않고 기본값으로 대체
                .exceptionally(ex -> {
                    log.warn("  ⚠ 예외 발생: {} → 기본값으로 대체", ex.getMessage());
                    return "기본값 응답";
                });

        log.info("  예외 복구 결과: {}", errorCf.get());

        // ── 예제 6: thenAccept - 결과를 소비 (부수효과) ──────────────────────
        log.info("--- [예제6] thenAccept - 결과 소비 (반환값 없음) ---");

        CompletableFuture<Void> consume = CompletableFuture
                .supplyAsync(() -> "처리 완료된 데이터", executor)
                // thenAccept: 결과를 받아 소비 (저장, 로깅 등). 반환값 없음
                .thenAccept(data -> log.info("  [thenAccept] DB 저장: {}", data));

        consume.get();

        executor.shutdown();
        executor.awaitTermination(3, TimeUnit.SECONDS);
        log.info("=== [단계5] 완료 ===");

        return """
                [단계5 완료] CompletableFuture
                - supplyAsync()   : 값 반환하는 비동기 작업 시작
                - thenApply()     : 결과 변환 (동기 map)
                - thenCompose()   : 비동기 작업 연결 (flatMap)
                - thenCombine()   : 두 비동기 결과 합치기 (병렬 실행)
                - allOf()         : 모든 작업 완료 대기
                - exceptionally() : 예외 발생 시 기본값으로 복구
                """;
    }

    private static void simulateWork(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
