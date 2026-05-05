package com.example.server5.service;

import com.example.server5.domain.entity.User;
import com.example.server5.domain.repository.UserRepository;
import com.example.server5.dto.UserSummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // ✅ JPA 최적화: 읽기 전용 트랜잭션 (스냅샷 저장 안 함)
public class UserService {

    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * ✅ Redis 최적화 1: @Cacheable
     * 첫 호출 → DB 조회 후 Redis 저장
     * 이후 호출 → Redis에서 바로 반환 (DB 접근 없음)
     *
     * key: "users::1", "users::2" ... (SpEL 표현식)
     */
    @Cacheable(value = "users", key = "#id", unless = "#result == null")
    public User findById(Long id) {
        log.info("DB 조회: userId={}", id);  // 캐시 히트 시 이 로그 안 찍힘
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    /**
     * ✅ Redis 최적화 2: DTO Projection + 캐싱
     * 엔티티 전체 대신 필요한 필드만 → 직렬화 크기 감소 → Redis 메모리 절약
     */
    @Cacheable(value = "users", key = "'all-summary'")
    public List<UserSummaryDto> findAllSummary() {
        return userRepository.findAllSummary();
    }

    @Transactional
    @CachePut(value = "users", key = "#result.id")  // ✅ 저장 후 캐시 갱신
    public User createUser(String email, String name) {
        User user = User.create(email, name);
        return userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")  // ✅ 수정 후 캐시 무효화
    public void updateUserName(Long id, String name) {
        userRepository.bulkUpdateName(id, name);
    }

    /**
     * ✅ Redis 최적화 3: Pipeline (파이프라이닝)
     * 여러 Redis 명령을 한 번의 네트워크 왕복으로 처리
     * 개별 호출 대비 latency 대폭 감소
     */
    public void cacheMultipleUsers(List<User> users) {
        redisTemplate.executePipelined((connection) -> {
            users.forEach(user -> {
                String key = "users::" + user.getId();
                redisTemplate.opsForValue().set(key, user, 30, TimeUnit.MINUTES);
            });
            return null;
        });
        log.info("Pipeline으로 {}명 캐시 완료", users.size());
    }

    /**
     * ✅ Redis 최적화 4: Hash 구조 활용
     * 객체 전체 대신 특정 필드만 업데이트 가능 (역직렬화 없이)
     */
    public void cacheUserWithHash(User user) {
        String hashKey = "user:hash:" + user.getId();
        redisTemplate.opsForHash().put(hashKey, "name", user.getName());
        redisTemplate.opsForHash().put(hashKey, "email", user.getEmail());
        redisTemplate.expire(hashKey, 30, TimeUnit.MINUTES);
    }

    public String getUserNameFromHash(Long userId) {
        String hashKey = "user:hash:" + userId;
        // ✅ 전체 객체 역직렬화 없이 name 필드만 가져옴
        return (String) redisTemplate.opsForHash().get(hashKey, "name");
    }
}
