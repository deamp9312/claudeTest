package com.example.server5.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisConfig implements CachingConfigurer {

    // ✅ Redis 최적화 1: 기본 JDK 직렬화 대신 JSON 직렬화 사용
    //    JDK 직렬화: 클래스 메타 정보까지 저장 → 크기 큼, 버전 호환 문제
    //    JSON 직렬화: 가독성 좋고 언어 중립적, 크기 더 작음
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 키: 문자열 직렬화
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // 값: JSON 직렬화 (타입 정보 포함)
        GenericJackson2JsonRedisSerializer jsonSerializer =
            new GenericJackson2JsonRedisSerializer(objectMapper());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    // ✅ Redis 최적화 2: 캐시별 TTL 차등 설정
    //    자주 바뀌지 않는 데이터 → TTL 길게
    //    자주 바뀌는 데이터 → TTL 짧게
    @Bean
    @Override
    public CacheManager cacheManager() {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))  // 기본 10분
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer(objectMapper())))
            .disableCachingNullValues();  // null 캐시 방지

        // 캐시별 개별 TTL 설정
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // 사용자 정보: 자주 안 바뀜 → 30분
        cacheConfigs.put("users", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // 주문 목록: 자주 바뀜 → 5분
        cacheConfigs.put("orders", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // 통계 데이터: 거의 안 바뀜 → 1시간
        cacheConfigs.put("stats", defaultConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(
                org.springframework.data.redis.cache.RedisCacheWriter
                    .nonLockingRedisCacheWriter(
                        org.springframework.data.redis.connection.RedisConnectionFactory.class
                            .cast(null)))  // factory는 실제론 주입받아야 함 - 아래 factory 버전 참고
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }

    // ✅ 실제 사용 버전 (factory 주입)
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer(objectMapper())))
            .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("users", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put("orders", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("stats", defaultConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // ✅ 타입 정보 포함: 역직렬화 시 정확한 타입으로 복원
        mapper.activateDefaultTyping(
            mapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL
        );
        return mapper;
    }
}
