package com.example.server3.gc;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

/**
 * [단계 1] JVM Heap 구조 이해 - "왜 GC가 느림을 유발하는가"의 출발점
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │                        JVM Heap                             │
 * │  ┌─────────────────────────────┐  ┌──────────────────────┐ │
 * │  │       Young Generation       │  │   Old Generation     │ │
 * │  │  ┌───────┬──────┬────────┐  │  │  (오래 사는 객체)    │ │
 * │  │  │ Eden  │  S0  │  S1    │  │  │                      │ │
 * │  │  │(새 객체│(생존자│(생존자 │  │  │                      │ │
 * │  │  │ 할당)  │  0)  │  1)    │  │  │                      │ │
 * │  │  └───────┴──────┴────────┘  │  └──────────────────────┘ │
 * │  └─────────────────────────────┘                            │
 * │                        + Metaspace (클래스 메타데이터)       │
 * └─────────────────────────────────────────────────────────────┘
 *
 * 객체 생애 주기:
 *   1. 새 객체 → Eden 할당
 *   2. Eden 꽉 참 → Minor GC 발생
 *   3. 살아남은 객체 → Survivor (S0 ↔ S1 교대)
 *   4. age 임계치(기본 15) 초과 → Old Gen으로 Promotion
 *   5. Old Gen 꽉 참 → Full GC (이게 느림의 핵심!)
 *
 * Stop-the-World (STW):
 *   GC가 동작하는 동안 모든 애플리케이션 스레드가 멈춤
 *   → Minor GC STW: 수 ms ~ 수십 ms
 *   → Full GC STW: 수백 ms ~ 수 초 (힙 클수록 길어짐)
 *   → 이 멈춤이 "왜 갑자기 느리냐"의 답
 */
@Slf4j
public class Step1_HeapStructure {

    public static String run() {
        log.info("=== [단계1] JVM Heap 구조 분석 시작 ===");

        // ── 현재 Heap 상태 조회 ───────────────────────────────────────────────
        Runtime runtime = Runtime.getRuntime();
        long maxHeap = runtime.maxMemory();
        long totalHeap = runtime.totalMemory();
        long freeHeap = runtime.freeMemory();
        long usedHeap = totalHeap - freeHeap;

        log.info("[Heap 현황]");
        log.info("  최대 Heap (Xmx)  : {} MB", toMB(maxHeap));
        log.info("  현재 할당된 Heap  : {} MB", toMB(totalHeap));
        log.info("  사용 중           : {} MB", toMB(usedHeap));
        log.info("  여유              : {} MB", toMB(freeHeap));

        // ── MemoryMXBean으로 Heap/Non-Heap 구분 조회 ─────────────────────────
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage(); // Metaspace

        log.info("[Heap 상세]");
        log.info("  Heap used/committed/max = {}/{}/{} MB",
                toMB(heapUsage.getUsed()),
                toMB(heapUsage.getCommitted()),
                toMB(heapUsage.getMax()));
        log.info("[Non-Heap (Metaspace 포함)]");
        log.info("  NonHeap used = {} MB", toMB(nonHeapUsage.getUsed()));

        // ── GC 종류 확인 (실행 중인 GC 알고리즘) ─────────────────────────────
        log.info("[현재 활성화된 GC 수집기]");
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gc : gcBeans) {
            log.info("  GC 이름: {} | 수집 횟수: {} | 총 소요시간: {} ms",
                    gc.getName(), gc.getCollectionCount(), gc.getCollectionTime());
        }
        /*
         * 일반적으로 2개 GC 수집기가 보임:
         *   G1 Young Generation → Minor GC 담당
         *   G1 Old Generation   → Major/Full GC 담당
         */

        // ── 객체 생성 → 세대 이동 시뮬레이션 ────────────────────────────────
        log.info("[객체 생애 주기 시뮬레이션]");
        log.info("  단기 객체(로컬 변수) 1만 개 생성 → 즉시 참조 해제 → Eden 대부분에서 처리됨");
        for (int i = 0; i < 10_000; i++) {
            String shortLived = "temp-" + i; // 즉시 참조 해제 → GC 대상
        }

        log.info("  장기 객체(static 리스트) 생성 → Old Gen으로 승격 가능");
        List<byte[]> longLived = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            longLived.add(new byte[1024]); // 1KB씩, age 증가하며 Old Gen으로 이동
        }
        longLived.clear(); // 참조 해제

        log.info("=== [단계1] 완료 ===");

        return """
                [단계1 완료] JVM Heap 구조
                ┌─ Young Gen (Eden + S0 + S1) ─────────────────────────────────┐
                │  새 객체 할당 → Eden 꽉 참 → Minor GC → 생존자 Survivor 이동 │
                │  age 임계치 초과 → Old Gen으로 Promotion                      │
                └───────────────────────────────────────────────────────────────┘
                ┌─ Old Gen ─────────────────────────────────────────────────────┐
                │  오래 살아남은 객체 → 여기 꽉 차면 Full GC 발생              │
                │  Full GC = Stop-the-World 시간이 길다 = 서비스 응답 지연      │
                └───────────────────────────────────────────────────────────────┘
                현재 Heap: %d MB 사용 / %d MB 할당 / %d MB 최대
                """.formatted(toMB(usedHeap), toMB(totalHeap), toMB(maxHeap));
    }

    private static long toMB(long bytes) {
        return bytes / (1024 * 1024);
    }
}
