package com.example.server1.thread;

import lombok.extern.slf4j.Slf4j;

/**
 * [단계 1] 스레드 기초 - Thread 생성 3가지 방법
 *
 * 핵심 개념:
 *   - 프로세스(Process): 실행 중인 프로그램 (독립된 메모리 공간)
 *   - 스레드(Thread) : 프로세스 안에서 실제 작업을 수행하는 단위
 *   - main 메서드 자체도 "main 스레드"라는 스레드 위에서 실행됨
 *
 * 스레드를 만드는 3가지 방법:
 *   방법1) Thread 클래스를 상속(extends)
 *   방법2) Runnable 인터페이스를 구현(implements)  ← 권장 (단일 상속 제한 없음)
 *   방법3) 람다(Lambda) 표현식                    ← 가장 간결
 */
@Slf4j
public class Step1_BasicThread {

    // ──────────────────────────────────────────────────────────────────────────
    // 방법 1: Thread 클래스 상속
    //   - Thread를 상속하면 run() 메서드를 오버라이드해서 스레드 작업을 정의
    //   - 단점: Java는 단일 상속이라 다른 클래스를 동시에 상속할 수 없음
    // ──────────────────────────────────────────────────────────────────────────
    static class MyThread extends Thread {

        private final String taskName; // 이 스레드가 수행할 작업 이름

        MyThread(String taskName) {
            this.taskName = taskName;
            this.setName("Thread-" + taskName); // 스레드에 이름 부여 (디버깅 시 구분용)
        }

        @Override
        public void run() {
            // run() 안의 코드가 "별도 스레드"에서 실행됨
            // Thread.currentThread().getName() : 현재 실행 중인 스레드 이름
            log.info("[방법1] {} 실행 중 | 스레드명: {}", taskName, Thread.currentThread().getName());

            try {
                Thread.sleep(500); // 0.5초 대기 (다른 스레드와 병렬 실행됨을 확인하기 위해)
            } catch (InterruptedException e) {
                // InterruptedException: 스레드가 sleep/wait 중에 interrupt() 를 받으면 발생
                Thread.currentThread().interrupt(); // interrupt 상태를 다시 설정 (best practice)
                log.warn("[방법1] 스레드 중단됨: {}", taskName);
            }

            log.info("[방법1] {} 완료", taskName);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 방법 2: Runnable 인터페이스 구현
    //   - Runnable은 run() 메서드 하나만 있는 함수형 인터페이스
    //   - 인터페이스 구현이라 다른 클래스 상속과 함께 사용 가능 → 권장 방식
    // ──────────────────────────────────────────────────────────────────────────
    static class MyRunnable implements Runnable {

        private final String taskName;

        MyRunnable(String taskName) {
            this.taskName = taskName;
        }

        @Override
        public void run() {
            // Runnable을 Thread에 감싸서 실행: new Thread(new MyRunnable(...)).start()
            log.info("[방법2] {} 실행 중 | 스레드명: {}", taskName, Thread.currentThread().getName());

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            log.info("[방법2] {} 완료", taskName);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 실행 진입점
    // ──────────────────────────────────────────────────────────────────────────
    public static String run() throws InterruptedException {
        log.info("=== [단계1] 기본 스레드 생성 시작 | 현재 스레드: {} ===",
                Thread.currentThread().getName());

        // ── 방법 1: Thread 상속 ──────────────────────────────────────────────
        MyThread t1 = new MyThread("작업A");
        // start()를 호출해야 새 스레드가 생성되고 run()이 별도 스레드에서 실행됨
        // run()을 직접 호출하면 현재 스레드(main)에서 순차 실행됨 → 절대 직접 호출 금지
        t1.start();

        // ── 방법 2: Runnable 구현 ─────────────────────────────────────────────
        Runnable r = new MyRunnable("작업B");
        Thread t2 = new Thread(r);    // Runnable을 Thread 로 감쌈
        t2.setName("Thread-작업B");   // 스레드 이름 설정
        t2.start();                   // 별도 스레드 시작

        // ── 방법 3: 람다 표현식 ───────────────────────────────────────────────
        // Runnable이 함수형 인터페이스이므로 람다로 간결하게 표현 가능
        Thread t3 = new Thread(() -> {
            log.info("[방법3] 람다 스레드 실행 중 | 스레드명: {}", Thread.currentThread().getName());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            log.info("[방법3] 람다 스레드 완료");
        }, "Thread-람다"); // Thread 생성자 두 번째 인자로 이름 설정
        t3.start();

        // ── join(): 스레드가 끝날 때까지 현재 스레드(main)가 기다림 ───────────
        // join() 없으면 main이 먼저 종료되어 로그가 뒤섞이거나 잘릴 수 있음
        t1.join();
        t2.join();
        t3.join();

        log.info("=== [단계1] 모든 스레드 완료 ===");

        // ── 핵심 정리 ────────────────────────────────────────────────────────
        return """
                [단계1 완료] 기본 스레드 생성
                - 방법1: Thread 상속 → run() 오버라이드
                - 방법2: Runnable 구현 → Thread(runnable).start()  ← 권장
                - 방법3: 람다 표현식 → new Thread(() -> { ... }).start()
                - start() : 새 스레드 생성 후 run() 실행
                - join()  : 해당 스레드가 끝날 때까지 현재 스레드 대기
                """;
    }
}
