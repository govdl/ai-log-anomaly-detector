# Data Schema Definition

> **Status: Draft**
>
> 백엔드와 AI 서버가 공통으로 사용할 로그 및 탐지 이벤트 데이터 규격을 정의한다.  
> 본 문서는 초기 개발을 위한 초안이며, 백엔드 및 AI 담당자의 검토 후 확정한다.

## 0. 공통 규칙

| 항목 | 규칙 |
|---|---|
| JSON 필드명 | camelCase |
| DB 컬럼명 | snake_case (예: `sourceIp` → `source_ip`) |
| 시각 | ISO-8601, 타임존 포함, KST(+09:00) 기준 |
| 시각 예시 | `2026-11-13T17:15:30+09:00` |
| enum 값 | 대문자 + 언더스코어 (예: `BRUTE_FORCE_SUSPECTED`) |
| ID | 로그 및 이벤트 ID 모두 DB 자동 증가 `long` |
| 선택 필드 | 값이 없으면 필드 생략 또는 `null` 허용 |
| 금지 항목 | 비밀번호, 토큰, 쿠키 등 민감정보 및 개인정보 원문 저장 금지 |
| username | HMAC-SHA256으로 해시한 값만 저장 |

---

## 1. 로그 필드 (Log)

수집 API(`POST /api/logs`)의 요청 본문 및 AI 전처리·특징 추출의 입력 기준이다.

### 로그 유형

- `AUTH`: 로그인 시도 로그 (Brute Force 탐지용)
- `ACCESS`: API 요청 로그 (비정상 요청 및 SQL Injection 탐지용)

### 로그 필드 정의

| 필드명 | 타입 | 필수 여부 | 설명 | 예시 값 |
|---|---|---|---|---|
| logId | long | 서버 생성 | 로그 고유 ID. 수집 요청에는 포함하지 않으며 서버가 부여 | 1001 |
| occurredAt | String (ISO-8601) | Y | 요청이 발생한 시각 | 2026-11-12T10:15:50+09:00 |
| createdAt | String (ISO-8601) | 서버 생성 | DB 저장 시각. 수집 요청에는 포함하지 않음 | 2026-11-12T10:15:50+09:00 |
| sourceIp | String | Y | 출발지 식별자. 실제 IP 대신 가상 ID 또는 문서용 IP 사용 | 192.0.2.10 |
| logType | enum | Y | 로그 유형: AUTH / ACCESS | AUTH |
| path | String | Y | 요청 경로 (Query String 제외) | /api/auth/login |
| queryString | String | N | SQL Injection 검사 대상 입력값. 최대 500자, 민감정보 제외 | id=1' OR '1'='1 |
| httpMethod | enum | Y | HTTP 메서드: GET, POST, PUT, PATCH, DELETE | POST |
| statusCode | Integer | Y | HTTP 응답 상태 코드 (100~599) | 401 |
| responseTimeMs | Integer | Y | 응답 처리 시간(ms), 0 이상 | 120 |
| eventType | enum | Y | 로그 이벤트 유형 (2번 참고) | LOGIN_FAILURE |
| username | String | N | 로그인 시도 계정의 HMAC-SHA256 해시값. 원문 저장 금지 | hmac_sha256_hash_value |
| userAgent | String | N | User-Agent 문자열. 최대 255자 | python-requests/2.31 |
| serviceName | String | N | 로그를 발생시킨 서비스 이름 | sample-web |
| requestSummary | String | N | 민감정보 제거 및 마스킹 처리된 요청 요약 | username field present; sensitive values masked |

**필수 여부 구분**

- Y: 수집 API 요청에 반드시 포함
- N: 선택적으로 포함 가능
- 서버 생성: 수집 요청에는 포함하지 않으며 서버에서 생성

### queryString 처리 원칙

- 원본 요청 본문은 저장하지 않는다.
- 탐지 대상 Query String만 저장한다.
- 다음 민감 키의 값은 `***`로 마스킹한다.
  - `password`, `passwd`, `pwd`, `token`, `secret`, `authorization`, `cookie`
- 마스킹 후 500자를 초과하면 잘라서 저장한다.

### 로그 JSON 예시

**1. AUTH 로그**

```json
{
  "occurredAt": "2026-11-13T17:15:30+09:00",
  "sourceIp": "192.0.2.10",
  "logType": "AUTH",
  "path": "/api/auth/login",
  "httpMethod": "POST",
  "statusCode": 401,
  "responseTimeMs": 85,
  "eventType": "LOGIN_FAILURE",
  "username": "hmac_sha256_hash_value",
  "userAgent": "python-requests/2.31",
  "serviceName": "sample-web",
  "requestSummary": "username field present; sensitive values masked"
}
```

**2. ACCESS 로그**

```json
{
  "occurredAt": "2026-11-13T17:16:02+09:00",
  "sourceIp": "192.0.2.20",
  "logType": "ACCESS",
  "path": "/api/products",
  "queryString": "id=1' OR '1'='1",
  "httpMethod": "GET",
  "statusCode": 500,
  "responseTimeMs": 310,
  "eventType": "API_REQUEST",
  "serviceName": "sample-web"
}
```

### 테스트 데이터 전용 라벨

- 정답 라벨(`label`)은 운영 스키마와 수집 API에 포함하지 않는다.
- 평가용 테스트 데이터는 별도 파일로 분리하며, 해당 파일에서만 `label`을 사용한다.
- `label`의 허용 값:
  - `NORMAL`
  - `BRUTE_FORCE`
  - `ABNORMAL_REQUEST`
  - `SQL_INJECTION`
- 탐지 로직에서는 `label`을 사용하지 않으며 모델 및 탐지 결과 평가에만 사용한다.

---

## 2. 로그 eventType 목록 (입력 로그용)

로그 자체의 유형을 구분하는 enum이다.

| 값 | 설명 |
|---|---|
| LOGIN_SUCCESS | 로그인 성공 |
| LOGIN_FAILURE | 로그인 실패 (Brute Force 집계 대상) |
| API_REQUEST | 일반 API 요청 (요청량 집계 대상) |
| OTHER | 위 유형에 해당하지 않는 요청 |

---

## 3. 탐지 이벤트 필드 (Event)

AI 서버 또는 탐지 로직에서 생성된 탐지 결과를 의미한다.

조회 API:
- `GET /api/events`
- `GET /api/events/{eventId}`

### 이벤트 필드 정의

| 필드명 | 타입 | 필수 여부 | 설명 | 예시 값 |
|---|---|---|---|---|
| id | long | 서버 생성 | 이벤트 고유 ID (DB 자동 증가) | 501 |
| eventType | enum | Y | 탐지 유형 (4번 참고) | BRUTE_FORCE_SUSPECTED |
| sourceIp | String | Y | 탐지 대상 출발지 IP 또는 식별자 | 192.0.2.10 |
| severity | enum | Y | 위험도: LOW, MEDIUM, HIGH (eventType별 고정) | MEDIUM |
| detectionMethod | enum | Y | 탐지 방식: RULE, ML, RULE_AND_ML | RULE |
| reason | String | Y | 탐지 근거 설명 | 동일 IP에서 60초 동안 로그인 실패 12회 발생 |
| ruleId | String | N | 적용된 규칙 ID (RULE 계열일 때) | R-BF-001 |
| ruleName | String | N | 적용된 규칙 이름 (RULE 계열일 때) | LOGIN_FAIL_10_PER_MIN |
| threshold | Integer | N | 적용된 탐지 임계값 | 10 |
| observedCount | Integer | N | 시간 창 내 관찰된 횟수 | 12 |
| anomalyScore | number | N | ML 이상 점수 (0~1, 클수록 이상) | 0.82 |
| modelName | String | N | ML 모델 식별자 | Isolation Forest |
| windowStart | String (ISO-8601) | N | 집계 시간 창 시작 | 2026-11-13T17:15:00+09:00 |
| windowEnd | String (ISO-8601) | N | 집계 시간 창 종료 | 2026-11-13T17:16:00+09:00 |
| relatedLogIds | Array of long | N | 탐지 근거가 된 로그 ID 목록 | [1001, 1002, 1003] |
| status | enum | Y | 처리 상태. 기본값 NEW | NEW |
| explanation | String | N | LLM이 생성한 상세 설명. 분석 요청 전에는 null | 동일 IP에서 짧은 시간 동안 로그인 실패가 반복됨 |
| explainedAt | String (ISO-8601) | N | LLM 설명 생성 시각 | 2026-11-13T17:16:10+09:00 |
| occurredAt | String (ISO-8601) | Y | 탐지 대상 행위가 발생한 시작 시각 | 2026-11-13T17:15:31+09:00 |
| createdAt | String (ISO-8601) | 서버 생성 | 이벤트 생성 시각 | 2026-11-13T17:16:01+09:00 |

**탐지 방식별 필드 관리**

규칙 기반 탐지 결과와 ML 탐지 결과는 하나의 점수로 혼합하지 않고 출처별 필드로 구분한다.

- RULE: `ruleId`, `ruleName`, `threshold`, `observedCount` 활용
- ML: `anomalyScore`, `modelName` 활용
- RULE_AND_ML: 각 탐지 방식에 해당하는 필드를 함께 활용

해당하지 않는 필드는 생략하거나 `null`로 처리한다.

### 이벤트 JSON 예시

```json
{
  "id": 501,
  "eventType": "BRUTE_FORCE_SUSPECTED",
  "sourceIp": "192.0.2.10",
  "severity": "MEDIUM",
  "detectionMethod": "RULE",
  "reason": "동일 IP에서 60초 동안 로그인 실패 12회 발생",
  "ruleId": "R-BF-001",
  "ruleName": "LOGIN_FAIL_10_PER_MIN",
  "threshold": 10,
  "observedCount": 12,
  "anomalyScore": null,
  "modelName": null,
  "windowStart": "2026-11-13T17:15:00+09:00",
  "windowEnd": "2026-11-13T17:16:00+09:00",
  "relatedLogIds": [1001, 1002, 1003],
  "status": "NEW",
  "explanation": null,
  "explainedAt": null,
  "occurredAt": "2026-11-13T17:15:45+09:00",
  "createdAt": "2026-11-13T17:16:01+09:00"
}
```

### 이벤트 상태 변경 API

- API: `PATCH /api/events/{eventId}/status`
- 요청 본문:

```json
{
  "status": "CONFIRMED"
}
```

- 허용 값:
  - `NEW`: 신규 탐지 이벤트
  - `CONFIRMED`: 확인된 이벤트
  - `FALSE_POSITIVE`: 오탐으로 판단된 이벤트

---

## 4. 탐지 이벤트 eventType 목록

로그 eventType과 별도로, 실제 탐지 결과의 유형을 구분하는 enum이다.

| 값 | 설명 | 주 탐지 방식 | 관련 규칙 ID | 초기 탐지 기준 (테스트용) |
|---|---|---|---|---|
| BRUTE_FORCE_SUSPECTED | 짧은 시간 내 로그인 실패 반복 | RULE | R-BF-001 | 동일 IP에서 1분간 LOGIN_FAILURE 10회 이상 (AUTH 로그) |
| API_REQUEST_SPIKE | 짧은 시간 내 과도한 API 요청 | RULE + ML | R-API-001 | 동일 IP에서 10초간 요청 100회 이상 (ACCESS 로그) |
| SQL_INJECTION_SUSPECTED | 입력값에서 SQL Injection 의심 패턴 확인 | RULE (패턴) | R-SQLI-001 | 입력값이 사전 정의한 의심 패턴과 일치 |
| ANOMALY_SUSPECTED | 특정 공격 유형으로 분류되지 않지만 ML이 행동 이상으로 판단 | ML | - | Isolation Forest 이상 점수가 기준 이상 |

**주의 사항**

- `*_SUSPECTED`는 의심 상태를 의미하며 공격이 확정되었거나 성공했다는 의미가 아니다.
- `ANOMALY_SUSPECTED`는 특정 공격 유형을 의미하지 않는다.
- 초기 임계값은 프로젝트 테스트용 기준이며 실제 서비스의 공격 판정 기준이 아니다.
- 임계값은 테스트 결과와 모델 평가 결과에 따라 조정한다.

### 초기 severity 정책 (협의용 제안)

| eventType | 초기 severity 제안 |
|---|---|
| BRUTE_FORCE_SUSPECTED | MEDIUM |
| API_REQUEST_SPIKE | MEDIUM |
| SQL_INJECTION_SUSPECTED | HIGH |
| ANOMALY_SUSPECTED | MEDIUM |

위 값은 초기 제안이며, 백엔드 및 AI 담당자 협의 후 확정한다.

---

## 5. 협의 결과 및 설계 결정 사항

현재까지 논의된 데이터 규격과 설계 방향을 정리한다.

| 항목 | 결정 |
|---|---|
| sourceIp | 모델 입력 특징에서는 제외하고 집계 그룹 키로만 사용 |
| eventType | 단독으로 사용하지 않고 다른 로그 필드와 조합해 특징 추출 |
| ANOMALY_SUSPECTED | ML 단독 탐지 결과를 저장하기 위해 추가 |
| anomalyScore | 0~1 범위, 클수록 이상 |
| 시간 창 | 규칙 기반은 10초 / 60초 고정, 슬라이딩 윈도우 사용. 기준 시각은 로그의 occurredAt |
| queryString | 탐지 대상 Query String만 저장. 민감 키 값 마스킹 및 길이 제한 적용 |
| 원본 요청 본문 | 저장하지 않음 |
| username | HMAC-SHA256으로 해시한 값만 저장 |
| HMAC 비밀 키 | 환경 변수로 관리하고 Git 저장소에 커밋하지 않음 |
| severity | eventType별 고정 위험도 적용 (초기 제안은 4번 표 참고) |
| status | NEW / CONFIRMED / FALSE_POSITIVE |
| 상태 변경 API | PATCH /api/events/{eventId}/status 사용 |
| id | 로그 및 이벤트 모두 DB 자동 증가 long |
| label | 운영 스키마에서 제외하고 테스트 데이터 전용 파일에서만 사용 |

### 추가 협의가 필요한 항목

- [ ] eventType별 severity 최종 확정
- [ ] API_REQUEST_SPIKE의 RULE 및 ML 탐지 조건 구분
- [ ] ML 모델의 anomalyScore 탐지 임계값 확정
- [ ] username 해시 처리 주체 및 HMAC 키 공유 방식 확정
- [ ] sourceIp의 실제 수집·저장 방식 및 가상 ID 적용 범위 확인
- [ ] `sourceIp`가 없는 이벤트의 처리 방식 확인

---

**문서 변경 이력**

| 버전 | 내용 |
|---|---|
| v0.1 | 공통 로그 및 탐지 이벤트 데이터 규격 초안 작성 |