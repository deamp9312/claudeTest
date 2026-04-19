package com.example.server3.gc;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * [단계 2] Minor GC 유발 및 Stop-the-World 체감
 *
 * Minor GC 발생 조건:
 *   Eden 영역이 꽉 차면 자동으로 발생 (JVM이 직접 트리거)
 *
 * Minor GC 동작:
 *   1. 모든 애플리케이션 스레드 중지 (STW 시작)
 *   2. Eden + Survivor(from)에서 살아있는 객체 탐색
 *   3. 살아있는 객체 → Survivor(to)로 복사, age +1
 *   4. age >= threshold → Old Gen으로 복사 (Promotion)
 *   5. Eden + Survivor(from) 전체 클리어
 *   6. 모든 스레드 재개 (STW 종료)
 *
 * 왜 Minor GC가 빠른가:
 *   - Young Gen 크기가 작음 (전체 Heap의 1/3~1/4)
 *   - 대부분 객체가 Eden에서 바로 죽음 (weak generational hypothesis)
 *   → STW: 수 ms ~ 수십 ms
 *
 * 왜 Minor GC도 문제가 될 수 있나:
 *   - 초당 수십 번 발생하면 STW 누적 → 처리량(throughput) 저하
 *   - 단기 객체를 너무 많이 만들면 GC 압력 증가
 */
@Slf4j
public class Step2_MinorGcTrigger {

    public static String run() throws InterruptedException {
        log.info("=== [단계2] Minor GC 유발 실험 시작 ===");

        // ── 실험 1: 대량 단기 객체 생성 → Minor GC 자연 발생 ────────────────
        log.info("[실험1] 단기 객체 100만 개 생성 (Eden 압박)");

        long gcCountBefore = getTotalGcCount();
        long gcTimeBefore = getTotalGcTime();
        long startMs = System.currentTimeMillis();

        for (int i = 0; i < 1_000_000; i++) {
            // 참조를 유지하지 않으므로 Eden에서 GC 대상이 됨
            byte[] garbage = new byte[128];
            String temp = "data-" + i;
        }

        long elapsed = System.currentTimeMillis() - startMs;
        long gcCountAfter = getTotalGcCount();
        long gcTimeAfter = getTotalGcTime();

        log.info("  소요 시간: {} ms", elapsed);
        log.info("  GC 발생 횟수 증가: {} 회", gcCountAfter - gcCountBefore);
        log.info("  GC 총 STW 시간 증가: {} ms", gcTimeAfter - gcTimeBefore);
        /*
         * 포인트: elapsed 중 GC STW 시간이 포함됨
         * GC STW 시간 = 애플리케이션이 "멈춰있던" 시간
         */

        // ── 실험 2: 살아있는 객체 일부 유지 → Survivor로 이동 시뮬레이션 ─────
        log.info("[실험2] 일부 객체 생존 → Survivor 이동 시뮬레이션");
        List<byte[]> survivors = new ArrayList<>();

        gcCountBefore = getTotalGcCount();
        startMs = System.currentTimeMillis();

        for (int round = 0; round < 5; round++) {
            for (int i = 0; i < 50_000; i++) {
                byte[] obj = new byte[256];
                if (i % 100 == 0) {
                    // 1% 객체만 살아남음 → Survivor로 복사됨
                    survivors.add(obj);
                }
                // 나머지 99%는 즉시 참조 해제 → Minor GC 시 Eden에서 제거
            }
        }

        elapsed = System.currentTimeMillis() - startMs;
        log.info("  생존 객체 수: {} 개", survivors.size());
        log.info("  소요 시간: {} ms | GC 횟수 증가: {} 회",
                elapsed, getTotalGcCount() - gcCountBefore);
        survivors.clear();

        // ── 실험 3: String 연결 (+ 연산) → 불필요한 중간 객체 과다 생성 ──────
        log.info("[실험3] String + 연산 vs StringBuilder 비교");

        gcCountBefore = getTotalGcCount();
        startMs = System.currentTimeMillis();
        String result = "";
        for (int i = 0; i < 10_000; i++) {
            result = result + "a"; // 매번 새 String 객체 생성 → GC 압력 상승
        }
        long stringPlusTime = System.currentTimeMillis() - startMs;
        long stringPlusGc = getTotalGcCount() - gcCountBefore;

        gcCountBefore = getTotalGcCount();
        startMs = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10_000; i++) {
            sb.append("a"); // 내부 버퍼 재사용 → 객체 1개만 생성
        }
        sb.toString();
        long sbTime = System.currentTimeMillis() - startMs;
        long sbGc = getTotalGcCount() - gcCountBefore;

        log.info("  String + 연산: {} ms | GC {} 회", stringPlusTime, stringPlusGc);
        log.info("  StringBuilder : {} ms | GC {} 회", sbTime, sbGc);
        log.info("  → String+가 느린 이유: 중간 String 객체가 매번 Eden에 쌓여 Minor GC 유발");

        log.info("=== [단계2] 완료 ===");

        return """
                [단계2 완료] Minor GC 체감
                - Minor GC는 Eden 꽉 차면 자동 발생 → STW 수~수십 ms
                - 단기 객체(로컬 변수, 임시 String)를 많이 만들수록 Minor GC 빈도 ↑
                - Minor GC 자체는 빠르지만, 초당 수십 번 발생하면 처리량 저하
                - String + 연산: 10,000번에 중간 객체 수천 개 → GC 압력 직결
                - StringBuilder로 GC 압력 제거 가능 (실무 핵심 팁)
                String + 시간: %d ms | StringBuilder 시간: %d ms
                """.formatted(stringPlusTime, sbTime);
    }

    static long getTotalGcCount() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionCount)
                .filter(c -> c >= 0)
                .sum();
    }

    static long getTotalGcTime() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionTime)
                .filter(t -> t >= 0)
                .sum();
    }
}
