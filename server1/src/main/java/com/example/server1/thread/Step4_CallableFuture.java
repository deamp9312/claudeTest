package com.example.server1.thread;

import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * [단계 4] Callable & Future - 스레드 실행 결과 받기
 *
 * 핵심 개념:
 *   - Runnable : 반환값이 없음 (void). 결과를 알 수 없음
 *   - Callable : 반환값이 있음 (V). 예외도 던질 수 있음
 *   - Future    : Callable 작업의 "미래 결과를 담는 컨테이너"
 *                 작업이 완료될 때까지 get()으로 대기 후 결과 수령
 *
 * Callable vs Runnable 비교:
 *   Runnable: void run()
 *   Callable: V      call() throws Exception
 */
@Slf4j
public class Step4_CallableFuture {

    // ──────────────────────────────────────────────────────────────────────────
    // 외부 API 호출을 시뮬레이션하는 Callable
    //   - Integer를 반환 (처리 시간 ms)
    // ──────────────────────────────────────────────────────────────────────────
    static class ApiCallTask implements Callable<String> {
        private final String apiName;
        private final long delayMs; // 응답 지연 시간 시뮬레이션

        ApiCallTask(String apiName, long delayMs) {
            this.apiName = apiName;
            this.delayMs = delayMs;
        }

        @Override
        public String call() throws Exception {
            // call()은 run()과 달리 반환값이 있고 checked exception을 던질 수 있음
            log.info("  [API 호출] {} 시작 | 스레드: {}", apiName, Thread.currentThread().getName());
            Thread.sleep(delayMs); // API 응답 대기 시뮬레이션
            String result = apiName + " 응답 완료 (" + delayMs + "ms)";
            log.info("  [API 완료] {}", result);
            return result;
        }
    }

    public static String run() throws InterruptedException, ExecutionException {
        log.info("=== [단계4] Callable & Future 시작 ===");

        ExecutorService executor = Executors.newFixedThreadPool(3);

        // ── 예제 1: 단일 Future ────────────────────────────────────────────────
        log.info("--- [예제1] 단일 Callable 제출 ---");

        // submit(): Callable을 스레드 풀에 제출하고, 즉시 Future 반환 (논블로킹)
        // Future는 아직 완료되지 않은 작업의 "약속 티켓" 같은 것
        Future<String> future1 = executor.submit(new ApiCallTask("주문API", 500));

        // 이 시점에서 "주문API"는 별도 스레드에서 실행 중
        log.info("  Future 반환 직후 → 다른 작업 수행 가능 (논블로킹)");

        // future1.get(): 작업 완료될 때까지 현재 스레드 블로킹 → 결과 반환
        // 주의: get()은 블로킹 호출! 완료될 때까지 기다림
        String result1 = future1.get();
        log.info("  결과 수령: {}", result1);

        // ── 예제 2: 타임아웃 설정 ─────────────────────────────────────────────
        log.info("--- [예제2] 타임아웃 설정 ---");

        Future<String> futureTimeout = executor.submit(new ApiCallTask("느린API", 2000));
        try {
            // get(timeout, unit): 지정 시간 내에 완료 안 되면 TimeoutException
            String result = futureTimeout.get(500, TimeUnit.MILLISECONDS);
            log.info("  타임아웃 내 완료: {}", result);
        } catch (TimeoutException e) {
            log.warn("  ⚠ 타임아웃 발생 (500ms 초과) → 작업 취소");
            // cancel(true): 실행 중인 스레드에 interrupt 전송하여 취소 시도
            futureTimeout.cancel(true);
        }

        // ── 예제 3: 여러 Callable 병렬 실행 ──────────────────────────────────
        log.info("--- [예제3] 여러 API 병렬 호출 (가장 많이 쓰는 패턴) ---");

        List<Callable<String>> tasks = List.of(
                new ApiCallTask("상품API", 300),
                new ApiCallTask("재고API", 200),
                new ApiCallTask("가격API", 400)
        );

        long startTime = System.currentTimeMillis();

        // invokeAll(): 모든 Callable을 병렬로 실행하고 모두 완료될 때까지 대기
        // 반환: 완료된 순서가 아닌 제출한 순서대로 Future 리스트 반환
        List<Future<String>> futures = executor.invokeAll(tasks);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("  병렬 실행 총 소요: {}ms (순차 실행이었다면 ~900ms)", elapsed);

        // 각 Future에서 결과 수집
        List<String> results = new ArrayList<>();
        for (Future<String> f : futures) {
            results.add(f.get()); // 이미 완료됐으므로 즉시 반환
        }
        log.info("  수집된 결과: {}", results);

        // ── 예제 4: invokeAny - 가장 먼저 완료된 결과만 ──────────────────────
        log.info("--- [예제4] invokeAny - 가장 빠른 응답만 사용 ---");

        List<Callable<String>> raceTasks = List.of(
                new ApiCallTask("서버1", 400),
                new ApiCallTask("서버2", 150), // 가장 빠름
                new ApiCallTask("서버3", 300)
        );

        // invokeAny(): 가장 먼저 완료된 작업의 결과만 반환, 나머지 작업은 취소
        // 여러 서버 중 가장 빠른 응답을 사용하는 패턴에 유용
        String fastest = executor.invokeAny(raceTasks);
        log.info("  가장 빠른 응답: {}", fastest);

        executor.shutdown();
        executor.awaitTermination(3, TimeUnit.SECONDS);
        log.info("=== [단계4] 완료 ===");

        return """
                [단계4 완료] Callable & Future
                - Callable : 반환값 있는 스레드 작업 (call() 메서드)
                - Future   : 미래 결과 컨테이너. get()으로 블로킹 대기
                - get(timeout): 타임아웃 설정으로 무한 대기 방지
                - invokeAll(): 모든 작업 병렬 실행 후 전체 결과 수집
                - invokeAny(): 가장 빠른 1개 결과만 수령, 나머지 취소
                """;
    }
}
