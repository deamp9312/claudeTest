package com.example.server3.gc;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * [단계 5] GC 알고리즘 비교 - "어떤 GC를 쓰느냐에 따라 왜 느림이 달라지는가"
 *
 * GC 알고리즘 선택이 중요한 이유:
 *   같은 코드라도 GC 알고리즘에 따라 STW 시간, 처리량, 메모리 효율이 크게 다름
 *   → 서비스 특성(처리량 우선 vs 응답시간 우선)에 맞는 GC 선택 필요
 *
 * ┌─────────────────┬──────────────┬────────────────┬──────────────────────────┐
 * │ GC 알고리즘      │ JVM 플래그   │ STW 특성        │ 적합한 상황              │
 * ├─────────────────┼──────────────┼────────────────┼──────────────────────────┤
 * │ Serial GC       │ -XX:+UseSerialGC │ 길고 단순    │ 단일 CPU, 소형 앱        │
 * │ Parallel GC     │ -XX:+UseParallelGC │ 짧지만 빈번 │ 배치처리, 처리량 우선   │
 * │ G1GC            │ -XX:+UseG1GC │ 예측 가능       │ Java 9+ 기본, 범용       │
 * │ ZGC             │ -XX:+UseZGC  │ < 1ms          │ 대용량 힙, 저지연 필수   │
 * │ Shenandoah      │ -XX:+UseShenandoahGC │ < 10ms  │ 저지연, Red Hat 개발     │
 * └─────────────────┴──────────────┴────────────────┴──────────────────────────┘
 */
@Slf4j
public class Step5_GcAlgorithm {

    public static String run() {
        log.info("=== [단계5] GC 알고리즘 분석 ===");

        // ── 현재 실행 중인 GC 확인 ───────────────────────────────────────────
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        log.info("[현재 JVM에서 실행 중인 GC 수집기]");
        String currentGc = "";
        for (GarbageCollectorMXBean gc : gcBeans) {
            log.info("  → {} (수집 {} 회, 총 {} ms)", gc.getName(),
                    gc.getCollectionCount(), gc.getCollectionTime());
            currentGc += gc.getName() + " ";
        }

        // ── 각 알고리즘 상세 설명 ────────────────────────────────────────────
        log.info("[Serial GC] -XX:+UseSerialGC");
        log.info("  - 단일 스레드로 GC 수행 → 멀티코어 서버에서 비효율");
        log.info("  - Minor GC: 단일 스레드로 Young Gen 수거");
        log.info("  - Full GC : 단일 스레드로 전체 Heap Mark-Sweep-Compact");
        log.info("  - STW 길이: 매우 김 (단일 스레드 + Compact)");
        log.info("  - 사용 케이스: CLI 툴, 단순 배치, 힙 < 100MB");

        log.info("[Parallel GC] -XX:+UseParallelGC (Java 8 기본)");
        log.info("  - 멀티 스레드로 GC 수행 → 처리량(throughput) 극대화");
        log.info("  - Minor GC: 멀티 스레드로 Young Gen 병렬 수거");
        log.info("  - Full GC : 멀티 스레드로 전체 Heap 병렬 Compact");
        log.info("  - STW 길이: Serial보다 짧지만, Full GC 시 여전히 수백 ms");
        log.info("  - 사용 케이스: 배치 처리, 응답시간 < 처리량이 중요한 경우");
        log.info("  - 튜닝: -XX:ParallelGCThreads=N (GC 스레드 수)");

        log.info("[G1GC] -XX:+UseG1GC (Java 9+ 기본)");
        log.info("  - Heap을 Region(1~32MB)으로 나눔 → Young/Old 경계 유동적");
        log.info("  ┌── Region ──┬── Region ──┬── Region ──┬── Region ──┐");
        log.info("  │   Eden     │  Survivor  │    Old     │  Humongous │");
        log.info("  └────────────┴────────────┴────────────┴────────────┘");
        log.info("  - 핵심 특성: STW 목표 시간 설정 가능 (-XX:MaxGCPauseMillis=200)");
        log.info("  - Concurrent Marking: 앱 스레드와 동시에 살아있는 객체 마킹");
        log.info("  - 가장 가비지 많은 Region 우선 수거 → 효율적 (G = Garbage First)");
        log.info("  - STW 길이: 수십~200ms (목표 달성 못할 수도 있음)");
        log.info("  - Full GC 트리거: Old Region이 Heap의 45% 초과 (-XX:InitiatingHeapOccupancyPercent)");

        log.info("[ZGC] -XX:+UseZGC (Java 15+ 프로덕션 준비)");
        log.info("  - 거의 모든 작업을 애플리케이션 스레드와 동시에 수행");
        log.info("  - Colored Pointers: 객체 참조에 메타비트를 심어 동시 마킹");
        log.info("  - Load Barriers: 객체 접근 시 GC 상태 자동 체크");
        log.info("  - STW 길이: < 1ms (힙 크기와 무관!)");
        log.info("  - 단점: CPU 오버헤드 증가 (동시 작업), 처리량 약간 감소");
        log.info("  - 사용 케이스: 대용량 힙(수십~수백GB), 낮은 지연시간 필수 서비스");
        log.info("  - 튜닝: -XX:SoftMaxHeapSize (ZGC가 유지하려는 힙 크기 목표)");

        log.info("[GC 선택 가이드]");
        log.info("  응답시간 우선 (API 서버, 실시간) → ZGC 또는 G1GC");
        log.info("  처리량 우선 (배치, 데이터 처리) → Parallel GC");
        log.info("  범용 (대부분의 Spring 앱)       → G1GC (기본값)");
        log.info("  힙 < 100MB, 단순 앱              → Serial GC");

        // ── GC 로깅 설정 안내 ─────────────────────────────────────────────────
        log.info("[GC 모니터링 JVM 플래그]");
        log.info("  -Xlog:gc*:file=gc.log:time,uptime,level,tags  → GC 로그 파일 출력");
        log.info("  -XX:+PrintGCDetails (Java 8)                   → 상세 GC 출력");
        log.info("  -XX:+HeapDumpOnOutOfMemoryError                → OOM 시 힙 덤프");
        log.info("  -XX:HeapDumpPath=/tmp/heap.hprof               → 덤프 경로 지정");

        log.info("=== [단계5] 완료 ===");

        return """
                [단계5 완료] GC 알고리즘 비교
                현재 JVM GC: %s
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                알고리즘     │ STW 길이  │ 처리량  │ 사용 케이스
                ─────────────┼──────────┼────────┼──────────────────
                Serial GC    │ 매우 김  │ 낮음   │ 소형 앱, 단일 CPU
                Parallel GC  │ 중간     │ 높음   │ 배치, 처리량 우선
                G1GC (기본)  │ 예측가능 │ 중상   │ 범용 (Java 9+)
                ZGC          │ < 1ms    │ 중간   │ 저지연, 대용량 힙
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                빠른 선택:
                  Spring API 서버 → -XX:+UseG1GC -XX:MaxGCPauseMillis=200
                  초저지연 필요   → -XX:+UseZGC
                  GC 로그 활성화  → -Xlog:gc*:file=gc.log:time,uptime
                """.formatted(currentGc.trim());
    }
}
