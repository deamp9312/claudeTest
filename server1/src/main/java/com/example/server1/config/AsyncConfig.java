package com.example.server1.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * [단계 6 설정] Spring @Async 설정
 *
 * @EnableAsync : 애플리케이션에서 @Async 어노테이션 동작 활성화
 * AsyncConfigurer: 기본 Executor와 예외 처리기를 커스터마이징하는 인터페이스
 */
@Slf4j
@Configuration
@EnableAsync // @Async가 동작하려면 반드시 필요 (없으면 @Async 무시됨)
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 기본 @Async 스레드 풀 설정
     * - @Async 만 붙였을 때 사용되는 기본 Executor
     */
    @Override
    public Executor getAsyncExecutor() {
        return threadPoolTaskExecutor(); // 아래 정의한 커스텀 풀을 기본으로 사용
    }

    /**
     * 커스텀 ThreadPoolTaskExecutor 빈 등록
     * - 이름으로 지정 가능: @Async("threadPoolTaskExecutor")
     */
    @Bean("threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // corePoolSize: 기본적으로 유지할 스레드 수
        // 요청이 없어도 이 수만큼의 스레드는 항상 대기 중
        executor.setCorePoolSize(3);

        // maxPoolSize: 큐가 꽉 찼을 때 생성 가능한 최대 스레드 수
        executor.setMaxPoolSize(10);

        // queueCapacity: 스레드가 모두 바쁠 때 대기 가능한 작업 수
        // core 스레드가 모두 사용 중이면 큐에 쌓이고, 큐도 꽉 차면 max까지 스레드 추가
        executor.setQueueCapacity(50);

        // 스레드 이름 접두사 (모니터링/로그에서 식별 용이)
        executor.setThreadNamePrefix("async-worker-");

        // setWaitForTasksToCompleteOnShutdown: 애플리케이션 종료 시 실행 중인 작업 완료 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // awaitTerminationSeconds: 최대 대기 시간 (초)
        executor.setAwaitTerminationSeconds(30);

        // initialize(): ThreadPoolTaskExecutor 초기화 (빈으로 등록할 때 필수)
        executor.initialize();

        log.info("[AsyncConfig] 스레드 풀 초기화: core={}, max={}, queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * @Async 메서드에서 예외 발생 시 처리
     * - void 반환 @Async 메서드는 예외가 호출자에게 전파되지 않으므로 여기서 처리
     * - CompletableFuture 반환 시는 .exceptionally()로 처리 가능
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("[AsyncConfig] @Async 예외 발생 | 메서드: {} | 파라미터: {} | 오류: {}",
                        method.getName(), params, ex.getMessage(), ex);
    }
}
