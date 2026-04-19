package com.example.server3.gc;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * [단계 3] Full GC (Major GC) - "서비스가 순간 멈추는" 진짜 원인
 *
 * Full GC 발생 조건:
 *   1. Old Gen이 꽉 찼을 때
 *   2. System.gc() 명시적 호출 (-XX:+DisableExplicitGC로 막을 수 있음)
 *   3. Metaspace 부족
 *   4. Promotion Failure: Minor GC 중 Old Gen에 공간 없어 Promotion 실패
 *   5. Concurrent Mode Failure: CMS GC에서 동시 수집이 Old Gen 꽉 참을 못 따라갈 때
 *
 * Full GC 동작:
 *   1. 모든 스레드 중지 (STW 시작) ← Minor GC보다 훨씬 길다
 *   2. Young Gen + Old Gen 전체 대상으로 살아있는 객체 탐색 (Mark)
 *   3. 사용하지 않는 객체 제거 (Sweep)
 *   4. 메모리 압축 (Compact) ← 이 단계가 특히 오래 걸림
 *   5. 모든 스레드 재개 (STW 종료)
 *
 * 왜 Full GC STW가 긴가:
 *   - 탐색 대상이 힙 전체 (Young + Old)
 *   - Old Gen은 크고 조각화(fragmentation) 심함
 *   - Compact 단계: 살아있는 객체를 한쪽으로 밀어 연속 공간 확보 → 모든 참조 업데이트 필요
 *   → Heap이 4GB면 STW 수 초도 가능 → 서비스 완전 중단처럼 보임
 *
 * 실무에서 Full GC 줄이는 방법:
 *   - Old Gen에 오래 살아남는 객체 수를 줄임
 *   - Heap 크기를 충분히 줌 (Xmx 증가)
 *   - G1GC/ZGC 사용 (STW 최소화 알고리즘)
 *   - 캐시를 Heap 외부 (off-heap, Redis)로 이동
 */
@Slf4j
public class Step3_FullGcTrigger {

    // Old Gen으로 Promotion 유도용 장수 객체 보관소
    private static final List<byte[]> oldGenResident = new ArrayList<>();

    public static String run() throws InterruptedException {
        log.info("=== [단계3] Full GC 유발 실험 시작 ===");

        // ── 실험 1: Old Gen 압박 (장수 객체 누적) ─────────────────────────────
        log.info("[실험1] Old Gen 압박: 장수 객체를 누적해 Promotion 유발");
        log.info("  → Young Gen에서 살아남은 객체가 Old Gen으로 이동하는 원리 시뮬레이션");

        long gcCountBefore = Step2_MinorGcTrigger.getTotalGcCount();
        long gcTimeBefore = Step2_MinorGcTrigger.getTotalGcTime();
        long startMs = System.currentTimeMillis();

        // 여러 라운드에 걸쳐 살아남는 객체 축적 → age 증가 → Old Gen Promotion 유도
        for (int round = 0; round < 20; round++) {
            // 라운드마다 단기 객체 대량 생성 → Minor GC 유발
            for (int i = 0; i < 50_000; i++) {
                byte[] ignored = new byte[256]; // 즉시 해제
            }
            // 일부는 계속 살려둠 → Survivor를 거쳐 Old Gen으로 이동
            oldGenResident.add(new byte[10 * 1024]); // 10KB
        }

        long elapsed = System.currentTimeMillis() - startMs;
        long gcCountAfter = Step2_MinorGcTrigger.getTotalGcCount();
        long gcTimeAfter = Step2_MinorGcTrigger.getTotalGcTime();

        log.info("  누적 장수 객체: {} 개 (약 {} KB)",
                oldGenResident.size(), oldGenResident.size() * 10);
        log.info("  소요 시간: {} ms | GC {} 회 | STW 총 {} ms",
                elapsed, gcCountAfter - gcCountBefore, gcTimeAfter - gcTimeBefore);

        // ── 실험 2: System.gc() 직접 호출 → Full GC 요청 ─────────────────────
        log.info("[실험2] System.gc() 직접 호출 (Full GC 요청)");
        log.info("  → 실무에서는 절대 호출 금지! 단지 Full GC 체감용");

        gcCountBefore = Step2_MinorGcTrigger.getTotalGcCount();
        gcTimeBefore = Step2_MinorGcTrigger.getTotalGcTime();
        startMs = System.currentTimeMillis();

        System.gc(); // JVM에 Full GC 힌트 (-XX:+DisableExplicitGC로 무시 가능)
        Thread.sleep(500); // GC 완료 대기

        elapsed = System.currentTimeMillis() - startMs;
        gcCountAfter = Step2_MinorGcTrigger.getTotalGcCount();
        gcTimeAfter = Step2_MinorGcTrigger.getTotalGcTime();

        Runtime rt = Runtime.getRuntime();
        long usedAfterGc = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);

        log.info("  Full GC 소요 시간: {} ms | GC {} 회 | STW {} ms",
                elapsed, gcCountAfter - gcCountBefore, gcTimeAfter - gcTimeBefore);
        log.info("  GC 후 Heap 사용량: {} MB", usedAfterGc);
        /*
         * 핵심 관찰 포인트:
         * - Full GC STW 시간이 Minor GC보다 훨씬 길다
         * - GC 후 Heap 사용량이 크게 감소 (대규모 정리)
         */

        // ── 실험 3: Promotion Failure 시뮬레이션 설명 ────────────────────────
        log.info("[실험3] Promotion Failure 상황 설명");
        log.info("  상황: Minor GC 중 Old Gen 남은 공간 < 이동할 객체 크기");
        log.info("  결과: JVM이 Full GC를 강제 실행 (Promotion Failure → Full GC)");
        log.info("  실무 증상: Minor GC인 줄 알았는데 갑자기 수백 ms 멈춤");
        log.info("  해결: Old Gen 크기 늘리기 (-Xmx) 또는 장수 객체 수 줄이기");

        // 정리
        oldGenResident.clear();

        log.info("=== [단계3] 완료 ===");

        return """
                [단계3 완료] Full GC 체감
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                Full GC STW vs Minor GC STW:
                  Minor GC: 수 ms ~ 수십 ms (Young Gen만 대상)
                  Full GC : 수백 ms ~ 수 초  (Heap 전체 대상)
                  → 힙이 클수록, 살아있는 객체 많을수록 더 길어짐

                Full GC가 느린 이유:
                  1. Heap 전체 스캔 (크다)
                  2. Compact 단계: 메모리 조각 모음 + 모든 참조 갱신
                  3. 모든 앱 스레드가 이 시간 동안 멈춤

                실무 조치:
                  - G1GC (-XX:+UseG1GC): STW 목표 시간 설정 가능
                  - ZGC  (-XX:+UseZGC) : STW < 1ms (Java 15+)
                  - 힙 크기 여유 확보 (-Xmx)
                  - System.gc() 호출 제거 + -XX:+DisableExplicitGC 설정
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                Full GC STW: %d ms (이 시간만큼 API 응답이 멈춤)
                """.formatted(gcTimeAfter - gcTimeBefore);
    }
}
