# Idempotency Pattern 구현

이 프로젝트는 Spring Boot에서 Idempotency 패턴을 구현한 예제입니다.

## 주요 기능

### 1. Idempotency 패턴이란?
- 동일한 요청을 여러 번 보내도 한 번만 처리되도록 보장하는 패턴
- 네트워크 오류, 재시도 로직 등으로 인한 중복 요청 방지
- 결제, 주문 생성 등 중요한 트랜잭션에 필수적

### 2. 구현 방식
- **AOP 기반**: `@Idempotent` 어노테이션을 메소드에 추가만 하면 자동 적용
- **두 가지 저장소 지원**:
  - In-Memory (기본값): 개발/테스트용, 단일 서버 환경
  - Redis: 프로덕션용, 분산 서버 환경

### 3. 프로젝트 구조

```
src/main/java/com/example/distributetest/
├── idempotency/
│   ├── annotation/
│   │   └── Idempotent.java              # Idempotency 어노테이션
│   ├── aspect/
│   │   └── IdempotencyAspect.java       # AOP 구현
│   ├── config/
│   │   └── IdempotencyConfig.java       # 설정 클래스
│   ├── model/
│   │   └── IdempotencyKey.java          # Idempotency 키 모델
│   └── service/
│       ├── IdempotencyService.java      # 서비스 인터페이스
│       ├── InMemoryIdempotencyService.java  # 메모리 구현
│       └── RedisIdempotencyService.java     # Redis 구현
└── controller/
    └── PaymentController.java           # 예제 컨트롤러
```

## 사용 방법

### 1. 기본 사용법

컨트롤러 메소드에 `@Idempotent` 어노테이션 추가:

```java
@PostMapping
@Idempotent(ttl = 60)  // 60분 동안 캐시 유지
public ResponseEntity<PaymentResponse> createPayment(@RequestBody PaymentRequest request) {
    // 결제 로직
}
```

### 2. HTTP 요청 예제

**첫 번째 요청:**
```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: payment-123456" \
  -d '{
    "amount": 10000,
    "currency": "KRW",
    "customerId": "customer-001"
  }'
```

**동일한 키로 재요청 (캐시된 응답 반환):**
```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: payment-123456" \
  -d '{
    "amount": 10000,
    "currency": "KRW",
    "customerId": "customer-001"
  }'
```

### 3. 설정 변경

#### In-Memory 사용 (기본값)
`application.yml`:
```yaml
idempotency:
  storage: memory
```

#### Redis 사용
`application.yml`:
```yaml
idempotency:
  storage: redis

spring:
  data:
    redis:
      host: localhost
      port: 6379
```

## 어노테이션 파라미터

```java
@Idempotent(
    ttl = 60,                    // 캐시 만료 시간 (분 단위, 기본값: 60)
    keyExpression = ""           // 커스텀 키 표현식 (선택사항)
)
```

## 동작 원리

1. **요청 수신**: 클라이언트가 `Idempotency-Key` 헤더와 함께 요청
2. **키 확인**: AOP Aspect가 요청을 가로채서 키 존재 여부 확인
3. **캐시 확인**:
   - 키가 존재하면: 캐시된 응답 즉시 반환
   - 키가 없으면: 실제 로직 실행
4. **응답 캐싱**: 실행 결과를 TTL 기간 동안 저장
5. **응답 반환**: 클라이언트에게 응답 전송

## 테스트 방법

### 1. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 2. 결제 API 테스트
```bash
# 첫 번째 요청 (새로운 거래 생성)
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: unique-key-001" \
  -d '{"amount": 50000, "currency": "KRW", "customerId": "cust-123"}'

# 동일한 키로 재요청 (동일한 transactionId 반환)
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: unique-key-001" \
  -d '{"amount": 50000, "currency": "KRW", "customerId": "cust-123"}'
```

### 3. 환불 API 테스트
```bash
curl -X POST http://localhost:8080/api/payments/refund \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: refund-key-001" \
  -d '{"transactionId": "txn-123", "amount": 30000, "reason": "Customer request"}'
```

## 주의사항

### 1. Idempotency Key 생성
- UUID 사용 권장: `UUID.randomUUID().toString()`
- 클라이언트 측에서 생성하여 재시도 시에도 동일한 키 사용
- 키는 충분히 고유해야 함

### 2. 분산 환경
- 여러 서버 인스턴스가 있는 경우 반드시 Redis 사용
- In-Memory는 단일 서버 인스턴스에서만 동작

### 3. TTL 설정
- 너무 짧으면: 재시도 시간 내에 만료될 수 있음
- 너무 길면: 메모리/스토리지 낭비
- 일반적으로 1-60분 권장

### 4. HTTP 메소드
- POST, PUT, PATCH 등 변경 작업에만 사용
- GET, DELETE는 일반적으로 불필요

## 실제 프로덕션 적용 시 고려사항

1. **Redis 클러스터 구성**: 고가용성을 위한 Redis Sentinel/Cluster
2. **키 네이밍 전략**: 체계적인 키 생성 규칙 (예: `{service}:{operation}:{uuid}`)
3. **모니터링**: 캐시 히트율, TTL 만료율 모니터링
4. **에러 처리**: 저장소 장애 시 fallback 전략
5. **보안**: Idempotency Key 예측 불가능하도록 설계

## 확장 가능성

이 구현은 다음과 같이 확장 가능합니다:

1. **데이터베이스 기반 저장소**: PostgreSQL, MongoDB 등
2. **조건부 Idempotency**: 특정 조건에서만 활성화
3. **부분 응답 캐싱**: 응답의 일부만 캐싱
4. **멀티 키 지원**: 여러 헤더 조합으로 키 생성
5. **통계/메트릭**: Micrometer 통합으로 성능 측정
