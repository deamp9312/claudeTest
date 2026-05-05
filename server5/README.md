# Server5 - 메시지 프로토콜 최적화 예시

Spring Boot + JPA + Redis + Kafka를 활용한 메시지 프로토콜 최적화 학습 프로젝트

---

## 전체 최적화 흐름

```
[클라이언트]
    │ HTTP 요청
    ▼
[Controller]
    │
    ▼
[Service] ──────────────────────────────────────────────┐
    │                                                    │
    ├─ [JPA] DB 저장                                     │
    │   └─ 배치 INSERT (50개씩 묶음)                     │
    │   └─ LAZY 로딩 (N+1 방지)                         │
    │   └─ DTO Projection (필요한 컬럼만)                │
    │                                                    │
    ├─ [Redis] 캐시                                      │
    │   └─ @Cacheable/@CachePut/@CacheEvict             │
    │   └─ Pipeline (다수 키 한 번에)                    │
    │   └─ Hash (필드별 부분 갱신)                       │
    │                                                    │
    └─ [Kafka] 이벤트 발행 ◀──────────────────────────┘
        └─ LZ4 압축 (30~70% 크기 절감)
        └─ 배치 전송 (linger.ms + batch.size)
        └─ 파티션 키 설계 (userId → 순서 보장)
        └─ 헤더 메타데이터 (페이로드 파싱 전 라우팅)
        └─ 배치 컨슈밍 (100개씩 묶음 처리)
```

---

## 최적화 포인트 상세

### 1. JPA 최적화

| 기법 | 파일 | 효과 |
|------|------|------|
| 배치 INSERT | `application.yml` → `jdbc.batch_size=50` | INSERT 50개를 1번 왕복으로 |
| LAZY 로딩 | `User.java` → `FetchType.LAZY` | 연관 엔티티 불필요 시 미조회 |
| @BatchSize | `User.java` → `@BatchSize(size=30)` | 컬렉션 N+1을 IN 쿼리로 |
| DTO Projection | `UserRepository.java` | SELECT 컬럼 수 감소 |
| FETCH JOIN | `UserRepository.java` | 연관 엔티티 1쿼리로 |
| 벌크 UPDATE | `UserRepository.java` | UPDATE N번 → 1번 |
| readOnly 트랜잭션 | `UserService.java` | 스냅샷 저장 비용 제거 |

### 2. Redis 최적화

| 기법 | 파일 | 효과 |
|------|------|------|
| JSON 직렬화 | `RedisConfig.java` | JDK 직렬화 대비 크기 50% 감소 |
| TTL 차등 설정 | `RedisConfig.java` | users=30분, orders=5분, stats=1시간 |
| Pipeline | `UserService.java` | N번 왕복 → 1번 왕복 |
| Hash 구조 | `UserService.java` | 특정 필드만 부분 갱신/조회 |
| null 캐시 방지 | `RedisConfig.java` | `disableCachingNullValues()` |

### 3. Kafka 최적화

| 기법 | 파일 | 효과 |
|------|------|------|
| LZ4 압축 | `KafkaConfig.java` | 메시지 크기 30~60% 감소 |
| 배치 전송 | `KafkaConfig.java` → `batch.size=16KB` | 처리량 10배 이상 향상 |
| linger.ms=1 | `KafkaConfig.java` | 지연 최소화 |
| 파티션 키 설계 | `OrderEventProducer.java` → `userId` | 같은 유저 이벤트 순서 보장 |
| 헤더 메타데이터 | `OrderEventProducer.java` | 컨슈머 사전 라우팅 |
| 배치 컨슈밍 | `OrderEventConsumer.java` → `batchListener=true` | DB 왕복 감소 |
| 수동 커밋 | `OrderEventConsumer.java` → `MANUAL_IMMEDIATE` | 처리 완료 후만 커밋 |
| 멱등성 | `KafkaConfig.java` → `enable.idempotence=true` | 중복 메시지 방지 |

### 4. 메시지 크기 최적화 (OrderEvent.java)

```json
// ❌ 최적화 전: 긴 필드명 + null 포함
{
  "orderId": 1,
  "userId": 100,
  "productName": "노트북",
  "amount": 1200000,
  "status": "PENDING",
  "timestamp": "2026-05-05T10:30:00",
  "eventType": "ORDER_CREATED",
  "cancelReason": null
}

// ✅ 최적화 후: 짧은 키 + null 제거 + epoch millis
{
  "oi": 1,
  "ui": 100,
  "pn": "노트북",
  "am": 1200000,
  "st": "PENDING",
  "ts": 1746441000000,
  "et": "ORDER_CREATED"
}
// 약 40% 크기 절감
```

---

## 실행 방법

```bash
# 1. Redis, Kafka 실행 (Docker)
docker run -d -p 6379:6379 redis:7
docker run -d -p 9092:9092 apache/kafka:3.7.0

# 2. 빌드 & 실행
cd server5
./gradlew bootRun

# 3. 테스트
# 유저 생성
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","name":"홍길동"}'

# 유저 조회 (첫 번째: DB, 두 번째: Redis 캐시)
curl http://localhost:8080/api/users/1

# 배치 주문 생성 (JPA 배치 INSERT + Kafka 배치 전송)
curl -X POST http://localhost:8080/api/orders/batch \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productNames": ["노트북", "마우스", "키보드"],
    "amounts": ["1200000", "50000", "150000"]
  }'
```

---

## 핵심 트레이드오프

| 최적화 | 장점 | 단점 |
|--------|------|------|
| Redis 캐시 | DB 부하 감소, 응답속도 향상 | 데이터 일관성 주의 (TTL 설정 중요) |
| Kafka 압축 | 네트워크/스토리지 절감 | CPU 사용량 증가 |
| 배치 처리 | 처리량 대폭 향상 | 지연시간 증가 |
| 수동 Kafka 커밋 | at-least-once 보장 | 중복 처리 대비 멱등성 구현 필요 |
| DTO Projection | 쿼리 성능 향상 | 엔티티 변경 시 DTO도 같이 수정 |
