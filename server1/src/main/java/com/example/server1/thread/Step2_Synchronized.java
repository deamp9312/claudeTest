package com.example.server1.thread;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * [단계 2] 동기화(Synchronization) - 공유 자원 보호
 *
 * 핵심 개념:
 *   - Race Condition(경쟁 상태): 여러 스레드가 동시에 같은 데이터를 읽고/쓸 때 결과가 예측 불가능해지는 현상
 *   - Critical Section(임계 구역): Race Condition이 발생할 수 있는 코드 블록
 *   - Monitor Lock: 객체마다 존재하는 자물쇠. synchronized 블록 진입 시 획득, 종료 시 반납
 *
 * 해결 방법 3가지:
 *   방법1) synchronized 키워드 (메서드/블록 레벨 락)
 *   방법2) volatile 키워드  (가시성 보장, 단순 플래그에만 적합)
 *   방법3) AtomicInteger    (CAS 방식의 락-프리 원자적 연산, 가장 성능 좋음)
 */
@Slf4j
public class Step2_Synchronized {

    // ──────────────────────────────────────────────────────────────────────────
    // [문제 상황] synchronized 없는 카운터 → Race Condition 발생
    // ──────────────────────────────────────────────────────────────────────────
    static class UnsafeCounter {
        private int count = 0; // 여러 스레드가 동시에 접근하는 공유 변수

        // synchronized 없음 → count++ 연산이 원자적(atomic)이지 않음
        // count++는 실제로 3단계: ① count 읽기 → ② +1 계산 → ③ count 저장
        // 두 스레드가 ①을 동시에 하면 같은 값을 읽고 같은 값을 쓰게 됨 → 누락 발생
        public void increment() {
            count++;
        }

        public int getCount() {
            return count;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // [해결 방법1] synchronized 메서드
    //   - 메서드 전체를 임계 구역으로 지정
    //   - this 객체의 monitor lock을 사용
    //   - 한 스레드가 진입하면 다른 스레드는 대기(BLOCKED 상태)
    // ──────────────────────────────────────────────────────────────────────────
    static class SynchronizedMethodCounter {
        private int count = 0;

        // synchronized 키워드: 이 메서드는 한 번에 하나의 스레드만 실행 가능
        public synchronized void increment() {
            count++; // 이제 읽기-계산-쓰기가 원자적으로 처리됨
        }

        public synchronized int getCount() {
            return count; // 읽기도 synchronized로 최신값 보장
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // [해결 방법2] synchronized 블록
    //   - 메서드 일부만 임계 구역으로 지정 → 락 범위를 최소화해 성능 향상
    //   - 락 객체를 명시적으로 지정 가능
    // ──────────────────────────────────────────────────────────────────────────
    static class SynchronizedBlockCounter {
        private int count = 0;
        private final Object lock = new Object(); // 전용 락 객체 (this 대신 사용)

        public void increment() {
            // synchronized 블록: lock 객체의 monitor를 획득한 스레드만 진입
            // 블록을 벗어나면 자동으로 lock 반납 (예외 발생해도 반납됨)
            synchronized (lock) {
                count++;
            }
            // 락이 필요 없는 코드는 블록 밖에 두어 동시성 향상
        }

        public int getCount() {
            synchronized (lock) {
                return count;
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // [해결 방법3] AtomicInteger - CAS(Compare-And-Swap) 방식
    //   - 락(Lock) 없이 CPU 명령어 수준에서 원자적 연산 보장
    //   - synchronized보다 경합이 낮을 때 성능이 더 좋음
    //   - java.util.concurrent.atomic 패키지 제공
    // ──────────────────────────────────────────────────────────────────────────
    static class AtomicCounter {
        // AtomicInteger: 내부적으로 CAS(Compare-And-Swap)을 사용
        // CAS: "현재 값이 예상값과 같으면 새 값으로 교체" → 실패 시 재시도
        private final AtomicInteger count = new AtomicInteger(0);

        public void increment() {
            count.incrementAndGet(); // 원자적으로 +1 하고 새 값 반환
        }

        public int getCount() {
            return count.get(); // 현재 값 반환
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 테스트: 10개 스레드가 각각 1000번 증가 → 기대값 10,000
    // ──────────────────────────────────────────────────────────────────────────
    private static int runTest(String label, Runnable incrementAction, java.util.function.Supplier<Integer> getCount)
            throws InterruptedException {

        int threadCount = 10;   // 스레드 수
        int loopCount = 1000;   // 스레드당 반복 횟수

        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < loopCount; j++) {
                    incrementAction.run(); // 각 스레드가 1000번 증가
                }
            });
            threads[i].start();
        }

        // 모든 스레드가 끝날 때까지 대기
        for (Thread t : threads) {
            t.join();
        }

        int result = getCount.get();
        log.info("[{}] 기대값: {} | 실제값: {} | {}",
                label, threadCount * loopCount, result,
                result == threadCount * loopCount ? "✓ 정확" : "✗ Race Condition 발생!");
        return result;
    }

    public static String run() throws InterruptedException {
        log.info("=== [단계2] 동기화(Synchronization) 시작 ===");

        // 비안전 카운터 (Race Condition 발생 → 10000보다 작을 가능성 높음)
        UnsafeCounter unsafe = new UnsafeCounter();
        runTest("비안전 카운터", unsafe::increment, unsafe::getCount);

        // synchronized 메서드 (항상 10000)
        SynchronizedMethodCounter syncMethod = new SynchronizedMethodCounter();
        runTest("synchronized 메서드", syncMethod::increment, syncMethod::getCount);

        // synchronized 블록 (항상 10000)
        SynchronizedBlockCounter syncBlock = new SynchronizedBlockCounter();
        runTest("synchronized 블록", syncBlock::increment, syncBlock::getCount);

        // AtomicInteger (항상 10000, 성능 최고)
        AtomicCounter atomic = new AtomicCounter();
        runTest("AtomicInteger", atomic::increment, atomic::getCount);

        log.info("=== [단계2] 완료 ===");

        return """
                [단계2 완료] 동기화(Synchronization)
                - Race Condition : 여러 스레드가 공유 자원 동시 접근 시 결과 불일치
                - synchronized  : 메서드/블록에 락을 걸어 한 번에 1개 스레드만 실행
                - synchronized 블록 : 락 범위를 최소화해 성능 개선
                - AtomicInteger : 락 없이 CAS로 원자적 연산 → 단순 카운터에 최적
                """;
    }
}
