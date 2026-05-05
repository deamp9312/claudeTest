package com.example.server5;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching   // Redis 캐시 활성화
@EnableAsync     // 비동기 Kafka 전송 활성화
public class Server5Application {
    public static void main(String[] args) {
        SpringApplication.run(Server5Application.class, args);
    }
}
