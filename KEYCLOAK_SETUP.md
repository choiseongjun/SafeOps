# Keycloak Setup Guide

## 1. Start Services

```bash
docker-compose up -d
```

**중요**: Keycloak이 완전히 시작될 때까지 **30-60초** 기다리세요!

로그 확인:
```bash
docker logs -f safeops-keycloak
```

"Keycloak 23.0.7 on JVM ... started" 메시지가 보일 때까지 기다립니다.

## 2. Access Keycloak Admin Console

- URL: http://localhost:8180
- Username: `admin`
- Password: `admin`

**주의**: 브라우저에서 페이지가 로딩되지 않으면 Keycloak이 아직 시작 중입니다. 1-2분 더 기다려주세요.

## 3. Create Realm

**중요**: Master realm에 로그인한 후 진행하세요!

1. **왼쪽 상단의 드롭다운 메뉴**를 클릭 (현재 "master"로 표시됨)
2. **"Create Realm"** 버튼 클릭
3. **Realm name**: `safeops` 입력
4. **Enabled**: ON (켜져 있는지 확인)
5. **"Create"** 버튼 클릭

**에러 발생 시**:
- 브라우저를 새로고침 (F5)
- Keycloak 재시작: `docker-compose restart keycloak`
- 전체 재시작: `docker-compose down -v && docker-compose up -d`

## 4. Create Client

**중요**: `safeops` realm이 생성되고 선택된 상태에서 진행하세요!

1. 왼쪽 메뉴에서 **"Clients"** 클릭
2. **"Create client"** 버튼 클릭

### Step 1: General Settings
3. 다음 정보 입력:
   - **Client type**: `OpenID Connect`
   - **Client ID**: `safeops-client`
4. **"Next"** 버튼 클릭

### Step 2: Capability config
5. 다음 옵션 활성화:
   - **Client authentication**: `ON` ✓
   - **Authorization**: `OFF`
   - **Authentication flow**:
     - ✓ **Standard flow**: `ON`
     - ✓ **Direct access grants**: `ON` (패스워드 그랜트를 위해 필수!)
     - ✗ **Implicit flow**: `OFF`
     - ✗ **Service accounts roles**: `OFF`
6. **"Next"** 버튼 클릭

### Step 3: Login settings
7. 다음 정보 입력:
   - **Root URL**: (비워둠)
   - **Home URL**: (비워둠)
   - **Valid redirect URIs**: `http://localhost:8080/*`
   - **Valid post logout redirect URIs**: `+` (자동으로 위 URI 사용)
   - **Web origins**: `http://localhost:8080`
8. **"Save"** 버튼 클릭

## 5. Get Client Secret

**중요**: Spring Boot 앱이 Keycloak과 통신하려면 Client Secret이 필요합니다!

1. **"Clients"** > **"safeops-client"** 클릭
2. **"Credentials"** 탭 클릭
3. **"Client secret"** 값을 복사
4. `application.yml` 파일 업데이트:
   ```yaml
   keycloak:
     credentials:
       secret: <복사한-client-secret>
   ```

**예시**:
```yaml
keycloak:
  credentials:
    secret: AbCdEf123456GhIjKl789MnOpQr
```

## 6. 추가 Realm 설정 (선택사항이지만 권장)

### 6.1 토큰 수명 설정
1. `safeops` realm 선택된 상태에서
2. 왼쪽 메뉴 **"Realm settings"** 클릭
3. **"Tokens"** 탭 클릭
4. 다음 값 확인/조정:
   - **Access Token Lifespan**: `5 minutes` (기본값) 또는 테스트를 위해 `30 minutes`
   - **Refresh Token Max**: `30 minutes` 또는 더 길게

### 6.2 사용자 등록 활성화 (선택)
1. **"Realm settings"** > **"Login"** 탭
2. **User registration**: `ON` (사용자가 직접 가입할 수 있게 하려면)
3. **Save**

## 7. Test the Setup

### Sign Up (Create User)

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }'
```

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

### Use Protected Endpoint

```bash
curl -X POST http://localhost:8080/api/payment \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access_token>" \
  -d '{
    "amount": 1000,
    "currency": "USD",
    "userId": "user123"
  }'
```

## Database Access

### PostgreSQL

- Host: `localhost`
- Port: `5432`
- Database: `safeops`
- Username: `safeops`
- Password: `safeops123`

```bash
docker exec -it safeops-postgres psql -U safeops -d safeops
```

### Redis

```bash
docker exec -it safeops-redis redis-cli
```

## Troubleshooting

### 1. "Network response was not OK" 에러 (Realm 생성 시)

**원인**: Keycloak이 아직 완전히 시작되지 않았거나 데이터베이스 연결 문제

**해결 방법**:
```bash
# 1. Keycloak 로그 확인
docker logs safeops-keycloak --tail 100

# 2. "Keycloak 23.0.7 ... started" 메시지 확인

# 3. 없다면 완전히 시작될 때까지 대기 (1-2분)

# 4. 여전히 문제가 있다면 재시작
docker-compose restart keycloak

# 5. 그래도 안 되면 완전 재설정
docker-compose down -v
docker-compose up -d
```

### 2. Keycloak Admin Console 접속 불가

**확인 사항**:
```bash
# Keycloak 컨테이너 상태 확인
docker ps | grep keycloak

# 로그 확인
docker logs -f safeops-keycloak

# PostgreSQL 연결 확인
docker exec safeops-postgres pg_isready -U safeops
```

**해결**: Keycloak이 완전히 시작될 때까지 30-60초 기다립니다.

### 3. PostgreSQL connection issues

```bash
# PostgreSQL 상태 확인
docker exec safeops-postgres pg_isready -U safeops

# 재시작
docker-compose restart postgres
```

### 4. "realm is null" 에러

**원인**: Realm이 제대로 생성되지 않았거나 손상됨

**해결**:
```bash
docker-compose down -v
docker-compose up -d
# 30-60초 대기 후 Realm 다시 생성
```

### 5. Reset everything (모든 데이터 삭제 후 재시작)

```bash
# 모든 컨테이너와 볼륨 삭제
docker-compose down -v

# 다시 시작
docker-compose up -d

# 로그 모니터링
docker logs -f safeops-keycloak
```

**주의**: 이 명령은 모든 데이터(사용자, 설정 등)를 삭제합니다!

Then follow the setup steps again.

## Architecture Overview

- **PostgreSQL**: Stores user data and Keycloak data
- **Keycloak**: Manages authentication and issues JWT tokens
- **Spring Boot App**: Validates JWT tokens from Keycloak
- **Redis**: Handles idempotency for payment operations

## Security Flow

1. User signs up -> Creates user in both Keycloak and PostgreSQL
2. User logs in -> Keycloak validates credentials and returns JWT token
3. User makes API request -> Spring Security validates JWT token
4. Protected endpoints require valid JWT token
