package com.example.server1.thread;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * [단계 7] java.util.concurrent 패키지 핵심 도구
 *
 * java.util.concurrent (JUC):
 *   멀티스레드 환경에서 자주 쓰이는 동기화 도구들을 표준화한 패키지
 *   synchronized/wait/notify보다 고수준 추상화 → 더 안전하고 효율적
 *
 * 도구 분류:
 *   ┌─ 동기화 보조 ─────────────────────────────────────────────────┐
 *   │  CountDownLatch  : N개 작업 완료까지 대기 (1회용 관문)         │
 *   │  CyclicBarrier   : N개 스레드가 같은 지점에서 모임 (재사용 가능)│
 *   │  Semaphore       : 동시 접근 수 제한 (리소스 풀, Rate limiting) │
 *   │  Phaser          : 다단계 작업 동기화                          │
 *   └───────────────────────────────────────────────────────────────┘
 *   ┌─ 스레드 안전 컬렉션 ────────────────────────────────────────────┐
 *   │  ConcurrentHashMap  : 분할 잠금(Segment), HashMap보다 효율적   │
 *   │  CopyOnWriteArrayList: 읽기 무잠금, 쓰기 시 복사 (읽기 多 환경)│
 *   │  BlockingQueue      : Producer-Consumer 패턴용 큐              │
 *   └───────────────────────────────────────────────────────────────┘
 *   ┌─ 잠금 도구 ────────────────────────────────────────────────────┐
 *   │  ReentrantLock  : synchronized보다 유연한 명시적 잠금           │
 *   │  ReadWriteLock  : 읽기 다중 허용, 쓰기 독점                    │
 *   │  StampedLock    : ReadWriteLock + 낙관적 읽기 (Java 8+)        │
 *   └───────────────────────────────────────────────────────────────┘
 */
@Slf4j
public class Step7_ConcurrentPackage {

    public static String run() throws InterruptedException, BrokenBarrierException {
        log.info("=== [단계7] java.util.concurrent 패키지 시작 ===");

        StringBuilder result = new StringBuilder();
        result.append(demoCountDownLatch()).append("\n");
        result.append(demoCyclicBarrier()).append("\n");
        result.append(demoSemaphore()).append("\n");
        result.append(demoBlockingQueue()).append("\n");
        result.append(demoConcurrentHashMap()).append("\n");
        result.append(demoReadWriteLock()).append("\n");

        log.info("=== [단계7] 완료 ===");
        return result.toString();
    }

    // ── CountDownLatch ─────────────────────────────────────────────────────────
    // 사용 사례: "N개 작업이 모두 완료된 후 다음 단계로 진행"
    //   ex) 서버 시작 시 DB, Cache, MQ 연결이 모두 완료된 후 요청 수락
    //       여러 외부 API 병렬 호출 후 모든 결과 취합
    private static String demoCountDownLatch() throws InterruptedException {
        log.info("--- [CountDownLatch] 3개 작업 완료 대기 ---");

        int taskCount = 3;
        CountDownLatch latch = new CountDownLatch(taskCount); // 카운터 초기값 = 3
        List<String> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 1; i <= taskCount; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    log.info("  [Worker-{}] 작업 시작", id);
                    Thread.sleep(id * 100L);
                    results.add("결과-" + id);
                    log.info("  [Worker-{}] 작업 완료 → latch.countDown()", id);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown(); // 카운터 -1
                }
            }).start();
        }

        log.info("  [Main] latch.await() → 0이 될 때까지 대기");
        latch.await(); // 카운터가 0이 될 때까지 블록
        log.info("  [Main] 모든 작업 완료! 결과: {}", results);
        /*
         * 핵심: CountDownLatch는 1회용
         * 재사용 불가 → 다시 쓰려면 새로 생성해야 함
         * CyclicBarrier와의 차이: Latch는 기다리는 스레드 수 != 카운터 수
         */

        return "[CountDownLatch] N개 병렬 작업 완료 후 집계: " + results;
    }

    // ── CyclicBarrier ─────────────────────────────────────────────────────────
    // 사용 사례: "N개 스레드가 모두 특정 지점에 도달하면 동시에 다음 단계 진행"
    //   ex) 병렬 테스트에서 모든 스레드가 준비된 후 동시에 부하 시작
    //       단계별 처리: 1단계 모두 완료 후 2단계 동시 시작
    private static String demoCyclicBarrier() throws InterruptedException, BrokenBarrierException {
        log.info("--- [CyclicBarrier] 3개 스레드 동시 출발 ---");

        int parties = 3;
        // 모든 스레드가 barrier에 도달하면 실행할 액션 (선택사항)
        CyclicBarrier barrier = new CyclicBarrier(parties,
                () -> log.info("  [Barrier] 모두 집결! 다음 단계 시작"));

        CountDownLatch done = new CountDownLatch(parties);

        for (int i = 1; i <= parties; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    log.info("  [Runner-{}] 1단계 처리 중...", id);
                    Thread.sleep(id * 80L);
                    log.info("  [Runner-{}] barrier.await() 도착 → 다른 스레드 대기", id);
                    barrier.await(); // 모든 parties가 도달할 때까지 대기
                    log.info("  [Runner-{}] 2단계 시작 (동시에!)", id);
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }

        done.await();
        /*
         * CyclicBarrier vs CountDownLatch:
         *   - CyclicBarrier: 참여자 수 == 기다리는 수, 재사용 가능 (reset())
         *   - CountDownLatch: 카운터와 기다리는 스레드 수 무관, 1회용
         */
        return "[CyclicBarrier] 3개 스레드가 장벽(barrier)에서 집결 후 동시 출발 완료";
    }

    // ── Semaphore ──────────────────────────────────────────────────────────────
    // 사용 사례: "동시 접근 수를 제한"
    //   ex) DB 커넥션 풀 (최대 10개 동시 사용)
    //       외부 API Rate Limiting (초당 5건 제한)
    //       공유 파일 핸들러 (동시 접근 3개 제한)
    private static String demoSemaphore() throws InterruptedException {
        log.info("--- [Semaphore] 동시 3개만 접근 허용 ---");

        Semaphore semaphore = new Semaphore(3); // 허가(permit) 3개
        CountDownLatch done = new CountDownLatch(7);
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        AtomicInteger current = new AtomicInteger(0);

        for (int i = 1; i <= 7; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    semaphore.acquire(); // 허가 획득 (없으면 대기)
                    int c = current.incrementAndGet();
                    maxConcurrent.accumulateAndGet(c, Math::max);
                    log.info("  [Task-{}] 접근 허가 획득 (현재 동시 접근: {})", id, c);
                    Thread.sleep(150);
                    current.decrementAndGet();
                    semaphore.release(); // 허가 반납
                    log.info("  [Task-{}] 처리 완료, 허가 반납", id);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }

        done.await();
        log.info("  최대 동시 접근 수: {} (허가 3개 제한)", maxConcurrent.get());
        return "[Semaphore] 7개 요청 중 동시 3개만 허용. 최대 동시 접근: " + maxConcurrent.get();
    }

    // ── BlockingQueue (Producer-Consumer) ────────────────────────────────────
    // 사용 사례: "생산자-소비자 패턴"
    //   ex) 요청 큐잉, 비동기 이벤트 처리, 로그 비동기 기록
    //   BlockingQueue 종류:
    //     LinkedBlockingQueue: 연결 리스트, 선택적 용량 제한
    //     ArrayBlockingQueue : 배열, 고정 용량, 공정성 선택 가능
    //     PriorityBlockingQueue: 우선순위 기반
    //     SynchronousQueue  : 버퍼 0, put이 take 만날 때까지 블록
    //     DelayQueue        : 지연 만료 후 꺼낼 수 있음
    private static String demoBlockingQueue() throws InterruptedException {
        log.info("--- [BlockingQueue] Producer-Consumer 패턴 ---");

        BlockingQueue<String> queue = new LinkedBlockingQueue<>(5); // 용량 5
        CountDownLatch done = new CountDownLatch(1);

        // Producer 스레드
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 8; i++) {
                    String item = "item-" + i;
                    queue.put(item); // 큐가 꽉 차면 블록
                    log.info("  [Producer] {} 추가 (큐 크기: {})", item, queue.size());
                    Thread.sleep(50);
                }
                queue.put("DONE"); // 종료 신호
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Consumer 스레드
        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    String item = queue.take(); // 큐가 비면 블록
                    if ("DONE".equals(item)) break;
                    log.info("  [Consumer] {} 처리 (큐 크기: {})", item, queue.size());
                    Thread.sleep(120); // 소비가 생산보다 느림 → 큐 배압(backpressure) 체험
                }
                done.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();
        done.await(5, TimeUnit.SECONDS);

        return "[BlockingQueue] Producer(50ms)-Consumer(120ms) 속도 차이 → 큐가 배압 역할 수행";
    }

    // ── ConcurrentHashMap ─────────────────────────────────────────────────────
    // 사용 사례: 멀티스레드 환경의 공유 캐시, 카운터
    //   vs HashMap: 동기화 없음 → 동시 수정 시 데이터 손상
    //   vs Hashtable: 모든 메서드 synchronized → 성능 병목
    //   ConcurrentHashMap: 세그먼트(버킷) 수준 잠금 → 높은 동시성
    private static String demoConcurrentHashMap() throws InterruptedException {
        log.info("--- [ConcurrentHashMap] 동시 카운팅 ---");

        ConcurrentHashMap<String, AtomicInteger> counter = new ConcurrentHashMap<>();
        CountDownLatch done = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < 1000; j++) {
                        String key = "key-" + (j % 5);
                        // computeIfAbsent: 키 없으면 AtomicInteger 생성 (원자적)
                        counter.computeIfAbsent(key, k -> new AtomicInteger(0))
                               .incrementAndGet();
                    }
                } finally {
                    done.countDown();
                }
            }).start();
        }

        done.await();
        int total = counter.values().stream().mapToInt(AtomicInteger::get).sum();
        log.info("  10개 스레드 × 1000번 카운팅. 총합: {} (기대값: 10000)", total);
        /*
         * ConcurrentHashMap 주요 원자적 연산:
         *   computeIfAbsent(key, fn) : 없으면 생성, 원자적
         *   compute(key, fn)         : 항상 재계산, 원자적
         *   merge(key, val, fn)      : 없으면 val, 있으면 fn 적용, 원자적
         *   putIfAbsent(key, val)    : 없을 때만 put
         */
        return "[ConcurrentHashMap] 10스레드 × 1000번 동시 카운팅. 총합: " + total + " (손실 없음)";
    }

    // ── ReadWriteLock ─────────────────────────────────────────────────────────
    // 사용 사례: 읽기가 쓰기보다 훨씬 많은 공유 데이터
    //   ex) 설정값 캐시, 조회 많은 공유 맵
    //   읽기 잠금: 동시에 여러 스레드 허용
    //   쓰기 잠금: 독점 (읽기/쓰기 모두 대기)
    private static String demoReadWriteLock() throws InterruptedException {
        log.info("--- [ReadWriteLock] 읽기 병렬 / 쓰기 독점 ---");

        ReadWriteLock rwLock = new ReentrantReadWriteLock();
        Map<String, String> sharedData = new HashMap<>();
        sharedData.put("config", "v1");

        AtomicInteger concurrentReaders = new AtomicInteger(0);
        AtomicInteger maxConcurrentReaders = new AtomicInteger(0);
        CountDownLatch done = new CountDownLatch(8);

        // 읽기 스레드 6개 동시 실행
        for (int i = 1; i <= 6; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    rwLock.readLock().lock(); // 읽기 잠금: 다른 읽기와 동시 허용
                    int readers = concurrentReaders.incrementAndGet();
                    maxConcurrentReaders.accumulateAndGet(readers, Math::max);
                    log.info("  [Reader-{}] 읽기 시작 (동시 독자: {})", id, readers);
                    Thread.sleep(100);
                    String val = sharedData.get("config");
                    concurrentReaders.decrementAndGet();
                    log.info("  [Reader-{}] 읽기 완료: {}", id, val);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    rwLock.readLock().unlock();
                    done.countDown();
                }
            }).start();
        }

        // 쓰기 스레드 2개
        for (int i = 1; i <= 2; i++) {
            final int version = i + 1;
            new Thread(() -> {
                try {
                    Thread.sleep(50); // 읽기 스레드들이 먼저 시작하도록
                    rwLock.writeLock().lock(); // 쓰기 잠금: 모든 읽기/쓰기 완료 대기
                    log.info("  [Writer] 쓰기 잠금 획득 (모든 Reader 대기 중)");
                    sharedData.put("config", "v" + version);
                    Thread.sleep(50);
                    log.info("  [Writer] 쓰기 완료: v{}", version);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    rwLock.writeLock().unlock();
                    done.countDown();
                }
            }).start();
        }

        done.await(5, TimeUnit.SECONDS);
        log.info("  최대 동시 읽기 스레드 수: {}", maxConcurrentReaders.get());

        return "[ReadWriteLock] 읽기 동시 허용 (최대 " + maxConcurrentReaders.get() + "개), 쓰기 독점 보장";
    }
}
