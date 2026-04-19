package com.example.server3.gc;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * [단계 4] 메모리 누수 - "GC가 있는데 왜 OOM이 나는가"
 *
 * GC의 전제 조건:
 *   GC는 "도달 불가능한 객체(unreachable)"만 수거한다.
 *   = GC Root에서 참조 체인이 끊긴 객체만 제거 가능
 *
 * GC Root 종류:
 *   - static 변수 (클래스가 로드된 동안 영구 참조)
 *   - 스택 변수 (메서드 실행 중인 로컬 변수)
 *   - JNI 참조
 *   - 활성 스레드
 *
 * 메모리 누수 = "필요 없지만 GC Root에서 여전히 참조되는 객체"
 *   → GC가 존재해도 수거 불가 → 점진적 Heap 증가 → 결국 Full GC 폭발 → OOM
 *
 * 대표적 메모리 누수 패턴:
 *   1. static 컬렉션에 계속 추가 (제거 없음)
 *   2. ThreadLocal 미제거 (스레드 풀 환경에서 치명적)
 *   3. 리스너/콜백 미해제
 *   4. 내부 클래스가 외부 클래스 참조 유지
 *   5. equals/hashCode 깨진 객체를 HashMap 키로 사용
 */
@Slf4j
public class Step4_MemoryLeak {

    // ── 패턴 1: static 컬렉션 누수 ───────────────────────────────────────────
    // static이므로 클래스가 살아있는 한 GC Root → 절대 수거 안 됨
    private static final Map<String, byte[]> staticCache = new HashMap<>();

    // ── 패턴 2: ThreadLocal 누수 ─────────────────────────────────────────────
    // 스레드 풀에서 스레드가 재사용될 때 이전 데이터 잔존
    private static final ThreadLocal<List<byte[]>> threadLocalData = new ThreadLocal<>();

    public static String run() throws InterruptedException {
        log.info("=== [단계4] 메모리 누수 패턴 분석 시작 ===");

        Runtime rt = Runtime.getRuntime();

        // ── 패턴 1: static 컬렉션 누수 시연 ──────────────────────────────────
        log.info("[패턴1] static HashMap 누수 시연");
        log.info("  → static 변수는 GC Root. 여기 담긴 객체는 절대 수거 불가");

        long usedBefore = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);

        for (int i = 0; i < 1000; i++) {
            String key = "session-" + i;
            byte[] sessionData = new byte[5 * 1024]; // 5KB per entry
            staticCache.put(key, sessionData); // static → GC 불가
        }

        long usedAfter = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        log.info("  1000건 추가 후 Heap 증가: {} → {} MB ({}MB 증가)",
                usedBefore, usedAfter, usedAfter - usedBefore);
        log.info("  System.gc() 호출해도 staticCache 항목은 수거 안 됨 (GC Root 참조)");

        System.gc();
        Thread.sleep(200);
        long usedAfterGc = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        log.info("  GC 후 Heap: {} MB → 누수 데이터 {} MB 잔존", usedAfterGc, usedAfterGc);

        staticCache.clear(); // 해제 (실제로는 이게 안 되는 경우가 문제)

        // ── 패턴 2: ThreadLocal 누수 시연 ─────────────────────────────────────
        log.info("[패턴2] ThreadLocal 누수 시연");
        log.info("  → 스레드 풀에서 remove() 없이 재사용하면 이전 데이터 잔존");

        Thread worker = new Thread(() -> {
            // 스레드에 데이터 바인딩
            List<byte[]> data = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                data.add(new byte[1024]);
            }
            threadLocalData.set(data);

            log.info("  [Worker] ThreadLocal 설정됨 (100KB)");
            // remove() 호출을 의도적으로 생략 → 누수 발생
            // threadLocalData.remove(); ← 이게 없으면 스레드 종료 후에도 잔존 가능
        });
        worker.start();
        worker.join();
        log.info("  [Worker] 종료 후 ThreadLocal 데이터: {}",
                threadLocalData.get() == null ? "null(정상)" : "잔존(누수!)");

        // ── 패턴 3: equals/hashCode 깨진 키 누수 ────────────────────────────
        log.info("[패턴3] HashMap 키 누수 (equals/hashCode 미구현)");
        log.info("  → 같은 논리적 키인데 다른 객체로 인식 → remove 불가");

        Map<BadKey, String> leakyMap = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            leakyMap.put(new BadKey("key"), "value-" + i); // 5개 다 다른 키로 인식
        }
        log.info("  BadKey 'key'로 5번 put → map 크기: {} (정상이면 1이어야 함)", leakyMap.size());
        boolean removed = leakyMap.remove(new BadKey("key")) != null;
        log.info("  remove 시도 → 성공: {} (실패하면 영구 잔존)", removed);

        // ── 패턴 4: 내부 클래스 참조 누수 설명 ──────────────────────────────
        log.info("[패턴4] 익명/내부 클래스 누수 (설명)");
        log.info("  → 익명 Runnable이 외부 Activity/Fragment 참조 시 GC 불가");
        log.info("  Android/Swing에서 흔함. 해결: static inner class + WeakReference 사용");

        log.info("=== [단계4] 완료 ===");

        return """
                [단계4 완료] 메모리 누수 패턴
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                GC의 한계: GC Root에서 참조되는 객체는 수거 불가

                주요 누수 패턴:
                  1. static 컬렉션 무한 추가 → clear() 없으면 영구 잔존
                  2. ThreadLocal.remove() 미호출 → 스레드 풀 환경에서 치명적
                  3. equals/hashCode 미구현 키 → HashMap에서 remove 불가
                  4. 익명 클래스가 외부 객체 참조 → 외부 객체 GC 불가

                누수 증상:
                  - Heap 사용량이 GC 후에도 점진적 증가
                  - Full GC 빈도 증가, 결국 OOM (java.lang.OutOfMemoryError)

                진단 도구:
                  - jmap -histo <pid>: 객체 유형별 메모리 사용량
                  - VisualVM / Eclipse MAT: Heap dump 분석
                  - -XX:+HeapDumpOnOutOfMemoryError: OOM 시 자동 덤프
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                """;
    }

    // equals/hashCode를 구현하지 않은 나쁜 키 예제
    static class BadKey {
        final String value;

        BadKey(String value) {
            this.value = value;
        }
        // equals/hashCode 없음 → Object 기본 구현 사용 → 객체 주소 기반 비교
        // → 논리적으로 같아도 다른 키로 취급
    }
}
