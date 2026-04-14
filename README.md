# IT-da

> 프로젝트 의뢰인과 IT 프리랜서를 연결하는 AI 기반 매칭 플랫폼

## 프로젝트 소개

IT-da는 클라이언트의 프로젝트 요구사항을 구조화하고, 적합한 IT 프리랜서를 더 빠르게 찾을 수 있도록 돕는 매칭 플랫폼입니다.
자유 텍스트 기반 프로젝트 입력을 AI 브리프로 정리하고, 스킬/경력 기반 추천 결과를 설명 가능하게 제공하는 것을 목표로 합니다.

현재 저장소는 MVP 백엔드/웹 애플리케이션의 기반을 정리하는 단계입니다.
홈/로그인/회원가입 진입점과 회원/이력서 중심 도메인, 파일 저장 패턴, 테스트/CI 구성이 먼저 잡혀 있으며, 제안서(`Proposal`), 매칭(`Matching`), AI 브리프, 추천 엔진은 `docs/` 문서를 기준으로 설계·확장 중입니다.

## 핵심 가치

- 자유 입력 기반 프로젝트 설명을 구조화된 제안서 초안으로 전환
- 스킬, 경력, 선호 조건을 바탕으로 한 설명 가능한 추천
- 제안서 작성부터 매칭, 진행, 완료까지 이어지는 일관된 도메인 흐름

## 목표 서비스 흐름

```text
프리랜서 프로필 등록 -> 클라이언트 프로젝트 입력 -> AI 브리프 생성
-> 제안서 검토 및 모집 시작 -> 추천/지원 -> 매칭 성사
-> 계약/진행 -> 완료 증빙 및 리뷰
```

## 현재 구현 상태

아래 표는 "서비스 전체 완성도"가 아니라 현재 리포지토리에서 확인되는 코드 기준으로 정리한 상태입니다.

| 영역 | 상태 | 설명 |
| --- | --- | --- |
| 인증/회원 | 기초 구현 | Spring Security 폼 로그인, 회원가입 처리, 이메일 중복 검증, BCrypt 비밀번호 암호화 |
| 프리랜서 프로필 | 도메인 기반 정리 | `Member`, `Resume`, `Skill`, `ProfileImage`, `StoredFile` 중심 엔티티와 일부 서비스/테스트 구성 |
| 파일 업로드 | 저장 패턴 구성 | 로컬 파일 저장 경로와 메타데이터 저장 패턴이 정리되어 있음 |
| 화면 | 최소 화면 구성 | 홈(`/`), 로그인(`/login`), 회원가입(`/signup`) 템플릿과 진입 컨트롤러만 구성 |
| 제안서/매칭/추천 | 설계 및 확장 예정 | `docs/project-overview.md`, `docs/domain-spec.md`, `docs/implementation-backlog.md` 기준으로 정리 중 |
| 테스트/품질 | 기반 구성 | 도메인/서비스/컨트롤러/리포지토리 테스트와 GitHub Actions CI 설정 포함 |

## 기술 스택

| 구분        | 기술                                                |
|-----------|---------------------------------------------------|
| Language  | Java 17                                           |
| Framework | Spring Boot 3.5.13                                |
| Web       | Spring MVC, Thymeleaf, Validation                 |
| Security  | Spring Security, BCrypt, Session-based Form Login |
| Data      | Spring Data JPA, QueryDSL, PostgreSQL, H2         |
| Infra     | Docker Compose, GitHub Actions                    |
| Test      | JUnit 5, Spring Boot Test, Testcontainers         |

## 주요 문서

- [유저 플로우 (FigJam)](https://www.figma.com/board/cWaXvSsJ430Zc04jn94YyM/IT-da-%EC%9C%A0%EC%A0%80-%ED%94%8C%EB%A1%9C%EC%9A%B0?node-id=0-1&t=HFlWFnYZp19BCIS1-1)
- [디자인 문서 (Figma)](https://www.figma.com/design/zpmqsi84xBvHAmdLABBwfZ/IT-da-%EB%94%94%EC%9E%90%EC%9D%B8?node-id=0-1&t=xHEfASK7hbFbtYnZ-1)
- [프로젝트 개요](docs/project-overview.md)
- [결정 로그](docs/decision-log.md)
- [도메인 상세 명세](docs/domain-spec.md)
- [구현 백로그](docs/implementation-backlog.md)
- [Figma Make 와이어프레임 프롬프트](docs/figma-make-wireframes.md)

## 로컬 실행 방법

### 사전 요구사항

- Java 17+
- Docker / Docker Compose

### 1. 환경 변수 준비

현재 저장소는 두 가지 설정 소스를 사용합니다.

- `compose.yaml`은 루트의 `.env`를 사용합니다.
- Spring Boot 애플리케이션은 루트의 `.env.properties` 또는 OS 환경 변수를 사용합니다.

프로젝트 루트에 `.env.properties` 파일을 생성하고 아래 키를 채워주세요.

```properties
DRIVER_CLASS_NAME=org.postgresql.Driver
POSTGRES_DB=itda
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_PORT=5432
POSTGRES_URL=jdbc:postgresql://localhost:5432/itda
FILE_UPLOAD_PATH=./uploads
```

`docker compose`에서 사용하는 `.env`도 같은 DB 값으로 맞춰두는 것을 권장합니다.

### 2. PostgreSQL 실행

```bash
docker compose up -d postgres
```

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 4. 접속 주소

- 홈: `http://localhost:8080`
- 로그인: `http://localhost:8080/login`
- 회원가입: `http://localhost:8080/signup`

### 5. 테스트 실행

```bash
./gradlew test
```

## 프로젝트 구조

```text
.
├── .github/
│   ├── ISSUE_TEMPLATE/          # Feature / Bug 이슈 템플릿
│   ├── workflows/ci.yml         # main 기준 CI
│   └── PULL_REQUEST_TEMPLATE.md
├── compose.yaml                 # PostgreSQL 로컬 실행
├── docs/                        # 기획/도메인/백로그 문서
├── src/
│   ├── main/
│   │   ├── java/com/generic4/itda/
│   │   │   ├── config/          # Security, JPA, QueryDSL, Web 설정
│   │   │   ├── controller/      # 홈, 로그인, 회원가입 진입점
│   │   │   ├── domain/          # member, resume, file, skill, position 도메인
│   │   │   ├── dto/             # 폼/인증 DTO
│   │   │   ├── exception/       # 예외 정의
│   │   │   ├── repository/      # JPA Repository
│   │   │   ├── service/         # 회원/파일 서비스
│   │   │   └── ItDaApplication.java
│   │   └── resources/
│   │       ├── application.yaml
│   │       └── templates/       # index, login, signup
│   └── test/                    # domain / service / controller / repository 테스트
├── build.gradle
├── gradlew
└── README.md
```

## 협업 가이드

현재 저장소에는 아래 협업 자산이 포함되어 있습니다.

- 이슈 템플릿: `.github/ISSUE_TEMPLATE/feature.md`, `.github/ISSUE_TEMPLATE/bug.md`
- PR 템플릿: `.github/PULL_REQUEST_TEMPLATE.md`
- CI: `main` 브랜치 대상 `push`, `pull_request` 시 `./gradlew test` 실행

## 앞으로의 구현 방향

문서 기준 다음 단계는 아래 흐름을 중심으로 확장합니다.

- `Proposal`, `ProposalPosition`, `ProposalPositionSkill` 도메인 구현
- AI 브리프 입력/출력 모델 연결
- 프리랜서 검색 및 추천 결과 노출
- `Matching`, `MatchingAttachment`, `MatchingReview` 기반 진행 상태 관리
- 설명 가능한 추천 결과와 비교 UX를 위한 응답 모델 정교화
