package com.example.server3.gc;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * [단계 6] GC 튜닝 실전 - "GC 부담을 코드 레벨에서 줄이는 법"
 *
 * GC 최적화의 두 방향:
 *   A. JVM 플래그 조정 → GC 알고리즘/힙 크기 조정 (운영 레벨)
 *   B. 코드 레벨 개선 → 객체 생성 자체를 줄임 (개발 레벨)
 *
 * 코드 레벨 최적화 원칙:
 *   1. 단기 객체 생성을 줄여라 → Minor GC 압력 ↓
 *   2. Old Gen 진입 객체를 줄여라 → Full GC 빈도 ↓
 *   3. 참조를 적시에 끊어라 → GC가 일을 빨리 할 수 있게
 *   4. 큰 객체 주의 → Humongous 영역 직행, G1GC 효율 ↓
 */
@Slf4j
public class Step6_GcTuning {

    public static String run() throws InterruptedException {
        log.info("=== [단계6] GC 튜닝 실전 시작 ===");

        // ── 최적화 1: 객체 재사용 (Object Pool 패턴) ─────────────────────────
        log.info("[최적화1] 객체 재사용 vs 매번 생성 비교");

        // 나쁜 패턴: 매번 새 객체 생성
        long gcBefore = getTotalGcCount();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 500_000; i++) {
            StringBuilder sb = new StringBuilder(); // 매번 새 객체
            sb.append("data-").append(i).append("-end");
            sb.toString();
        }
        long noReuseTime = System.currentTimeMillis() - start;
        long noReuseGc = getTotalGcCount() - gcBefore;

        // 좋은 패턴: 하나를 재사용
        gcBefore = getTotalGcCount();
        start = System.currentTimeMillis();
        StringBuilder reusedSb = new StringBuilder(64); // 한 번 생성
        for (int i = 0; i < 500_000; i++) {
            reusedSb.setLength(0); // 재사용: 내부 배열 유지, 길이만 0으로
            reusedSb.append("data-").append(i).append("-end");
            reusedSb.toString();
        }
        long reuseTime = System.currentTimeMillis() - start;
        long reuseGc = getTotalGcCount() - gcBefore;

        log.info("  매번 new StringBuilder: {} ms | GC {} 회", noReuseTime, noReuseGc);
        log.info("  재사용 StringBuilder  : {} ms | GC {} 회", reuseTime, reuseGc);
        log.info("  → 객체 재사용으로 GC 압력 대폭 감소");

        // ── 최적화 2: 초기 용량 지정 → 내부 배열 재할당 방지 ─────────────────
        log.info("[최적화2] 컬렉션 초기 용량 지정");

        gcBefore = getTotalGcCount();
        start = System.currentTimeMillis();
        List<String> noCapacity = new ArrayList<>(); // 기본 용량 10, 꽉 차면 2배 확장 → 구 배열 GC 대상
        for (int i = 0; i < 100_000; i++) {
            noCapacity.add("item-" + i);
        }
        long noCapTime = System.currentTimeMillis() - start;
        long noCapGc = getTotalGcCount() - gcBefore;

        gcBefore = getTotalGcCount();
        start = System.currentTimeMillis();
        List<String> withCapacity = new ArrayList<>(100_000); // 처음부터 충분한 크기
        for (int i = 0; i < 100_000; i++) {
            withCapacity.add("item-" + i);
        }
        long capTime = System.currentTimeMillis() - start;
        long capGc = getTotalGcCount() - gcBefore;

        log.info("  초기 용량 없음: {} ms | GC {} 회 (내부 배열 재할당 발생)", noCapTime, noCapGc);
        log.info("  초기 용량 지정: {} ms | GC {} 회", capTime, capGc);

        // ── 최적화 3: 수동 Object Pool ────────────────────────────────────────
        log.info("[최적화3] 수동 Object Pool 패턴 (비용이 큰 객체 재사용)");

        Deque<byte[]> pool = new ArrayDeque<>(10);
        for (int i = 0; i < 10; i++) {
            pool.push(new byte[4096]); // 4KB 버퍼 10개 사전 할당
        }

        gcBefore = getTotalGcCount();
        start = System.currentTimeMillis();
        for (int i = 0; i < 10_000; i++) {
            byte[] buf = pool.isEmpty() ? new byte[4096] : pool.pop(); // 풀에서 꺼냄
            // ... 버퍼 사용 ...
            pool.push(buf); // 다 쓰면 풀에 반납
        }
        long poolTime = System.currentTimeMillis() - start;
        long poolGc = getTotalGcCount() - gcBefore;

        gcBefore = getTotalGcCount();
        start = System.currentTimeMillis();
        for (int i = 0; i < 10_000; i++) {
            byte[] buf = new byte[4096]; // 매번 할당
            // ... 버퍼 사용 ...
        }
        long noPoolTime = System.currentTimeMillis() - start;
        long noPoolGc = getTotalGcCount() - gcBefore;

        log.info("  Object Pool 사용: {} ms | GC {} 회", poolTime, poolGc);
        log.info("  매번 new byte[]  : {} ms | GC {} 회", noPoolTime, noPoolGc);

        // ── 최적화 4: 큰 객체 주의 ───────────────────────────────────────────
        log.info("[최적화4] Humongous 객체 주의 (G1GC 기준 Region 크기의 50% 초과)");
        log.info("  G1GC에서 큰 객체(Humongous)는 Humongous Region에 직행");
        log.info("  → Young GC 경유 없이 바로 Old Gen급 처리 → Full GC 유발 가능");
        log.info("  → 큰 배열/컬렉션은 분할하거나 off-heap(ByteBuffer.allocateDirect) 사용 고려");

        // ── 최적화 5: 불필요한 참조 조기 제거 ───────────────────────────────
        log.info("[최적화5] 참조 조기 제거 (null 할당)");
        log.info("  장기 실행 메서드에서 큰 객체를 초반에 쓰고 이후 불필요하면 null 처리");
        log.info("  → GC가 더 이른 시점에 수거 가능 → Old Gen 승격 방지");

        List<byte[]> bigData = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            bigData.add(new byte[10240]); // 10KB씩 1MB
        }
        // bigData 사용 완료 후
        bigData = null; // 명시적 null → 이 시점 이후 GC 수거 가능
        // bigData = null이 없으면 메서드 종료까지 참조 유지

        log.info("=== [단계6] 완료 ===");

        return """
                [단계6 완료] GC 튜닝 실전
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                코드 레벨 GC 최적화:
                  1. StringBuilder 재사용 (setLength(0)) → Minor GC 압력 ↓
                  2. 컬렉션 초기 용량 지정 → 내부 배열 재할당 제거
                  3. Object Pool → 비싼 객체 재사용, 할당 횟수 ↓
                  4. 큰 객체(Humongous) 회피 → G1GC Humongous Region 직행 방지
                  5. 참조 조기 null → Old Gen 승격 전 수거 유도

                JVM 플래그 레벨 튜닝:
                  -Xms/-Xmx 동일하게 → Heap 크기 동적 조정 오버헤드 제거
                  -XX:NewRatio=2  → Young:Old = 1:2 (기본)
                  -XX:SurvivorRatio=8 → Eden:S0:S1 = 8:1:1
                  -XX:MaxGCPauseMillis=200 → G1GC STW 목표 시간
                  -XX:+UseStringDeduplication → G1GC에서 중복 String 제거

                StringBuilder 재사용 효과: %d → %d ms (%d GC → %d GC)
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                """.formatted(noReuseTime, reuseTime, noReuseGc, reuseGc);
    }

    private static long getTotalGcCount() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionCount)
                .filter(c -> c >= 0)
                .sum();
    }
}
