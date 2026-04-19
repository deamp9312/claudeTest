package com.example.server3.controller;

import com.example.server3.gc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GC 학습 컨트롤러 (port: 8082)
 *  curl http://localhost:8082/gc/1
 *
 * 학습 목표: "서비스가 왜 갑자기 느려지는가" → GC Stop-the-World 이해
 *
 *   GET /gc/1  → JVM Heap 구조 (Young/Old/Metaspace, 객체 생애 주기)
 *   GET /gc/2  → Minor GC 유발 (Eden 압박, STW 체감)
 *   GET /gc/3  → Full GC 유발 (Old Gen 압박, 더 긴 STW)
 *   GET /gc/4  → 메모리 누수 패턴 (GC가 왜 못 치우는가)
 *   GET /gc/5  → GC 알고리즘 비교 (Serial/Parallel/G1/ZGC)
 *   GET /gc/6  → GC 튜닝 실전 (코드 레벨 최적화)
 *   GET /gc/all → 모든 단계 순차 실행
 */
@Slf4j
@RestController
@RequestMapping("/gc")
public class GcLearningController {

    @GetMapping("/1")
    public String step1() {
        log.info("=== API 호출: GC 단계1 - Heap 구조 ===");
        return Step1_HeapStructure.run();
    }

    @GetMapping("/2")
    public String step2() throws InterruptedException {
        log.info("=== API 호출: GC 단계2 - Minor GC ===");
        return Step2_MinorGcTrigger.run();
    }

    @GetMapping("/3")
    public String step3() throws InterruptedException {
        log.info("=== API 호출: GC 단계3 - Full GC ===");
        return Step3_FullGcTrigger.run();
    }

    @GetMapping("/4")
    public String step4() throws InterruptedException {
        log.info("=== API 호출: GC 단계4 - 메모리 누수 ===");
        return Step4_MemoryLeak.run();
    }

    @GetMapping("/5")
    public String step5() {
        log.info("=== API 호출: GC 단계5 - GC 알고리즘 ===");
        return Step5_GcAlgorithm.run();
    }

    @GetMapping("/6")
    public String step6() throws InterruptedException {
        log.info("=== API 호출: GC 단계6 - GC 튜닝 ===");
        return Step6_GcTuning.run();
    }

    @GetMapping("/all")
    public String allSteps() throws InterruptedException {
        log.info("=== API 호출: GC 전체 단계 실행 ===");
        StringBuilder sb = new StringBuilder();
        sb.append(Step1_HeapStructure.run()).append("\n\n");
        sb.append(Step2_MinorGcTrigger.run()).append("\n\n");
        sb.append(Step3_FullGcTrigger.run()).append("\n\n");
        sb.append(Step4_MemoryLeak.run()).append("\n\n");
        sb.append(Step5_GcAlgorithm.run()).append("\n\n");
        sb.append(Step6_GcTuning.run()).append("\n");
        return sb.toString();
    }
}
