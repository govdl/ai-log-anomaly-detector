# AI 기반 서버 로그 이상 행위 탐지 및 분석 시스템

서버 로그를 수집하고, **규칙 기반 탐지**와 **머신러닝(Isolation Forest)** 으로 이상 행위를 식별한 뒤, **LLM**이 탐지 결과를 사람이 이해하기 쉽게 설명하는 프로젝트입니다.

> ⚠️ 교육·포트폴리오 목적의 제한된 탐지 시스템입니다. 탐지 결과는 공격 확정이 아니라 사전에 정의한 조건 또는 모델 기준에 따른 **의심 징후**입니다.

## 📌 진행 상황

> 최종 업데이트: 2026-11-12

| 주차 | 목표 | 상태 |
|---|---|---|
| 1주 차 | 기반 구축 (데이터 형식, DB, 로그 수집 API) | 🔄 진행 중 |
| 2주 차 | 규칙 기반 탐지, Isolation Forest 1차 실험 | ⏳ 예정 |
| 3주 차 | AI 서버 연동, LLM 설명, 통합 테스트 | ⏳ 예정 |
| 4주 차 | Docker, AWS 배포, CI/CD, 문서 정리 | ⏳ 예정 |

### 기능 체크리스트
- [x] 프로젝트 기획 및 범위 확정
- [ ] 로그 수집 API
- [ ] Brute Force 의심 행위 탐지
- [ ] 비정상 API 요청 탐지
- [ ] SQL Injection 의심 요청 탐지
- [ ] Isolation Forest 이상 탐지
- [ ] LLM 기반 탐지 결과 설명
- [ ] 보안 이벤트 저장 및 조회
- [ ] Swagger API 문서
- [ ] Docker 실행 환경
- [ ] AWS EC2 배포
- [ ] GitHub Actions CI/CD

## 🎯 프로젝트 목표

다음 세 가지 이상 행위를 탐지하고 분석합니다.

1. **Brute Force 의심**: 동일 IP에서 짧은 시간 동안 로그인 실패 반복
2. **비정상 API 요청**: 동일 IP에서 짧은 시간 동안 과도한 요청
3. **SQL Injection 의심**: 사전 정의한 패턴이 입력값에 포함

명확한 조건으로 판별 가능한 행위는 **규칙 기반**으로 탐지하고, 머신러닝은 정상 패턴에서 벗어난 로그를 **보조적으로** 식별합니다. LLM은 최종 판정을 내리지 않고 **설명과 권장 확인 사항**만 제공합니다.

### 범위

| 포함 | 제외 |
|---|---|
| 로그 수집 및 저장 | 실시간 네트워크 패킷 분석 |
| 3가지 시나리오 규칙 탐지 | Kafka, Redis 기반 대규모 분산 처리 |
| Isolation Forest 이상 탐지 실험 | 자동 공격 차단, 계정 잠금 |
| LLM 기반 결과 설명 | 다양한 공격 유형의 포괄적 탐지 |
| 이벤트 조회 API, Swagger | 자체 LLM 학습·파인튜닝 |
| Docker, AWS EC2, GitHub Actions | 상용 보안 관제 수준의 실시간 대응 |

## 🔍 탐지 시나리오

아래 임계값은 프로젝트 테스트용 초기 기준이며, 실제 서비스의 보편적인 공격 판정 기준이 아닙니다. 테스트 결과에 따라 조정할 수 있습니다.

| 시나리오 | 이벤트 타입 | 초기 탐지 기준 |
|---|---|---|
| Brute Force 의심 | `BRUTE_FORCE_SUSPECTED` | 동일 IP에서 1분 동안 로그인 실패 10회 이상 |
| 비정상 API 요청 | `ABNORMAL_REQUEST_RATE` | 동일 IP에서 10초 동안 요청 100회 이상 |
| SQL Injection 의심 | (작성 예정) | 입력값이 사전 정의한 의심 패턴과 일치 |

## 🏗️ 시스템 아키텍처

```text
[테스트 로그 생성기 / CLI]
          |
          v
[Spring Boot Backend]
  - 로그 수집 API / 데이터 검증
  - 규칙 기반 탐지 / 탐지 결과 통합
  - 이벤트 조회 API
          |
          +----------------------+
          |                      |
          v                      v
 [PostgreSQL]             [Python AI Server]
  - 원본 로그               - 전처리 / 특징 추출
  - 탐지 이벤트             - Isolation Forest
  - 분석 결과               - 이상 점수 반환
                                  |
                                  v
                            [LLM API]
                            - 탐지 결과 설명
                            - 근거 요약 / 권장 대응 안내
```

## 🛠️ 기술 스택

| 구분 | 기술 |
|---|---|
| Backend | Java, Spring Boot |
| AI Server | Python, FastAPI |
| ML / 데이터 | Scikit-learn (Isolation Forest), Pandas, NumPy |
| Database | PostgreSQL |
| LLM | 외부 LLM API |
| 배포 | Docker, Docker Compose, AWS EC2 |
| CI/CD | GitHub Actions |
| API 문서 | Swagger / OpenAPI |

## 📡 API (초안)

> 요청·응답 필드와 상태 코드는 구현 전 확정 예정입니다.

| Method | Endpoint | 설명 |
|---|---|---|
| POST | `/api/logs` | 단일 로그 수집 |
| POST | `/api/logs/batch` | 로그 여러 건 수집 |
| GET | `/api/logs` | 로그 목록 조회 |
| GET | `/api/events` | 탐지 이벤트 목록 조회 |
| GET | `/api/events/{eventId}` | 탐지 이벤트 상세 조회 |
| POST | `/api/events/{eventId}/analysis` | 탐지 결과 LLM 설명 요청 |
| GET | `/api/health` | 서비스 상태 확인 |

AI 서버 내부 API(`/internal/anomaly/detect`, `/internal/health`)는 외부에 공개하지 않고 백엔드를 통해서만 호출합니다.

## 🗄️ 데이터

| 데이터 | 용도 |
|---|---|
| [LogHub](https://github.com/logpai/loghub) | 실제 시스템 로그 형식 학습, 전처리 실험 |
| 자체 생성 로그 | 3가지 시나리오 통합 테스트 (생성 규칙과 라벨 함께 기록) |
| [CIC-IDS2017](https://www.unb.ca/cic/datasets/ids-2017.html) 등 (선택) | 네트워크 침입 탐지 참고 실험 (애플리케이션 로그와 구분) |

**데이터 사용 원칙**
- 형식이 다른 데이터셋을 무작정 합치지 않습니다.
- 학습 데이터와 평가 데이터를 분리합니다.
- 자체 생성 데이터의 점수를 실제 환경의 일반화 성능으로 주장하지 않습니다.
- 실제 공격은 수행하지 않으며, 테스트용 IP·계정만 사용합니다.

## 📊 실험 결과

> 아직 측정 전입니다. **실제 측정한 수치만** 기록합니다.

| 방식 | Precision | Recall | F1 | FPR |
|---|---|---|---|---|
| 규칙 기반만 | - | - | - | - |
| ML(Isolation Forest)만 | - | - | - | - |
| 규칙 + ML | - | - | - | - |

## 🚀 실행 방법

> 구현 후 작성 예정

```bash
# 예정
# 1) 환경 변수 설정
cp .env.example .env

# 2) 실행
docker compose up -d
```

## 📁 디렉터리 구조 (예정)

```text
.
├── README.md
├── docs/
│   ├── api-spec.md
│   ├── erd.md
│   └── experiments.md
├── backend/        # Spring Boot
├── ai-server/      # FastAPI
├── docker-compose.yml
└── .env.example
```

## 🔐 보안 유의사항

- 비밀번호, 토큰, 쿠키, 개인정보를 로그에 저장하지 않습니다.
- LLM 입력에 민감정보와 원본 요청 본문을 포함하지 않습니다.
- API 키, DB 비밀번호는 Git에 올리지 않고 환경 변수로 관리합니다.
- 자동 차단 대신 탐지 이벤트를 생성하고 사람이 확인합니다.

## 🌿 Git 협업 규칙

- 브랜치: `main`(안정) / `develop`(통합) / `feature/*` / `fix/*`
- 통합은 Pull Request로 진행하고, 통합 전 빌드·테스트를 실행합니다.
- 커밋 메시지 예시: `feat:`, `fix:`, `test:`, `docs:`

## 👥 팀

| 역할 | 담당 | 이름 |
|---|---|---|
| 데이터 분석 / ML / AI 서버 / LLM | AI 전공 | 이윤지 |
| 백엔드 / 서버 운영 / 배포 | 컴퓨터공학 전공 | 한윤슬 |

## 📚 참고 자료

- [LogHub](https://github.com/logpai/loghub)
- [CIC-IDS2017](https://www.unb.ca/cic/datasets/ids-2017.html)
- [UNSW-NB15](https://research.unsw.edu.au/projects/unsw-nb15-dataset)
- [Scikit-learn Isolation Forest](https://scikit-learn.org/stable/modules/generated/sklearn.ensemble.IsolationForest.html)
- [Spring Boot](https://docs.spring.io/spring-boot/index.html) / [FastAPI](https://fastapi.tiangolo.com/)
- [Docker Compose](https://docs.docker.com/compose/) / [GitHub Actions](https://docs.github.com/actions)

## ⚖️ 유의사항

본 프로젝트는 제한된 테스트 데이터와 사전 정의된 규칙을 사용하는 학습·포트폴리오용 시스템이며, 실제 보안 관제 제품을 대체하지 않습니다. 실제 환경에 적용하려면 별도의 보안 검증과 임계값 조정이 필요합니다.