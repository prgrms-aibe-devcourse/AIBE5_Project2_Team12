# IT-da 도메인 상세 명세

이 문서는 2026-04-19 기준 `ERDCloud` 초안과 이번에 함께 반영하는 추천 도메인 테이블(`5.16 ~ 5.18`)을 기준으로
`member`, `resume`, `file`, `skill`, `proposal`, `proposal_position`, `position`, `matching` 도메인을 다시 정렬한 명세다.
이전 대화에서 나온 확장 설계 중 ERD에 아직 반영되지 않은 항목은 현재 모델로 취급하지 않고, 문서 마지막의 `추후 재논의`로 내린다.

## 1. 기준과 범위

현재 문서의 source of truth는 아래 두 가지다.

- 현재 리포지토리의 구현 상태
- ERDCloud 초안과 이번 ERD 변경안에 반영하는 현재 스키마

이번 문서의 범위는 아래와 같다.

- 회원과 이력서의 현재 ERD 필드
- 프로필 이미지, 이력서 첨부, 파일 메타데이터
- 스킬과 직무 마스터
- 클라이언트가 작성하는 제안서
- 제안서 내부의 모집 단위
- 직무 마스터와 모집 단위 요구 스킬
- 요청, 수락, 진행, 완료를 하나의 상태로 관리하는 매칭
- 현재 ERD 기준에서 추천 엔진이 읽을 수 있는 입력 구조
- MVP 추천 결과 저장 구조

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
- `ProfileImage`는 `StoredFile`을 감싸는 별도 엔티티이며, 현재 ERD와 코드 모두 `Member`를 1:1 연관관계 주인으로 둔다.

현재 ERD와 코드가 완전히 일치하지 않는 부분도 있다.

- `members.memo`는 현재 코드에도 반영되어 있다.
- `resumes.status`, `resumes.writing_status`, `resumes.portfolio_url`도 현재 코드에 반영되어 있다.
- ERD는 `resumes.career`를 nullable로 볼 여지가 있지만 현재 코드는 필수로 검증한다.
- `resume_attachments`와 `profile_image`는 현재 코드 엔티티가 존재한다.
- `stored_file.content_type`은 ERDCloud 표기와 별개로 현재 구현 기준 MIME 문자열(`String` / `varchar`)로 본다.

따라서 신규 도메인은 현재 패턴을 깨지 않는 선에서 추가하는 것이 우선이다.

## 3. 현재 ERD 기준 확정 결정

현재 ERD 초안 기준으로 확정하는 결정은 아래와 같다.

- `position`은 회원 소유가 아닌 얇은 직무 마스터다.
- `members`는 `memo` optional 필드를 가진다.
- `resumes`는 `status`, `writing_status`, `portfolio_url`을 가진다.
- `profile_image`와 `resume_attachments`는 현재 ERD에 존재한다.
- `stored_file`은 업로드 자산의 공통 메타데이터 테이블이다.
- `stored_file.content_type`은 enum이 아니라 MIME 타입 문자열(`varchar`)로 저장한다.
- `skills.description`은 optional이다.
- `proposal_position_skills`는 현재 ERD에 존재하는 정규화된 모집 단위-스킬 연결 테이블이다.
- `proposal`은 프로젝트 전체 수준의 문서다.
- `proposal`은 `raw_input_text`, `title`, `description`, `total_budget_*`, `expected_period`, `status`를 가진다.
- `proposal.status`는 `WRITING`, `MATCHING`, `COMPLETE` 3단계다.
- `proposal_position`은 실제 모집 단위이며 `position_id`, `title`, `work_type`, `head_count`, `unit_budget_*`,
  `expected_period`, `career_min_years`, `career_max_years`, `work_place`, `status`를 가진다.
- `proposal_position.status`는 `OPEN`, `FULL`, `CLOSED`를 저장한다.
- `proposal_position_skills`는 `proposal_position`별 요구 스킬을 저장한다.
- `matching`은 `proposal_position`과 `resume`의 조합을 표현하되, 당사자 판정 anchor로 `client_member_id`, `freelancer_member_id`를 함께 가진다.
- `matching`은 `status` 단일 필드와 `contract_date`, `complete_date`만으로 흐름을 관리한다.
- 현재 ERD에는 `initiator_type`, `participation_status`, 시작/종료 승인 시각이 없다.
- 매칭 단위 계약서/완료 증빙은 `matching_attachments`, 상호 리뷰는 `matching_reviews`로 관리한다.
- 현재 ERD에는 `proposal_attachments`가 없다.
- MVP에서는 별도 `project` 테이블을 두지 않는다.
- 서비스 역할은 별도 컬럼으로 저장하지 않는다.
- 모든 활성 회원은 기본적으로 클라이언트 기능을 사용할 수 있다.
- `Resume`가 존재하고 `resume.status = ACTIVE`이면 프리랜서로서 직접 지원과 매칭 흐름에 참여할 수 있다.
- `resume.writing_status = DONE`이고 `resume.publicly_visible = true`여야 검색/목록 노출 대상이 된다.
- `resume.ai_matching_enabled`는 그 전제 조건을 만족한 이력서 중 AI 추천 후보군 포함 여부만 추가로 제어한다.
- MVP 추천 엔진을 위해 `recommendation_runs`, `recommendation_results`, `resume_embeddings`를 ERD에 추가한다.
- `proposal_position_embeddings`는 MVP에서 추가하지 않는다.
- MVP에서는 `resume_embeddings.embedding_vector`를 `jsonb`로 저장하고 `pgvector` extension은 도입하지 않는다.
- MVP에서는 외부 MQ를 두지 않고 `recommendation_results.llm_status` 상태 전이로 설명 생성 흐름을 관리한다.

## 4. 권장 집합 경계

### 4.1 Member / Resume

- `Member`는 계정 루트다.
- `Resume`는 프리랜서 프로필 루트다.
- `ProfileImage`, `ResumeAttachment`, `StoredFile`은 프로필 자산 루트다.
- 모든 활성 회원은 클라이언트로서 제안서를 작성할 수 있다.
- `Resume`가 생성되고 활성 상태인 회원은 프리랜서로서 지원과 매칭 흐름에 참여할 수 있다.

현재 도메인에서는 서비스 역할을 별도 필드로 저장하지 않고, 아래 규칙으로 파생시킨다.

- `canActAsClient(member) = member.status == ACTIVE`
- `canActAsFreelancer(member) = resume exists && resume.status == ACTIVE`
- `canApplyDirectly(member) = resume exists && resume.status == ACTIVE`
- `canBeListed(member) = resume exists && resume.status == ACTIVE && resume.writing_status == DONE && resume.publicly_visible`
- `canBeRecommended(member) = resume exists && resume.status == ACTIVE && resume.writing_status == DONE && resume.publicly_visible && resume.ai_matching_enabled`

따라서 `UserRole`은 권한 관리 축으로만 두고, 서비스 역할은 `Resume` 존재 여부, 이력서 상태, 공개 설정으로 해석한다.

### 4.2 File Asset

- `StoredFile`은 실제 업로드 파일의 메타데이터 루트다.
- `ProfileImage`는 회원 프로필 대표 이미지를 연결하는 별도 엔티티다.
- 현재 ERD와 코드 모두 `Member`를 `ProfileImage` 1:1 연관관계 주인으로 두고, 회원 조회 시 프로필 이미지를 필요할 때만 지연 로딩하는 방향을 택한다.
- `ProfileImage`를 제거하고 `StoredFile`로 통합할지 여부는 프로필 이미지 서비스 책임을 정리한 뒤 다시 결정한다.
- `ResumeAttachment`는 이력서에 연결되는 첨부 파일 목록이다.
- 애플리케이션 레벨에서는 `/files/profile/**`, `/files/proposal/**`, `/files/resume/**` 공개 경로를 이미 사용한다.
- 현재 ERD에는 제안서 첨부파일 연결 테이블은 없다.
- 따라서 제안서 파일 업로드 경로가 존재하더라도 현재 시점에서는 도메인 연관이 확정된 자산이 아니라 임시 업로드 또는 화면 보조 자산으로 해석한다.

### 4.3 Proposal

- 제안서 초안과 모집 진행 상태를 관리하는 집합 루트
- 클라이언트가 소유
- 현재 저장 모델은 `raw_input_text`와 `description` 2단 구조다.
- `raw_input_text`는 사용자의 원본 자유 입력을 보존한다.
- `description`은 AI 브리프가 만든 뒤 사용자가 검토하고 수정하는 최종 제안서 본문이다.
- MVP에서는 별도 `overview` 필드를 두지 않고, 목록이나 카드 미리보기는 `description` 발췌로 처리한다.

### 4.4 ProposalPosition

- 하나의 제안서 내부에서 실제로 모집되는 단위
- 직무 마스터(`position`)와 사용자에게 보이는 포지션 제목(`title`)을 함께 가진다.
- 근무 형태, 근무지, 포지션별 예상 기간, 경력 범위, 인원, 예산, 요구 스킬을 포지션 단위에서 관리한다.

### 4.5 Matching

- 클라이언트 요청과 프리랜서 지원을 하나의 흐름으로 표현하는 집합 루트
- 대상은 항상 `proposal_position`과 `resume`의 조합이다.
- 참여자 판정과 권한 검증을 단순화하기 위해 `client_member_id`, `freelancer_member_id`를 직접 가진다.
- 현재 ERD는 요청 처리와 진행 이력을 `matching.status` 하나로 관리한다.
- 계약서/완료 증빙은 `MatchingAttachment`, 상호 리뷰는 `MatchingReview`를 종속 집합으로 둔다.

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
- `MatchingAttachment`: 매칭 단위 계약서/완료 증빙 연결
- `MatchingReview`: 매칭 단위 상호 리뷰

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
| `profile_image_id`| bigint                          | N  | 대표 프로필 이미지 FK    |
| `created_at`      | timestamp                       | Y  | 생성 시각             |
| `modified_at`     | timestamp                       | Y  | 수정 시각             |

규칙은 아래와 같다.

- `role`은 서비스 역할이 아니라 권한 축이다.
- 서비스 역할은 `Resume` 존재 여부, 이력서 상태, 공개 설정으로 파생한다.
- 일반 회원가입 기본값은 `role = USER`, `type = INDIVIDUAL`, `status = ACTIVE`다.
- `name`과 `nickname`은 trim 후 저장한다.
- `nickname`이 비어 있으면 `name`으로 대체한다.
- `email`은 값 객체에서 형식을 검증하고 유니크하게 취급한다.
- `phone`은 지원 가능한 국내 전화번호 형식만 허용하고, 하이픈을 제거한 값으로 정규화해 저장한다.
- `delete()`와 `restore()`는 물리 삭제가 아니라 `status` 변경 기반 soft delete / restore다.
- `memo`는 현재 ERD에는 있지만 현재 코드 엔티티에는 아직 없다.
- 현재 ERD와 코드 모두 `profile_image_id` FK로 `ProfileImage`를 참조하고, `Member`가 1:1 연관관계 주인이다.
- `profile_image_id`는 optional이며, 회원이 프로필 이미지를 등록한 경우에만 값을 가진다.
- 회원당 대표 프로필 이미지는 1개이므로 `members.profile_image_id` 1:1 제약으로 관리한다.

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

- `member_id`는 1:1 연결이므로 `UNIQUE (member_id)`를 강제한다.
- `preferred_work_type` 기본값은 `SITE`다.
- `publicly_visible`, `ai_matching_enabled` 기본값은 `true`다.
- 현재 코드 기준으로 `introduction`, `career_years`, `career`는 모두 필수다.
- 현재 코드 기준으로 `career_years >= 0`이어야 한다.
- 현재 ERD에는 `writing_status`, `status`, `portfolio_url`이 있지만 현재 코드 엔티티에는 아직 없다.

### 5.3 profile_images

회원 프로필 대표 이미지를 연결하는 테이블이다.

| 필드            | 타입        | 필수 | 설명       |
|---------------|-----------|----|----------|
| `id`          | bigint    | Y  | PK       |
| `file_id`     | bigint    | Y  | 파일 메타데이터 |
| `created_at`  | timestamp | Y  | 생성 시각    |
| `modified_at` | timestamp | Y  | 수정 시각    |

규칙은 아래와 같다.

- 회원과의 1:1 연결은 `profile_images`가 아니라 `members.profile_image_id` FK에서 관리한다.
- `file_id`는 필수이며, `ProfileImage`는 현재 `StoredFile`을 감싼 얇은 래퍼다.
- 현재 `ProfileImage`는 `StoredFile`을 감싼 얇은 래퍼로 유지하고, 프로필 이미지 서비스 경계를 확인한 뒤 통합 여부를 다시 본다.

### 5.4 stored_files

업로드 자산의 공통 메타데이터다.

| 필드              | 타입        | 필수 | 설명        |
|-----------------|-----------|----|-----------|
| `id`            | bigint    | Y  | PK        |
| `original_name` | varchar   | Y  | 원본 파일명    |
| `stored_name`   | varchar   | Y  | 저장 파일명    |
| `file_url`      | varchar   | Y  | 논리적 요청 경로 |
| `content_type`  | varchar   | Y  | MIME 타입 문자열 |
| `size`          | bigint    | Y  | 바이트 단위 크기 |
| `created_at`    | timestamp | Y  | 생성 시각     |
| `modified_at`   | timestamp | Y  | 수정 시각     |
| `created_by`    | varchar   | N  | 감사 정보     |
| `modified_by`   | varchar   | N  | 감사 정보     |

규칙은 아래와 같다.

- `file_url`은 실제 저장 경로가 아니라 애플리케이션이 노출하는 논리 경로다.
- 현재 코드 기준으로 `file_url`은 반드시 `/`로 시작해야 한다.
- 현재 코드 기준으로 `file_url`의 base path는 `/files/profile/`, `/files/proposal/`, `/files/resume/` 중 하나다.
- 현재 코드 기준으로 `original_name`, `stored_name`, `content_type`은 모두 필수다.
- 현재 코드 기준으로 `content_type`은 `MultipartFile.getContentType()`에서 얻은 MIME 문자열을 저장한다.
- 현재 코드 기준으로 `size`는 `0` 이상이어야 한다.
- MVP 단계에서는 `content_type`을 enum으로 정규화하지 않고 문자열 allowlist 검증으로 다룬다.

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
- `UNIQUE (resume_id, display_order)`를 강제한다.
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

| 필드                     | 타입                              | 필수 | 설명    |
|------------------------|---------------------------------|----|-------|
| `id`                   | bigint                          | Y  | PK    |
| `proposal_position_id` | bigint                          | Y  | 모집 단위 |
| `skill_id`             | bigint                          | Y  | 스킬    |
| `importance`           | enum(`PREFERENCE`, `ESSENTIAL`) | Y  | 우대/필수 |
| `created_at`           | timestamp                       | Y  | 생성 시각 |
| `modified_at`          | timestamp                       | Y  | 수정 시각 |

현재 역할은 아래와 같다.

- `proposal_position`별 실제 요구 스킬 저장
- 추천 엔진이 읽는 현재 normalized requirement source
- 매칭 단위별 필수/우대 스킬 표현

해석 기준은 아래와 같다.

- 이 테이블의 FK 기준은 `proposal_position.id`다.
- 따라서 이름이 비슷하더라도 직무 마스터 `position`의 기본 스킬 템플릿으로 해석하지 않는다.
- 한 모집 단위 안에서 같은 스킬을 중복 저장하지 않도록 `UNIQUE (proposal_position_id, skill_id)`를 강제한다.
- `importance`는 null 저장을 허용하지 않으며, 입력이 비어 있으면 기본값 `PREFERENCE`로 보정한다.

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
- 현재 구현에서는 `total_budget_*`를 직접 입력하지 않고 포지션별 인원과 최소/최대 예산 합산값으로 계산해 저장한다.
- 현재 ERD 기준으로 추천과 모집 시작 상태는 `MATCHING`이다.
- `COMPLETE`는 제안서 단위의 종료 상태다.

### 5.10 proposal_positions

실제 모집 단위다.

| 필드                | 타입                             | 필수 | 설명          |
|-------------------|--------------------------------|----|-------------|
| `id`              | bigint                         | Y  | PK          |
| `proposal_id`     | bigint                         | Y  | 상위 제안서      |
| `position_id`     | bigint                         | Y  | 직무 마스터      |
| `title`           | varchar(200)                   | Y  | 포지션 제목      |
| `work_type`       | enum(`SITE`, `REMOTE`, `HYBRID`) | N  | 근무 형태      |
| `head_count`      | bigint                         | N  | 모집 인원       |
| `status`          | enum(`OPEN`, `FULL`, `CLOSED`) | Y  | 모집 상태       |
| `unit_budget_min` | bigint                         | N  | 1인 기준 최소 예산 |
| `unit_budget_max` | bigint                         | N  | 1인 기준 최대 예산 |
| `expected_period` | bigint                         | N  | 포지션 예상 기간(주) |
| `career_min_years`| integer                        | N  | 최소 경력 연차    |
| `career_max_years`| integer                        | N  | 최대 경력 연차    |
| `work_place`      | varchar(255)                   | N  | 근무지          |
| `created_at`      | timestamp                      | Y  | 생성 시각       |
| `modified_at`     | timestamp                      | Y  | 수정 시각       |
| `created_by`      | varchar                        | N  | 감사 정보       |
| `modified_by`     | varchar                        | N  | 감사 정보       |

규칙은 아래와 같다.

- `title`은 필수다.
- `head_count`는 현재 ERD에서는 nullable이지만, 서비스 계층에서는 모집 시작 전 양수 검증을 둔다.
- `work_type = REMOTE`이면 `work_place`를 비워야 하고, `SITE`, `HYBRID`이면 `work_place`가 필요하다.
- 예산이 둘 다 존재하면 `unit_budget_min <= unit_budget_max`여야 한다.
- `expected_period`가 존재하면 양수여야 하고, 제안서 전체 예상 기간이 있으면 그 값을 넘길 수 없다.
- 경력 범위가 둘 다 존재하면 `career_min_years <= career_max_years`여야 한다.
- `FULL`은 현재 ERD에서는 파생 상태가 아니라 저장 상태다.
- 현재 문서에서는 `ACCEPTED`, `IN_PROGRESS` 상태 매칭만 정원을 점유하는 것으로 해석한다.
- 현재 문서에서는 `PROPOSED` 상태 매칭은 정원을 점유하지 않는다.
- `head_count`가 존재하고 정원 점유 매칭 수가 `head_count`에 도달하면 `FULL`로 전이한다.
- 수락된 매칭 취소, 반려, 정원 변경으로 여석이 다시 생기면 명시적 재계산 후 `OPEN`으로 되돌릴 수 있다.
- `CLOSED`이면 신규 요청과 지원을 받을 수 없다.
- 같은 제안서 안에서도 같은 직무 마스터를 여러 번 사용할 수 있고, 실제 구분은 `title`로 한다.

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

- `UNIQUE (resume_id, skill_id)`를 강제한다.
- 추천 대상은 `publicly_visible = true`인 이력서만 포함한다.
- AI 추천 대상은 `ai_matching_enabled = true`인 이력서만 포함한다.
- 직접 지원은 `ai_matching_enabled = false`여도 허용할 수 있다.

### 5.12 matchings

요청, 수락, 진행, 완료를 하나의 상태 필드로 다루는 핵심 집합이다.

| 필드                     | 타입                                                                               | 필수 | 설명                       |
|------------------------|----------------------------------------------------------------------------------|----|--------------------------|
| `id`                   | bigint                                                                           | Y  | PK                       |
| `proposal_position_id` | bigint                                                                           | Y  | 대상 모집 단위                 |
| `resume_id`            | bigint                                                                           | Y  | 대상 이력서                   |
| `client_member_id`     | bigint                                                                           | Y  | 제안서를 소유한 클라이언트 회원         |
| `freelancer_member_id` | bigint                                                                           | Y  | 이력서를 소유한 프리랜서 회원         |
| `status`               | enum(`PROPOSED`, `ACCEPTED`, `REJECTED`, `IN_PROGRESS`, `COMPLETED`, `CANCELED`) | Y  | 단일 매칭 상태, 기본값 `PROPOSED` |
| `contract_date`        | timestamp                                                                        | N  | 양측 계약 체결 시각               |
| `complete_date`        | timestamp                                                                        | N  | 완료 요건 충족 시각               |
| `created_at`           | timestamp                                                                        | Y  | 생성 시각                    |
| `modified_at`          | timestamp                                                                        | Y  | 수정 시각                    |

현재 ERD 기준 핵심 규칙은 아래와 같다.

- 활성 매칭은 `PROPOSED`, `ACCEPTED`, `IN_PROGRESS`로 정의한다.
- 종료 매칭은 `REJECTED`, `CANCELED`, `COMPLETED`로 정의한다.
- 한 `proposal_position`과 한 `resume` 조합에는 동시에 하나의 활성 매칭만 허용한다.
- `client_member_id`는 해당 `proposal_position`이 속한 `proposal`의 소유 회원과 같아야 한다.
- `freelancer_member_id`는 해당 `resume`의 소유 회원과 같아야 한다.
- `client_member_id <> freelancer_member_id`를 강제하는 편이 자연스럽다.
- 목록 조회, 권한 검증, 연락처 공개 여부 판단은 `proposal_position -> proposal`, `resume -> member` 역추적 대신 `matching.client_member_id`, `matching.freelancer_member_id`를 우선 기준으로 삼는다.
- 현재 ERD는 `initiator_type`을 저장하지 않으므로, 클라이언트 요청인지 프리랜서 지원인지는 API나 감사 로그에서 구분해야 한다.
- 현재 ERD는 `participation_status`를 별도로 두지 않으므로 요청 처리와 진행 상태를 하나의 `status`가 모두 담당한다.
- 연락처 공개 시점은 `ACCEPTED` 이후로 해석하는 것이 자연스럽다.
- `ACCEPTED`는 상호 수락 완료 상태지만 아직 계약 체결 완료 상태는 아니다.
- `ACCEPTED -> IN_PROGRESS` 전이는 양측이 각각 계약서를 업로드했을 때만 허용한다.
- `contract_date`는 양측 계약서가 모두 올라와 계약이 체결된 시각으로 사용한다.
- `IN_PROGRESS -> COMPLETED` 전이는 양측이 각각 완료 증빙 서류를 업로드하고 상대방 리뷰를 작성했을 때만 허용한다.
- `complete_date`는 완료 증빙과 리뷰 조건이 모두 충족되어 완료 처리된 시각으로 사용한다.
- 계약서/완료 증빙은 `matching_attachments`, 리뷰는 `matching_reviews`로 저장한다.
- 활성 매칭 중복 방지는 partial unique index 또는 동일 트랜잭션 내 재검증으로 강제한다.

### 5.13 matching_attachments

매칭 단위 계약서/완료 증빙 첨부 집합이다.

| 필드                | 타입                                             | 필수 | 설명               |
|-------------------|------------------------------------------------|----|------------------|
| `id`              | bigint                                         | Y  | PK               |
| `matching_id`     | bigint                                         | Y  | 대상 매칭            |
| `member_id`       | bigint                                         | Y  | 첨부 파일 당사자 회원     |
| `file_id`         | bigint                                         | Y  | 파일 메타데이터         |
| `attachment_type` | enum(`CONTRACT`, `COMPLETION_EVIDENCE`)        | Y  | 첨부 타입            |
| `created_at`      | timestamp                                      | Y  | 생성 시각            |
| `modified_at`     | timestamp                                      | Y  | 수정 시각            |

규칙은 아래와 같다.

- 계약서와 완료 증빙은 별도 테이블로 분리하지 않고 `matching_attachments` 공통 집합으로 관리한다.
- `member_id`는 단순 업로더가 아니라 해당 계약서/증빙의 당사자 회원을 뜻한다.
- `member_id`는 해당 `matching.client_member_id` 또는 `matching.freelancer_member_id` 중 하나여야 한다.
- 파일 FK 컬럼명은 다른 자산 테이블과 맞춰 `file_id`를 사용한다.
- `attachment_type`은 `CONTRACT`, `COMPLETION_EVIDENCE`만 허용한다.
- `UNIQUE (matching_id, member_id, attachment_type)`를 강제한다.
- MVP에서는 회원당 타입별 첨부 파일을 1개만 허용한다.
- `attachment_type = CONTRACT`인 첨부가 양측 모두 존재해야 `ACCEPTED -> IN_PROGRESS` 전이를 허용한다.
- `attachment_type = COMPLETION_EVIDENCE`인 첨부가 양측 모두 존재하고 상호 리뷰까지 완료돼야 `IN_PROGRESS -> COMPLETED` 전이를 허용한다.
- 추후 참여자별 완료 증빙 다중 파일이 필요해지면 제출 묶음 엔티티를 추가로 검토한다.

### 5.14 matching_reviews

매칭 단위 상호 리뷰 집합이다.

| 필드                   | 타입                                                                | 필수 | 설명                    |
|----------------------|-------------------------------------------------------------------|----|-----------------------|
| `id`                 | bigint                                                            | Y  | PK                    |
| `matching_id`        | bigint                                                            | Y  | 대상 매칭                 |
| `reviewer_member_id` | bigint                                                            | Y  | 리뷰 작성자 회원             |
| `reviewee_member_id` | bigint                                                            | Y  | 리뷰 대상자 회원             |
| `direction`          | enum(`CLIENT_TO_FREELANCER`, `FREELANCER_TO_CLIENT`)              | Y  | 리뷰 방향                 |
| `rating`             | numeric                                                           | Y  | 평점                    |
| `comment`            | text                                                              | Y  | 리뷰 코멘트                |
| `created_at`         | timestamp                                                         | Y  | 생성 시각, 제출 시각으로 함께 사용   |
| `modified_at`        | timestamp                                                         | Y  | 수정 시각                 |

규칙은 아래와 같다.

- 리뷰는 독립 집합이 아니라 `matching` 종속 집합으로 둔다.
- 리뷰의 기준 FK는 `matching_id`다. `proposal_id + resume_id` 조합으로 식별하지 않는다.
- `reviewer_member_id`와 `reviewee_member_id`는 모두 유지한다.
- 역할은 `reviewer_role`, `reviewee_role` 2개로 중복 저장하지 않고 `direction` 하나로 표현한다.
- `UNIQUE (matching_id, direction)`를 강제한다.
- `UNIQUE (matching_id, reviewer_member_id)`를 강제한다.
- `reviewer_member_id <> reviewee_member_id`를 강제한다.
- 리뷰 생성 시 작성자, 대상자, 방향은 `matching.client_member_id`, `matching.freelancer_member_id`, 로그인 회원 기준으로 서버가 계산한다.
- 리뷰 작성 화면 접근 시에도 `matching.client_member_id`, `matching.freelancer_member_id`, 로그인 회원 기준으로 작성 가능 여부, 이미 작성했는지 여부, 상대방 정보를 계산한다.
- 클라이언트가 hidden field로 작성자/대상자 값을 보내더라도 저장 시 신뢰하지 않는다.
- 리뷰 수정은 로그인 회원과 `reviewer_member_id`가 같은 경우에만 허용한다.
- 리뷰 작성은 `matching.status = IN_PROGRESS`일 때만 시작할 수 있다.
- 별도 초안 상태가 없다면 `created_at`을 제출 시각으로 해석하고 `submitted_at`은 두지 않는다.

### 5.15 상태별 허용 액션 매트릭스

현재 ERD 기준으로 구현할 때 해석하기 쉬운 최소 액션 매트릭스는 아래와 같다.

| 집합                  | 상태                     | 허용 액션                   | 비고            |
|---------------------|------------------------|-------------------------|---------------|
| `proposal`          | `WRITING`              | 생성, 수정, 포지션 편집          | 추천/매칭 진입 전 상태 |
| `proposal`          | `MATCHING`             | 조회, 추천 조회, 조건부 수정, 매칭 진행, 완료 처리 | 매칭 이력 상태에 따라 원본 수정/복제/차단 |
| `proposal`          | `COMPLETE`             | 조회                      | 신규 모집 종료 상태   |
| `proposal_position` | `OPEN`                 | 신규 요청/지원 생성             | 정원 미충족 전제     |
| `proposal_position` | `FULL`                 | 조회, 기존 매칭 관리            | 신규 요청/지원 차단   |
| `proposal_position` | `CLOSED`               | 조회                      | 신규 요청/지원 차단   |
| `matching`          | `PROPOSED`             | 수락, 거절, 취소              | 연락처 비공개       |
| `matching`          | `ACCEPTED`             | 계약서 업로드, 진행 시작 준비         | 연락처 공개 가능, 계약 미체결 가능 |
| `matching`          | `IN_PROGRESS`          | 진행 관리, 완료 증빙 업로드, 리뷰 작성 | 계약 체결 후 진행 중 상태 |
| `matching`          | `COMPLETED`            | 조회                      | 양측 증빙/리뷰 완료 상태 |
| `matching`          | `REJECTED`, `CANCELED` | 조회                      | 종료 상태         |

이 매트릭스는 현재 ERD에서 직접 드러나지 않는 운영 해석을 정리한 것이다.
코드 구현 전 서비스 정책과 다르면 여기부터 먼저 맞추는 편이 안전하다.

아래 `5.16 ~ 5.18`은 현재 ERD에 함께 반영하는 추천 도메인 집합이다.

### 5.16 recommendation_runs

추천 실행 1회를 저장하는 루트다.
MVP에서는 캐시 키와 실행 이력을 함께 담당한다.

| 필드                     | 타입                                                   | 필수 | 설명                    |
|------------------------|------------------------------------------------------|----|-----------------------|
| `id`                   | bigint                                               | Y  | PK                    |
| `proposal_position_id` | bigint                                               | Y  | 추천 대상 모집 단위           |
| `request_fingerprint`  | varchar(128)                                         | Y  | 추천 입력 해시              |
| `algorithm_version`    | varchar(50)                                          | Y  | 추천 알고리즘 버전            |
| `candidate_count`      | integer                                              | Y  | 하드 필터 통과 후 점수 계산 대상 수 |
| `top_k`                | integer                                              | Y  | 저장 결과 개수, MVP 기본값 3    |
| `hard_filter_stats`    | jsonb                                                | N  | 필터별 통과/탈락 집계          |
| `status`               | enum(`PENDING`, `RUNNING`, `COMPUTED`, `FAILED`)     | Y  | 실행 상태                 |
| `error_message`        | text                                                 | N  | 실패 사유                 |
| `created_at`           | timestamp                                            | Y  | 생성 시각                 |
| `modified_at`          | timestamp                                            | Y  | 수정 시각                 |
| `created_by`           | varchar                                              | N  | 감사 정보                 |
| `modified_by`          | varchar                                              | N  | 감사 정보                 |

규칙은 아래와 같다.

- `UNIQUE (proposal_position_id, request_fingerprint, algorithm_version)`를 강제한다.
- 같은 `proposal_position`, 같은 fingerprint, 같은 알고리즘 버전이면 기존 실행 결과를 재사용한다.
- `request_fingerprint`는 최소한 `proposal`, `proposal_position`, `proposal_position_skills`, 추천 파라미터를 반영해야 한다.
- MVP에서는 `created_at`을 추천 실행 시작 시각, `modified_at`을 최종 상태 반영 시각으로 사용하고 별도 `started_at`, `finished_at` 컬럼은 두지 않는다.
- MVP에서는 `input_snapshot_json`, `stale` 플래그, 프롬프트 버전 컬럼을 두지 않고 캐시 키와 재계산 규칙으로 단순화한다.
- `resumes`, `resume_skills`, `publicly_visible`, `ai_matching_enabled` 변경 시 해당 이력서가 포함된 기존 추천 실행은 재사용하지 않고 새로 계산한다.

### 5.17 recommendation_results

추천 실행 안에서 실제 Top K 결과를 저장하는 테이블이다.
MVP에서는 전체 후보 랭킹이 아니라 응답에 노출할 결과만 저장한다.

| 필드                      | 타입                                           | 필수 | 설명                 |
|-------------------------|----------------------------------------------|----|--------------------|
| `id`                   | bigint                                       | Y  | PK                 |
| `recommendation_run_id` | bigint                                       | Y  | 상위 추천 실행           |
| `resume_id`             | bigint                                       | Y  | 추천된 이력서            |
| `rank`                  | integer                                      | Y  | 추천 순위              |
| `final_score`           | numeric(5,4)                                 | Y  | 최종 점수              |
| `embedding_score`       | numeric(5,4)                                 | Y  | 임베딩 유사도 점수         |
| `reason_facts`          | jsonb                                        | Y  | 추천 근거 구조화 데이터      |
| `llm_reason`            | text                                         | N  | 사용자 노출용 추천 설명       |
| `llm_status`            | enum(`PENDING`, `READY`, `FAILED`)           | Y  | 설명 생성 상태           |
| `created_at`            | timestamp                                    | Y  | 생성 시각, 최초 결과 저장 시각  |
| `modified_at`           | timestamp                                    | Y  | 수정 시각, 설명 상태 반영 시각  |
| `created_by`            | varchar                                      | N  | 감사 정보              |
| `modified_by`           | varchar                                      | N  | 감사 정보              |

규칙은 아래와 같다.

- `UNIQUE (recommendation_run_id, resume_id)`를 강제한다.
- `UNIQUE (recommendation_run_id, rank)`를 강제한다.
- 추천 설명은 `resume`에 저장하지 않고 `recommendation_result.llm_reason`에 저장한다.
- `llm_status = READY`이면 `llm_reason`가 존재해야 한다.
- MVP에서는 Top 3 점수 계산 직후 `recommendation_results`를 저장하고, `created_at`을 최초 결과 저장 시각으로 사용한다.
- `llm_status = READY` 또는 `FAILED`로 바뀌는 시점의 설명 생성 결과 반영 시각은 `modified_at`으로 해석하고, 별도 `llm_generated_at` 컬럼은 두지 않는다.
- `reason_facts`에는 최소한 필수 스킬 충족 여부, 우대 스킬 겹침, 근무 형태 호환성, 경력 적합 해석, 임베딩 유사도가 포함돼야 한다.
- MVP에서는 `skill_score`, `work_type_score`, `career_score`, `llm_model`, `prompt_version` 컬럼을 별도로 두지 않고 `reason_facts`에 포함해 관리한다.
- MVP에서는 외부 MQ 없이 결과 row 생성 후 동일 row의 `llm_status`, `llm_reason`, `modified_at` 업데이트로 설명 생성 흐름을 관리한다.

### 5.18 resume_embeddings

추천에 사용하는 이력서 임베딩 캐시다.
MVP에서는 반복 비용이 큰 이력서 쪽만 영속화하고, `proposal_position` 임베딩은 요청 시 계산한다.

| 필드                | 타입          | 필수 | 설명         |
|-------------------|-------------|----|------------|
| `id`              | bigint      | Y  | PK         |
| `resume_id`       | bigint      | Y  | 대상 이력서     |
| `source_hash`     | varchar(128)| Y  | 임베딩 원문 해시  |
| `embedding_model` | varchar(100)| Y  | 임베딩 모델     |
| `embedding_vector`| jsonb       | Y  | 임베딩 벡터 값   |
| `created_at`      | timestamp   | Y  | 생성 시각      |
| `modified_at`     | timestamp   | Y  | 수정 시각      |
| `created_by`      | varchar     | N  | 감사 정보      |
| `modified_by`     | varchar     | N  | 감사 정보      |

규칙은 아래와 같다.

- MVP에서는 임베딩 모델을 하나만 사용하더라도 향후 전환 비용을 줄이기 위해 `UNIQUE (resume_id, embedding_model)`를 강제한다.
- MVP에서는 `resume_id + embedding_model` 기준 최신 임베딩 캐시 한 줄만 유지하고, `created_at`은 캐시 레코드 최초 생성 시각, `modified_at`은 마지막 임베딩 재생성 시각으로 사용한다. 별도 `generated_at` 컬럼은 두지 않는다.
- `source_hash`는 `introduction`, `career`, `preferred_work_type`, `portfolio_url`, `resume_skills`를 기준으로 계산한다.
- 현재 source hash와 저장된 hash가 다르면 임베딩을 재생성한다.
- MVP에서는 `embedding_vector`를 `jsonb`로 저장하고, `pgvector` extension과 ANN 인덱스는 도입하지 않는다.
- `proposal_position_embeddings` 테이블은 MVP에서 만들지 않는다. 현재는 `proposal_position` 입력을 요청 시 임베딩하고, 최종 추천 결과만 `recommendation_runs`와 `recommendation_results`로 재사용한다.

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

- 추천만 받은 `MATCHING` 제안서는 추천 이력을 삭제하고 원본을 `WRITING`으로 되돌린 뒤 수정한다.
- `REJECTED`, `CANCELED`만 남아 있는 제안서는 원본 대신 새 `WRITING` 복제본을 만들어 수정한다.
- `PROPOSED`, `ACCEPTED`, `IN_PROGRESS`, `COMPLETED` 매칭이 하나라도 있으면 수정하지 않는다.

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
- 명시적 시작 승인, 종료 승인 버튼 개념은 현재 ERD에 없다.
- 대신 운영 규칙상 `ACCEPTED -> IN_PROGRESS`는 양측 계약서 업로드 완료를, `IN_PROGRESS -> COMPLETED`는 양측 완료 증빙 업로드와 상호 리뷰 작성을 게이트로 둔다.
- 구현 시에는 이를 `matching_attachments`, `matching_reviews` 확장으로 반영해야 한다.

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
- `resume_skills.skill_id`
- `resume_skills.proficiency_level`
- `career` JSON의 상세 경력

### 제안서 쪽

- `proposal.description`
- `proposal.expected_period`
- `proposal_position.position_id`
- `proposal_position.title`
- `proposal_position.work_type`
- `proposal_position.head_count`
- `proposal_position.unit_budget_min`
- `proposal_position.unit_budget_max`
- `proposal_position.expected_period`
- `proposal_position.career_min_years`
- `proposal_position.career_max_years`
- `proposal_position.work_place`
- `proposal_position_skills.skill_id`
- `proposal_position_skills.importance`

현재 모델의 한계는 아래와 같다.

- `proposal_position_skills`는 현재 스킬 ID와 중요도만 가지므로 요구 이유나 숙련도 조건을 담기 어렵다.
- 직무 마스터 `position` 자체에는 기본 스킬 템플릿이 없어서 생성 보조 시 재사용성이 약하다.
- AI 브리프 응답 스키마는 아직 확장된 `proposal_position` 상세 필드를 모두 채우지 못한다.

## 8. 핵심 운영 규칙

현재 모델에서 최소한으로 지켜야 할 규칙은 아래와 같다.

1. `member.status = ACTIVE`인 회원만 클라이언트로서 제안서를 작성할 수 있다.
2. `resume`가 존재하고 `resume.status = ACTIVE`인 회원만 프리랜서로서 직접 지원과 매칭 흐름에 참여할 수 있다.
3. `resume.writing_status = WRITING`이면 검색/목록/추천 노출 대상에서 제외한다.
4. `resume.publicly_visible = false`면 검색과 추천 결과에서 제외한다.
5. `resume.ai_matching_enabled = false`면 AI 추천 결과에서 제외하지만 직접 지원은 허용한다.
6. `proposal_position.status = FULL` 또는 `CLOSED`이면 신규 매칭 생성을 막아야 한다.
7. `proposal_position` 정원은 `matching.status in (ACCEPTED, IN_PROGRESS)`만 점유하고, `PROPOSED`는 정원을 점유하지 않는다.
8. 연락처는 `matching.status = ACCEPTED` 이전에는 공개하지 않는다.
9. 전체 예산과 포지션 예산은 의미가 다르므로 별도로 유지한다.
10. 현재 ERD에는 `proposal_attachments`가 없으므로, 제안서 파일은 저장 경로가 있더라도 아직 정식 도메인 연관 자산이 아니다.
11. 한 `proposal_position + resume` 조합에는 동시에 하나의 활성 매칭만 존재해야 한다.
12. `matching.status = ACCEPTED`는 상호 수락 완료 상태지만 아직 계약 체결 완료 상태는 아니다.
13. `ACCEPTED -> IN_PROGRESS` 전이는 양측 계약서 업로드가 모두 완료됐을 때만 허용한다.
14. `contract_date`는 수락 시각이 아니라 양측 계약서가 모두 업로드되어 계약이 체결된 시각으로 기록한다.
15. `IN_PROGRESS -> COMPLETED` 전이는 양측 완료 증빙 서류 업로드와 상호 리뷰 작성이 모두 끝났을 때만 허용한다.
16. `complete_date`는 완료 승인 버튼 클릭 시각이 아니라 완료 증빙과 리뷰 조건이 모두 충족된 시각으로 기록한다.
17. 리뷰는 `matching` 종속 집합으로 두고 FK는 `matching_id`를 사용한다.
18. 리뷰 생성 시 작성자, 대상자, 방향은 `matching.client_member_id`, `matching.freelancer_member_id`, 로그인 회원으로 서버가 결정한다.
19. 리뷰 수정은 로그인 회원과 `reviewer_member_id`가 같은 경우에만 허용한다.
20. `created_at`은 리뷰 제출 시각으로 함께 사용하고 별도 `submitted_at`은 두지 않는다.
21. 매칭 단위 계약서/완료 증빙은 `matching_attachments(matching_id, member_id, file_id, attachment_type)`로 저장한다.
22. `matching_attachments`는 `UNIQUE (matching_id, member_id, attachment_type)`를 강제한다.

## 9. 구현 전 확인 체크리스트

실제 엔티티/서비스를 만들기 전에 아래 항목을 한 번 더 확인한다.

1. `members.memo`를 내부 전용 필드로만 둘지
2. `resumes.career`를 nullable로 둘지 현재 코드처럼 필수로 둘지
3. `proposal.status = MATCHING` 진입 최소 조건
4. `proposal_position.status` 재계산을 서비스에서 수동으로 할지 배치/트리거성 로직으로 둘지
5. `matching.status`의 정확한 enum literal이 ERDCloud 원본과 일치하는지
6. 활성 매칭 중복 방지를 DB partial unique index로 강제할지 서비스 계층 락으로 강제할지
7. `matching_attachments`에서 완료 증빙을 참여자당 1개로 고정할지, 다중 파일 제출 묶음으로 확장할지
8. 현재 인증 principal에 `memberId`를 직접 포함할지, `email` 기반 회원 재조회로 해결할지
9. 리뷰 평점 스케일과 완료 이후 수정 허용 범위를 어디까지 둘지
10. `contract_date`, `complete_date`를 각각 계약 체결/완료 조건 충족 시각으로 일관되게 사용할지

## 10. 추후 재논의 항목

아래 항목은 이전 대화에서 한 번 논의됐고, 일부는 현재 문서에 방향만 반영돼 있다.
현재 문서에서는 "미래 확장 후보" 또는 "추가 정교화 포인트"로 유지한다.

### 10.1 AI 브리프 응답 스키마 정렬

- 현재 `proposal_position`은 `title`, `work_type`, `expected_period`, `career_min_years`, `career_max_years`, `work_place`를 가진다.
- 하지만 AI 브리프 응답과 매퍼는 아직 이 상세 필드를 모두 안정적으로 채우지 못한다.
- 따라서 AI 브리프 스키마와 매퍼를 확장된 `proposal_position` 구조에 맞추는 후속 작업이 필요하다.

### 10.2 position 기본 스킬 템플릿

현재 요구 스킬 source는 `proposal_position_skills`다.
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

### 10.5 matching_attachments 확장 가능성

현재는 아래 구조로 정리했다.

- `matching_attachments(matching_id, member_id, file_id, attachment_type)`를 사용한다.
- `member_id`는 업로더가 아니라 해당 계약서/증빙의 당사자 회원으로 해석한다.
- `attachment_type`은 `CONTRACT`, `COMPLETION_EVIDENCE`를 사용한다.
- `UNIQUE (matching_id, member_id, attachment_type)`를 전제로 한다.
- 리뷰는 `matching_reviews(matching_id, reviewer_member_id, reviewee_member_id, direction)`로 유지한다.

추후 참여자별 완료 증빙을 여러 파일로 관리해야 하면, `matching_attachments`의 유니크 제약을 깨는 대신 제출 묶음 엔티티를 추가하는 방향으로 재논의한다.
