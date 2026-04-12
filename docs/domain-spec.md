# IT-da 도메인 상세 명세

이 문서는 2026-04-10 기준 `ERDCloud` 초안을 기준으로 `member`, `resume`, `file`, `skill`, `proposal`, `proposal_position`,
`position`, `matching` 도메인을 다시 정렬한 명세다.
이전 대화에서 나온 확장 설계 중 ERD에 아직 반영되지 않은 항목은 현재 모델로 취급하지 않고, 문서 마지막의 `추후 재논의`로 내린다.

## 1. 기준과 범위

현재 문서의 source of truth는 아래 두 가지다.

- 현재 리포지토리의 구현 상태
- ERDCloud 초안에 반영된 현재 스키마

이번 문서의 범위는 아래와 같다.

- 회원과 이력서의 현재 ERD 필드
- 프로필 이미지, 이력서 첨부, 파일 메타데이터
- 스킬과 직무 마스터
- 클라이언트가 작성하는 제안서
- 제안서 내부의 모집 단위
- 직무 마스터와 모집 단위 요구 스킬
- 요청, 수락, 진행, 완료를 하나의 상태로 관리하는 매칭
- 현재 ERD 기준에서 추천 엔진이 읽을 수 있는 입력 구조

이번 문서의 비범위는 아래와 같다.

- 결제, 에스크로, 정산
- SNS 기능
- 관심 프리랜서 알림
- 외부 데이터 대량 연동
- 벡터 DB 도입 여부의 최종 결정

## 2. 현재 코드 기준 제약

현재 리포지토리에서 이미 확인된 제약은 아래와 같다.

- 인증은 세션 기반 Spring Security 폼 로그인이다.
- `Resume`는 `Member`와 1:1 관계다.
- `Resume`는 `careerYears`, `career` JSON, `preferredWorkType`, `publiclyVisible`, `aiMatchingEnabled`를 가진다.
- `Skill`은 별도 엔티티이며 `name`이 유니크하다.
- 파일은 로컬 저장 + `StoredFile` 메타데이터 저장 패턴을 사용한다.

현재 ERD와 코드가 어긋나는 부분도 있다.

- ERD에는 `members.memo`가 있지만 현재 코드에는 없다.
- ERD에는 `resumes.status`, `resumes.writing_status`, `resumes.portfolio_url`이 있지만 현재 코드에는 없다.
- ERD는 `resumes.career`를 nullable로 보지만 현재 코드는 필수로 검증한다.
- ERD에는 `profile_image`, `resume_attachments`가 있지만 현재 코드 엔티티는 아직 없다.
- ERD는 `stored_file.content_type`을 enum처럼 표현하지만 현재 코드는 `String`이다.

따라서 신규 도메인은 현재 패턴을 깨지 않는 선에서 추가하는 것이 우선이다.

## 3. 현재 ERD 기준 확정 결정

현재 ERD 초안 기준으로 확정하는 결정은 아래와 같다.

- `position`은 회원 소유가 아닌 얇은 직무 마스터다.
- `members`는 `memo` optional 필드를 가진다.
- `resumes`는 `status`, `writing_status`, `portfolio_url`을 가진다.
- `profile_image`와 `resume_attachments`는 현재 ERD에 존재한다.
- `stored_file`은 업로드 자산의 공통 메타데이터 테이블이다.
- `skills.description`은 optional이다.
- `proposal_position_skill`은 현재 ERD에 존재하는 정규화된 모집 단위-스킬 연결 테이블이다.
- `proposal`은 프로젝트 전체 수준의 문서다.
- `proposal`은 `raw_input_text`, `title`, `description`, `total_budget_*`, `work_type`, `work_place`, `expected_period`, `status`를 가진다.
- `proposal.status`는 `WRITING`, `MATCHING`, `COMPLETE` 3단계다.
- `proposal_position`은 실제 모집 단위지만 현재 ERD에서는 최소 필드만 가진다.
- `proposal_position`은 `position_id`, `head_count`, `unit_budget_*`, `status`를 가진다.
- `proposal_position.status`는 `OPEN`, `FULL`, `CLOSED`를 저장한다.
- `proposal_position_skill`은 `proposal_position`별 요구 스킬을 저장한다.
- `matching`은 `proposal_position`과 `resume`의 조합을 표현한다.
- `matching`은 `status` 단일 필드와 `contract_date`, `complete_date`만으로 흐름을 관리한다.
- 현재 ERD에는 `initiator_type`, `participation_status`, 시작/종료 승인 시각이 없다.
- 현재 ERD에는 `proposal_attachments`가 없다.
- MVP에서는 별도 `project` 테이블을 두지 않는다.
- 서비스 역할은 별도 컬럼으로 저장하지 않는다.
- 모든 활성 회원은 기본적으로 클라이언트 기능을 사용할 수 있다.
- `Resume`가 존재하면 프리랜서 기능을 사용할 수 있다.
- `Resume.publiclyVisible`은 발견 가능 영역 노출 여부를 제어한다.
- `Resume.aiMatchingEnabled`는 AI 추천 후보군 포함 여부만 제어한다.

## 4. 권장 집합 경계

### 4.1 Member / Resume

- `Member`는 계정 루트다.
- `Resume`는 프리랜서 프로필 루트다.
- `ProfileImage`, `ResumeAttachment`, `StoredFile`은 프로필 자산 루트다.
- 모든 활성 회원은 클라이언트로서 제안서를 작성할 수 있다.
- `Resume`가 생성된 회원은 프리랜서로서 추천, 지원, 매칭 흐름에 참여할 수 있다.

현재 도메인에서는 서비스 역할을 별도 필드로 저장하지 않고, 아래 규칙으로 파생시킨다.

- `canActAsClient(member) = member.status == ACTIVE`
- `canActAsFreelancer(member) = resume exists`
- `canBeRecommended(member) = resume exists && resume.status == ACTIVE && resume.writing_status == DONE && resume.publiclyVisible && resume.aiMatchingEnabled`

따라서 `UserRole`은 권한 관리 축으로만 두고, 서비스 역할은 `Resume` 존재 여부와 공개 설정으로 해석한다.

### 4.2 File Asset

- `StoredFile`은 실제 업로드 파일의 메타데이터 루트다.
- `ProfileImage`는 회원 프로필 대표 이미지를 연결한다.
- `ResumeAttachment`는 이력서에 연결되는 첨부 파일 목록이다.
- 현재 ERD에는 제안서 첨부파일 연결 테이블은 없다.

### 4.3 Proposal

- 제안서 초안과 모집 진행 상태를 관리하는 집합 루트
- 클라이언트가 소유
- 현재 저장 모델은 `raw_input_text`와 `description` 2단 구조다.
- `raw_input_text`는 사용자의 원본 자유 입력을 보존한다.
- `description`은 AI 브리프가 만든 뒤 사용자가 검토하고 수정하는 최종 제안서 본문이다.
- MVP에서는 별도 `overview` 필드를 두지 않고, 목록이나 카드 미리보기는 `description` 발췌로 처리한다.

### 4.4 ProposalPosition

- 하나의 제안서 내부에서 실제로 모집되는 단위
- 현재 ERD에서는 직무, 인원, 포지션 예산, 상태만 가진다.
- 직무별 세부 요구사항, 경력 범위, 근무 형태, 정렬 순서는 아직 정규화되어 있지 않다.

### 4.5 Matching

- 클라이언트 요청과 프리랜서 지원을 하나의 흐름으로 표현하는 집합 루트
- 대상은 항상 `proposal_position`과 `resume`의 조합이다.
- 현재 ERD는 요청 처리와 진행 이력을 `matching.status` 하나로 관리한다.

### 4.6 Project

- MVP에서는 별도 집합으로 두지 않는다.
- 현재 ERD는 프로젝트 전체 실행 엔티티보다 매칭 상태 이력에 집중한다.
- 공통 일정, 산출물, 채팅, 정산 같은 공유 실행 데이터가 생기면 그 시점에 별도 `project` 집합을 추가한다.

### 4.7 빠른 용어 사전

- `Member`: 계정 루트
- `Resume`: 프리랜서 프로필 루트
- `ProfileImage`: 회원 대표 이미지 연결
- `ResumeAttachment`: 이력서 첨부 파일 연결
- `StoredFile`: 업로드 자산 메타데이터
- `Position`: 공용 직무 마스터
- `ProposalPositionSkill`: 실제 모집 단위 요구 스킬
- `Proposal`: 클라이언트가 작성하는 프로젝트 문서
- `ProposalPosition`: 실제 모집 단위
- `Matching`: 요청, 수락, 진행, 완료 흐름 기록

## 5. 엔티티 명세

### 5.1 members

회원 계정 루트다.

| 필드                | 타입                              | 필수 | 설명                |
|-------------------|---------------------------------|----|-------------------|
| `id`              | bigint                          | Y  | PK                |
| `email`           | email                           | Y  | 이메일, 유니크          |
| `hashed_password` | varchar                         | Y  | BCrypt 등 암호화 비밀번호 |
| `name`            | varchar(100)                    | Y  | 이름                |
| `nickname`        | varchar(100)                    | N  | 닉네임               |
| `phone`           | phone                           | Y  | 전화번호              |
| `role`            | enum(`ADMIN`, `USER`)           | Y  | 권한 축              |
| `type`            | enum(`CORPORATE`, `INDIVIDUAL`) | Y  | 회원 유형             |
| `status`          | enum(`ACTIVE`, `INACTIVE`)      | Y  | 계정 상태             |
| `memo`            | varchar                         | N  | 내부 메모             |
| `created_at`      | timestamp                       | Y  | 생성 시각             |
| `modified_at`     | timestamp                       | Y  | 수정 시각             |

규칙은 아래와 같다.

- `role`은 서비스 역할이 아니라 권한 축이다.
- 서비스 역할은 `Resume` 존재 여부로 파생한다.
- `memo`는 현재 ERD에는 있지만 현재 코드 엔티티에는 아직 없다.

### 5.2 resumes

프리랜서 프로필 루트다.

| 필드                    | 타입                               | 필수 | 설명              |
|-----------------------|----------------------------------|----|-----------------|
| `id`                  | bigint                           | Y  | PK              |
| `member_id`           | bigint                           | Y  | 회원 1:1 연결       |
| `introduction`        | text                             | Y  | 자기소개            |
| `career_years`        | tinyint                          | Y  | 경력 연차           |
| `career`              | json                             | Y  | 경력 상세 JSON      |
| `preferred_work_type` | enum(`SITE`, `REMOTE`, `HYBRID`) | Y  | 선호 근무 형태        |
| `publicly_visible`    | boolean                          | Y  | 검색/목록 노출 여부     |
| `ai_matching_enabled` | boolean                          | Y  | AI 추천 후보군 포함 여부 |
| `writing_status`      | enum(`WRITING`, `DONE`)          | Y  | 이력서 작성 상태       |
| `status`              | enum(`ACTIVE`, `INACTIVE`)       | Y  | 이력서 상태          |
| `portfolio_url`       | varchar                          | N  | 외부 포트폴리오 링크     |
| `created_at`          | timestamp                        | Y  | 생성 시각           |
| `modified_at`         | timestamp                        | Y  | 수정 시각           |
| `created_by`          | varchar                          | N  | 감사 정보           |
| `modified_by`         | varchar                          | N  | 감사 정보           |

규칙은 아래와 같다.

- `member_id`는 1:1 연결이므로 유니크 제약이 필요하다.
- `preferred_work_type` 기본값은 `SITE`다.
- `publicly_visible`, `ai_matching_enabled` 기본값은 `true`다.
- 현재 ERD는 `career`를 nullable로 보지만 현재 코드는 필수로 검증한다.
- 현재 ERD에는 `writing_status`, `status`, `portfolio_url`이 있지만 현재 코드 엔티티에는 아직 없다.

### 5.3 profile_images

회원 프로필 대표 이미지를 연결하는 테이블이다.

| 필드            | 타입        | 필수 | 설명       |
|---------------|-----------|----|----------|
| `id`          | bigint    | Y  | PK       |
| `member_id`   | bigint    | Y  | 회원       |
| `file_id`     | bigint    | Y  | 파일 메타데이터 |
| `created_at`  | timestamp | Y  | 생성 시각    |
| `modified_at` | timestamp | Y  | 수정 시각    |

규칙은 아래와 같다.

- 용도상 회원당 대표 프로필 이미지는 1개이므로 `member_id` 유니크 제약을 두는 편이 안전하다.
- 현재 ERD에는 존재하지만 현재 코드 엔티티는 아직 없다.

### 5.4 stored_files

업로드 자산의 공통 메타데이터다.

| 필드              | 타입        | 필수 | 설명        |
|-----------------|-----------|----|-----------|
| `id`            | bigint    | Y  | PK        |
| `original_name` | varchar   | Y  | 원본 파일명    |
| `stored_name`   | varchar   | Y  | 저장 파일명    |
| `file_url`      | varchar   | Y  | 논리적 요청 경로 |
| `content_type`  | enum      | Y  | 콘텐츠 타입    |
| `size`          | bigint    | Y  | 바이트 단위 크기 |
| `created_at`    | timestamp | Y  | 생성 시각     |
| `modified_at`   | timestamp | Y  | 수정 시각     |
| `created_by`    | varchar   | N  | 감사 정보     |
| `modified_by`   | varchar   | N  | 감사 정보     |

규칙은 아래와 같다.

- `file_url`은 실제 저장 경로가 아니라 애플리케이션이 노출하는 논리 경로다.
- 현재 ERD는 `content_type`을 enum처럼 표현하지만 현재 코드는 `String`으로 구현돼 있다.

### 5.5 resume_attachments

이력서에 연결된 첨부 파일 목록이다.

| 필드              | 타입        | 필수 | 설명           |
|-----------------|-----------|----|--------------|
| `id`            | bigint    | Y  | PK           |
| `resume_id`     | bigint    | Y  | 이력서          |
| `file_id`       | bigint    | Y  | 파일 메타데이터     |
| `display_order` | integer   | Y  | 0-indexed 정렬 |
| `created_at`    | timestamp | Y  | 생성 시각        |
| `modified_at`   | timestamp | Y  | 수정 시각        |

규칙은 아래와 같다.

- 첨부파일은 이력서당 여러 개를 가질 수 있다.
- `display_order`는 현재 ERD 주석 기준 0-indexed다.
- `UNIQUE (resume_id, display_order)`를 두는 편이 안전하다.
- 현재 ERD에는 존재하지만 현재 코드 엔티티는 아직 없다.

### 5.6 skills

정규화된 스킬 마스터다.

| 필드            | 타입           | 필수 | 설명       |
|---------------|--------------|----|----------|
| `id`          | bigint       | Y  | PK       |
| `name`        | varchar(100) | Y  | 스킬명, 유니크 |
| `description` | varchar      | N  | 스킬 설명    |
| `created_at`  | timestamp    | Y  | 생성 시각    |
| `modified_at` | timestamp    | Y  | 수정 시각    |

규칙은 아래와 같다.

- `name`은 trim 후 저장한다.
- `description`은 optional이다.

### 5.7 positions

직무 마스터다. 분류 코드 테이블에 가깝다.

| 필드            | 타입           | 필수 | 설명       |
|---------------|--------------|----|----------|
| `id`          | bigint       | Y  | PK       |
| `name`        | varchar(100) | Y  | 직무명, 유니크 |
| `created_at`  | timestamp    | Y  | 생성 시각    |
| `modified_at` | timestamp    | Y  | 수정 시각    |

규칙은 아래와 같다.

- `name`은 trim 후 저장한다.
- `name`은 유니크해야 한다.
- `description`, `status`, `member_id`는 현재 ERD에 없다.

### 5.8 proposal_position_skills

현재 ERD에서 모집 단위와 스킬을 연결하는 정규화 테이블이다.

| 필드            | 타입                              | 필수 | 설명     |
|---------------|---------------------------------|----|--------|
| `id`          | bigint                          | Y  | PK     |
| `proposal_position_id` | bigint                          | Y  | 모집 단위    |
| `skill_id`             | bigint                          | Y  | 스킬      |
| `importance`           | enum(`PREFERENCE`, `ESSENTIAL`) | N  | 우대/필수   |
| `created_at`           | timestamp                       | Y  | 생성 시각   |
| `modified_at`          | timestamp                       | Y  | 수정 시각   |

현재 역할은 아래와 같다.

- `proposal_position`별 실제 요구 스킬 저장
- 추천 엔진이 읽는 현재 normalized requirement source
- 매칭 단위별 필수/우대 스킬 표현

해석 기준은 아래와 같다.

- 이 테이블의 FK 기준은 `proposal_position.id`다.
- 따라서 이름이 비슷하더라도 직무 마스터 `position`의 기본 스킬 템플릿으로 해석하지 않는다.

### 5.9 proposals

프로젝트 전체 수준의 제안서다.

| 필드                 | 타입                                      | 필수 | 설명                       |
|--------------------|-----------------------------------------|----|--------------------------|
| `id`               | bigint                                  | Y  | PK                       |
| `member_id`        | bigint                                  | Y  | 제안서 작성 클라이언트             |
| `title`            | varchar(200)                            | Y  | 프로젝트 제목                  |
| `raw_input_text`   | text                                    | Y  | 사용자 입력 원문                |
| `description`      | text                                    | N  | 프로젝트 설명                  |
| `total_budget_min` | bigint                                  | N  | 전체 프로젝트 최소 예산            |
| `total_budget_max` | bigint                                  | N  | 전체 프로젝트 최대 예산            |
| `work_type`        | enum(`SITE`, `REMOTE`, `HYBRID`)        | N  | 전체 프로젝트 근무 형태            |
| `work_place`       | varchar(255)                            | N  | 근무 장소 텍스트                |
| `expected_period`  | bigint                                  | N  | 예상 기간, 현재 ERD 주석 기준 주 단위 |
| `status`           | enum(`WRITING`, `MATCHING`, `COMPLETE`) | Y  | 제안서 상태                   |
| `created_at`       | timestamp                               | Y  | 생성 시각                    |
| `modified_at`      | timestamp                               | Y  | 수정 시각                    |
| `created_by`       | varchar                                 | N  | 감사 정보                    |
| `modified_by`      | varchar                                 | N  | 감사 정보                    |

규칙은 아래와 같다.

- `member_id`는 클라이언트 계정이어야 한다.
- `total_budget_min`, `total_budget_max`는 둘 다 null일 수 있다.
- 둘 다 존재하면 `total_budget_min <= total_budget_max`여야 한다.
- 현재 ERD 기준으로 추천과 모집 시작 상태는 `MATCHING`이다.
- `COMPLETE`는 제안서 단위의 종료 상태다.

### 5.10 proposal_positions

실제 모집 단위다.

| 필드                | 타입                             | 필수 | 설명          |
|-------------------|--------------------------------|----|-------------|
| `id`              | bigint                         | Y  | PK          |
| `proposal_id`     | bigint                         | Y  | 상위 제안서      |
| `position_id`     | bigint                         | Y  | 직무 마스터      |
| `head_count`      | bigint                         | N  | 모집 인원       |
| `status`          | enum(`OPEN`, `FULL`, `CLOSED`) | Y  | 모집 상태       |
| `unit_budget_min` | bigint                         | N  | 1인 기준 최소 예산 |
| `unit_budget_max` | bigint                         | N  | 1인 기준 최대 예산 |
| `created_at`      | timestamp                      | Y  | 생성 시각       |
| `modified_at`     | timestamp                      | Y  | 수정 시각       |
| `created_by`      | varchar                        | N  | 감사 정보       |
| `modified_by`     | varchar                        | N  | 감사 정보       |

규칙은 아래와 같다.

- `head_count`는 현재 ERD에서는 nullable이지만, 서비스 계층에서는 모집 시작 전 양수 검증을 두는 편이 안전하다.
- 예산이 둘 다 존재하면 `unit_budget_min <= unit_budget_max`여야 한다.
- `FULL`은 현재 ERD에서는 파생 상태가 아니라 저장 상태다.
- `CLOSED`이면 신규 요청과 지원을 받을 수 없다.
- 현재 ERD에는 포지션별 제목, 요구사항 요약, 근무 형태, 경력 범위, 정렬 순서가 없다.
- 현재 ERD에는 같은 `proposal_id + position_id` 중복을 막는 제약이 보이지 않는다.

### 5.11 resume_skills

이력서의 보유 스킬과 숙련도다.

| 필드                  | 타입                                           | 필수 | 설명    |
|---------------------|----------------------------------------------|----|-------|
| `id`                | bigint                                       | Y  | PK    |
| `resume_id`         | bigint                                       | Y  | 이력서   |
| `skill_id`          | bigint                                       | Y  | 보유 스킬 |
| `proficiency_level` | enum(`BEGINNER`, `INTERMEDIATE`, `ADVANCED`) | Y  | 숙련도   |
| `created_at`        | timestamp                                    | Y  | 생성 시각 |
| `modified_at`       | timestamp                                    | Y  | 수정 시각 |

규칙은 아래와 같다.

- `UNIQUE (resume_id, skill_id)`를 두는 편이 안전하다.
- 추천 대상은 `publiclyVisible = true`인 이력서만 포함한다.
- AI 추천 대상은 `aiMatchingEnabled = true`인 이력서만 포함한다.
- 직접 지원은 `aiMatchingEnabled = false`여도 허용할 수 있다.

### 5.12 matchings

요청, 수락, 진행, 완료를 하나의 상태 필드로 다루는 핵심 집합이다.

| 필드                     | 타입                                                                                | 필수 | 설명                       |
|------------------------|-----------------------------------------------------------------------------------|----|--------------------------|
| `id`                   | bigint                                                                            | Y  | PK                       |
| `proposal_position_id` | bigint                                                                            | Y  | 대상 모집 단위                 |
| `resume_id`            | bigint                                                                            | Y  | 대상 이력서                   |
| `status`               | enum(`PROPOSED`, `ACCEPTED`, `REJECTED`, `IN_PROGRESS`, `COMPLETED`, `CANCELED`) | Y  | 단일 매칭 상태, 기본값 `PROPOSED` |
| `contract_date`        | timestamp                                                                         | N  | 수락 또는 계약 성립 시각           |
| `complete_date`        | timestamp                                                                         | N  | 완료 시각                    |
| `created_at`           | timestamp                                                                         | Y  | 생성 시각                    |
| `modified_at`          | timestamp                                                                         | Y  | 수정 시각                    |

현재 ERD 기준 핵심 규칙은 아래와 같다.

- 한 `proposal_position`과 한 `resume` 조합에는 동시에 하나의 활성 매칭만 허용하는 편이 안전하다.
- 현재 ERD는 `initiator_type`을 저장하지 않으므로, 클라이언트 요청인지 프리랜서 지원인지는 API나 감사 로그에서 구분해야 한다.
- 현재 ERD는 `participation_status`를 별도로 두지 않으므로 요청 처리와 진행 상태를 하나의 `status`가 모두 담당한다.
- 연락처 공개 시점은 `ACCEPTED` 이후로 해석하는 것이 자연스럽다.
- `contract_date`와 `complete_date`는 각각 수락/완료 시점 기록으로 사용한다.

### 5.13 상태별 허용 액션 매트릭스

현재 ERD 기준으로 구현할 때 해석하기 쉬운 최소 액션 매트릭스는 아래와 같다.

| 집합                  | 상태                     | 허용 액션                   | 비고            |
|---------------------|------------------------|-------------------------|---------------|
| `proposal`          | `WRITING`              | 생성, 수정, 포지션 편집          | 추천/매칭 진입 전 상태 |
| `proposal`          | `MATCHING`             | 조회, 추천 조회, 매칭 진행, 완료 처리 | 수정 정책은 추후 재논의 |
| `proposal`          | `COMPLETE`             | 조회                      | 신규 모집 종료 상태   |
| `proposal_position` | `OPEN`                 | 신규 요청/지원 생성             | 정원 미충족 전제     |
| `proposal_position` | `FULL`                 | 조회, 기존 매칭 관리            | 신규 요청/지원 차단   |
| `proposal_position` | `CLOSED`               | 조회                      | 신규 요청/지원 차단   |
| `matching`          | `PROPOSED`             | 수락, 거절, 취소              | 연락처 비공개       |
| `matching`          | `ACCEPTED`             | 진행 시작                   | 연락처 공개 가능     |
| `matching`          | `IN_PROGRESS`          | 완료 처리                   | 진행 중 상태       |
| `matching`          | `COMPLETED`            | 조회                      | 완료 상태         |
| `matching`          | `REJECTED`, `CANCELED` | 조회                      | 종료 상태         |

이 매트릭스는 현재 ERD에서 직접 드러나지 않는 운영 해석을 정리한 것이다.
코드 구현 전 서비스 정책과 다르면 여기부터 먼저 맞추는 편이 안전하다.

## 6. 상태 모델

### 6.1 Proposal 상태

현재 ERD 기준 상태는 아래 3개다.

```text
WRITING -> MATCHING -> COMPLETE
```

의미는 아래처럼 해석한다.

- `WRITING`: 작성 중
- `MATCHING`: 제출 완료 및 모집/추천 진행 중
- `COMPLETE`: 제안서 종료

중요한 점은 아래와 같다.

- 이전 문서에서 가정했던 `DONE -> WRITING` 재오픈 정책은 현재 ERD에 반영되어 있지 않다.
- `MATCHING` 상태 제안서의 수정 가능 범위와 재오픈 정책은 추후 재논의 대상이다.

### 6.2 ProposalPosition 상태

현재 ERD 기준 상태는 아래 3개다.

```text
OPEN -> FULL -> CLOSED
```

현재 모델에서는 `FULL`도 저장 상태다.
즉, 정원 충족 여부를 계산으로만 다루지 않고 상태값으로 유지한다.

### 6.3 Matching 상태

현재 ERD는 단일 상태 필드로 흐름을 관리한다.
실무적으로는 아래 흐름으로 해석하는 것이 자연스럽다.

```text
PROPOSED -> ACCEPTED -> IN_PROGRESS -> COMPLETED
PROPOSED -> REJECTED
PROPOSED -> CANCELED
```

단, 정확한 enum literal은 코드 생성 전 ERDCloud 원본에서 한 번 더 확정하는 편이 안전하다.
현재 문서의 핵심 포인트는 아래와 같다.

- 요청/수락 상태와 참여 상태가 분리되어 있지 않다.
- 별도 시작 승인, 종료 승인 개념은 현재 ERD에 없다.

## 7. 현재 ERD 기준 추천 엔진 입력

현재 ERD 기준 추천 엔진이 읽을 수 있는 입력은 아래와 같다.

### 이력서 쪽

- `resume.status`
- `resume.writing_status`
- `resume.career_years`
- `resume.preferred_work_type`
- `resume.publicly_visible`
- `resume.ai_matching_enabled`
- `resume.portfolio_url`
- `resume_skill.skill_id`
- `resume_skill.proficiency_level`
- `career` JSON의 상세 경력

### 제안서 쪽

- `proposal.description`
- `proposal.work_type`
- `proposal.work_place`
- `proposal.expected_period`
- `proposal_position.position_id`
- `proposal_position.head_count`
- `proposal_position.unit_budget_min`
- `proposal_position.unit_budget_max`
- `proposal_position_skill.skill_id`
- `proposal_position_skill.importance`

현재 모델의 한계는 아래와 같다.

- `proposal_position_skill`은 현재 스킬 ID와 중요도만 가지므로 요구 이유나 숙련도 조건을 담기 어렵다.
- 직무 마스터 `position` 자체에는 기본 스킬 템플릿이 없어서 생성 보조 시 재사용성이 약하다.
- 포지션별 경력 범위나 요구사항 요약이 없어서 하드 필터와 설명 근거가 거칠어진다.

## 8. 핵심 운영 규칙

현재 모델에서 최소한으로 지켜야 할 규칙은 아래와 같다.

1. `Resume`가 있어야 프리랜서 기능을 사용할 수 있다.
2. `resume.status = INACTIVE`면 프리랜서 프로필로 취급하지 않는다.
3. `resume.writing_status = WRITING`이면 추천 노출 대상에서 제외하는 편이 자연스럽다.
4. `resume.publiclyVisible = false`면 검색과 추천 결과에서 제외한다.
5. `resume.aiMatchingEnabled = false`면 AI 추천 결과에서 제외하지만 직접 지원은 허용 가능하다.
6. `proposal_position.status = FULL` 또는 `CLOSED`이면 신규 매칭 생성을 막아야 한다.
7. 연락처는 `matching.status = ACCEPTED` 이전에는 공개하지 않는다.
8. 전체 예산과 포지션 예산은 의미가 다르므로 별도로 유지한다.
9. 현재 ERD에는 `proposal_attachments`가 없으므로 제안서 첨부파일은 아직 정식 스키마에 포함되지 않는다.
10. 현재 ERD에는 시작/종료 양측 승인 모델이 없으므로, 진행/완료는 `matching.status` 단일 상태로 처리한다.

## 9. 구현 전 확인 체크리스트

실제 엔티티/서비스를 만들기 전에 아래 항목을 한 번 더 확인한다.

1. `members.memo`를 내부 전용 필드로만 둘지
2. `resumes.career`를 nullable로 둘지 현재 코드처럼 필수로 둘지
3. `profile_image.member_id`를 1:1 unique로 강제할지
4. `resume_attachments.display_order`의 unique 제약 범위
5. `proposal.status = MATCHING` 진입 최소 조건
6. `proposal_position.status = FULL` 갱신 트리거
7. `matching.status`의 정확한 enum literal과 전이 표
8. `StoredFile.contentType`을 enum으로 바꿀지 문자열로 유지할지

## 10. 추후 재논의 항목

아래 항목은 이전 대화에서 한 번 논의됐지만, 현재 ERD 초안에는 아직 반영되지 않았다.
현재 문서에서는 "미래 확장 후보"로만 유지한다.

### 10.1 proposal_position 상세 필드 확장

- `title`
- `requirement_summary`
- `work_type`
- `career_years_min`
- `career_years_max`
- `sort_order`

현재 ERD는 최소 모집 단위만 갖고 있어서, 포지션별 세부 조건을 표현하기 어렵다.

### 10.2 position 기본 스킬 템플릿

현재 요구 스킬 source는 `proposal_position_skill`이다.
향후 AI 브리프 생성 보조나 직무별 기본값 재사용이 필요해지면, 별도의 `position_skill` 또는 `position_default_skill` 템플릿 모델을 검토할 수 있다.

### 10.3 proposal_attachments

현재 리포의 파일 저장 구조로는 바로 붙일 수 있지만, 현재 ERD에는 테이블이 없다.

### 10.4 matching 상태 분리

이전 논의에서는 아래 항목이 있었다.

- `initiator_type`
- `requested_by_member_id`
- `participation_status`
- 시작 승인 시각
- 종료 승인 시각

현재 ERD에는 없다.
요청/수락 흐름과 실제 참여 이력을 분리하려면 추후 스키마 재설계가 필요하다.

### 10.5 양측 시작 승인 / 종료 승인

이전 논의에서는 아래 규칙을 검토했다.

- 클라이언트 요청은 시작 승인으로 보지 않음
- 매칭 성립 후 양측이 별도로 시작 승인해야 진행 상태 진입
- 진행 중 양측이 별도로 종료 승인해야 완료 상태 진입

현재 ERD에는 이 모델이 반영되어 있지 않으므로, 추후 재논의 대상이다.
