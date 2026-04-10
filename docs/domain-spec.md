# IT-da 도메인 상세 명세

이 문서는 현재 기획 문서와 ERD 초안을 기준으로 `proposal`, `proposal_position`, `position`, `matching` 도메인을 구현 가능한 수준으로 구체화한 명세다.
현재 코드베이스에 이미 존재하는 `member`, `resume`, `skill`, `stored_file`을 전제로 작성했다.

## 1. 범위

이번 명세의 범위는 아래와 같다.

- 클라이언트가 작성하는 프로젝트 제안서
- 제안서 내부의 실제 모집 단위
- 재사용 가능한 직무 마스터
- 요청, 지원, 수락, 거절, 참여 이력을 표현하는 매칭
- 추천 엔진이 읽는 최소 입력 구조

이번 명세의 비범위는 아래와 같다.

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

따라서 신규 도메인은 현재 패턴을 깨지 않는 선에서 추가하는 것이 우선이다.

## 3. 확정 결정

현재 논의 기준으로 이번 문서에서 확정하는 결정은 아래와 같다.

- `position`은 직무 마스터 테이블이다.
- `position`은 회원이 소유하지 않는다.
- `position`은 MVP에서 `id`, `name` 중심의 얇은 마스터로 유지한다.
- `proposal`은 프로젝트 전체 수준의 문서다.
- `proposal_position`은 실제 모집 단위다.
- `matching`은 항상 `proposal_position`을 기준으로 생성된다.
- `proposal`의 예산과 `proposal_position`의 예산은 의미가 다르므로 둘 다 유지할 수 있다.
- `proposal.total_budget_*`는 전체 프로젝트 예산, `proposal_position.unit_budget_*`는 1인 기준 예산을 의미한다.
- `proposal_position.status = FULL`은 저장 상태가 아니라 파생 결과로 본다.
- MVP에서는 별도 `project` 테이블을 두지 않는다.
- MVP에서는 `클라이언트 / 프리랜서` 서비스 역할을 별도 컬럼으로 저장하지 않는다.
- 모든 활성 회원은 기본적으로 클라이언트 기능을 사용할 수 있다.
- `Resume`가 존재하면 프리랜서 기능을 사용할 수 있다.
- AI 추천 노출 허용 여부는 `Resume.aiMatchingEnabled`로 제어한다.
- `Resume.aiMatchingEnabled`는 AI 추천 후보군 포함 여부만 의미하고, 직접 지원 허용 여부와는 분리한다.
- 매칭 성립 이후의 이력은 `matching.participation_status`로 관리한다.
- `matching`의 요청/수락 상태와 참여 이력 상태는 분리한다.

## 4. 권장 집합 경계

### 4.1 Member / Resume

- `Member`는 계정 루트다.
- `Resume`는 프리랜서 프로필 루트다.
- 추천 엔진은 `Resume`와 그 하위 스킬/경력 정보를 읽는다.
- 모든 활성 회원은 클라이언트로서 제안서를 작성할 수 있다.
- `Resume`가 생성된 회원은 프리랜서로서 추천, 지원, 매칭 흐름에 참여할 수 있다.

현재 도메인에서는 서비스 역할을 별도 필드로 저장하지 않고, 아래 규칙으로 파생시킨다.

- `canActAsClient(member) = member.status == ACTIVE`
- `canActAsFreelancer(member) = resume exists`
- `canBeRecommended(member) = resume exists && resume.publiclyVisible && resume.aiMatchingEnabled`

따라서 `UserRole`은 권한 관리 축으로만 두고, 서비스 역할은 `Resume` 존재 여부와 공개 설정으로 해석한다.
또한 공개 설정의 의미는 아래처럼 나눈다.

- `resume.publiclyVisible = true`: 검색, 목록, 추천 화면 같은 발견 가능 영역에 노출 가능
- `resume.aiMatchingEnabled = true`: AI 추천 후보군 계산 대상에 포함 가능
- `resume.aiMatchingEnabled = false`: AI 추천에서는 제외하지만 직접 지원은 허용 가능

### 4.2 Proposal

- 제안서 초안과 제출 상태를 관리하는 집합 루트
- 클라이언트가 소유
- AI 브리프의 입력/출력 결과를 보관
- 전체 프로젝트 수준의 제목, 설명, 전체 예산을 가짐

### 4.3 ProposalPosition

- 하나의 제안서 내부에서 실제로 채용 또는 모집되는 단위
- 매칭과 추천의 기준점
- 직무 마스터를 참조하지만, 실제 요구조건은 별도 보관
- 여러 명 채용을 위해 `headcount`를 가짐

### 4.4 Matching

- 클라이언트 요청과 프리랜서 지원을 하나의 흐름으로 표현하는 집합 루트
- 대상은 항상 `proposal_position`과 `resume`의 조합
- 요청 상태와 참여 이력 상태를 별도 필드로 관리

### 4.5 Project

- MVP에서는 별도 집합으로 두지 않는다.
- 현재 관리하려는 상태는 "프로젝트 전체 상태"보다 "특정 프리랜서의 참여 상태"에 가깝다.
- 따라서 `matching`이 요청, 수락, 참여 이력을 함께 관리한다.
- 공통 일정, 산출물, 계약, 채팅, 정산처럼 여러 참여자를 묶는 실행 데이터가 생기면 그 시점에 별도 `project` 집합을 추가한다.

## 5. 엔티티 명세

## 5.1 positions

직무 마스터다. 분류 코드 테이블에 가깝다.

권장 필드는 아래와 같다.

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `id` | bigint | Y | PK |
| `name` | varchar(100) | Y | 직무명, 유니크 |
| `created_at` | timestamp | Y | 생성 시각 |
| `modified_at` | timestamp | Y | 수정 시각 |

규칙은 아래와 같다.

- `name`은 trim 후 저장한다.
- `name`은 유니크해야 한다.
- `description`, `status`, `member_id`는 MVP에서 두지 않는다.

## 5.2 proposals

프로젝트 전체 수준의 제안서다.

권장 필드는 아래와 같다.

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `id` | bigint | Y | PK |
| `member_id` | bigint | Y | 제안서 작성 클라이언트 |
| `title` | varchar(200) | Y | 프로젝트 제목 |
| `raw_input_text` | text | Y | 단일 텍스트 입력 원문 |
| `overview` | text | N | 사용자가 정리한 프로젝트 설명 |
| `total_budget_min` | bigint | N | 전체 프로젝트 최소 예산 |
| `total_budget_max` | bigint | N | 전체 프로젝트 최대 예산 |
| `status` | enum(`WRITING`, `DONE`) | Y | 제안서 작성 상태 |
| `created_at` | timestamp | Y | 생성 시각 |
| `modified_at` | timestamp | Y | 수정 시각 |
| `created_by` | varchar | N | 감사 정보 필요 시 |
| `modified_by` | varchar | N | 감사 정보 필요 시 |

규칙은 아래와 같다.

- `member_id`는 클라이언트 계정이어야 한다.
- `total_budget_min`, `total_budget_max`는 둘 다 null일 수 있다.
- 둘 다 존재하면 `total_budget_min <= total_budget_max`여야 한다.
- `DONE` 상태로 제출하려면 최소 1개 이상의 `proposal_position`이 있어야 한다.
- `DONE` 상태의 추천 입력 필드를 수정하면 추천 캐시는 무효화된다.

권장 제출 규칙은 아래와 같다.

- 임시 저장은 `WRITING`
- 추천 시작 기준은 `DONE`
- `DONE`은 "마지막으로 제출된 추천 기준 스냅샷"을 의미한다.
- `DONE` 상태에서도 추천 입력에 영향이 없는 부가 정보 수정은 허용할 수 있다.
- `DONE` 상태에서 추천 입력에 영향이 있는 변경이 발생하면 MVP에서는 다시 `WRITING`으로 되돌리는 쪽이 가장 안전하다.

## 5.3 proposal_positions

실제 추천, 지원, 매칭의 기준이 되는 모집 단위다.

권장 필드는 아래와 같다.

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `id` | bigint | Y | PK |
| `proposal_id` | bigint | Y | 상위 제안서 |
| `position_id` | bigint | Y | 직무 마스터 |
| `title` | varchar(200) | Y | 모집 단위 표시명 |
| `requirement_summary` | text | N | 포지션별 요구사항 요약 |
| `headcount` | int | Y | 모집 인원 |
| `unit_budget_min` | bigint | N | 1인 기준 최소 예산 |
| `unit_budget_max` | bigint | N | 1인 기준 최대 예산 |
| `work_type` | enum(`SITE`, `REMOTE`, `HYBRID`) | Y | 근무 형태 |
| `career_years_min` | smallint | N | 최소 경력 연차 |
| `career_years_max` | smallint | N | 최대 경력 연차 또는 상한 |
| `status` | enum(`OPEN`, `CLOSED`) | Y | 모집 가능 여부 |
| `sort_order` | int | Y | 화면 정렬 순서 |
| `created_at` | timestamp | Y | 생성 시각 |
| `modified_at` | timestamp | Y | 수정 시각 |

규칙은 아래와 같다.

- `headcount >= 1`이어야 한다.
- 예산이 둘 다 존재하면 `unit_budget_min <= unit_budget_max`여야 한다.
- 포지션 전체 예산이 필요하면 `unit_budget_* * headcount`로 계산한다.
- 경력 연차가 둘 다 존재하면 `career_years_min <= career_years_max`여야 한다.
- `status = CLOSED`이면 신규 요청과 지원을 받을 수 없다.
- `FULL`은 저장하지 않는다.
- `accepted`된 매칭 수가 `headcount` 이상이면 화면에서는 `FULL`로 표시한다.
- 같은 `proposal_id` 안에서 같은 `position_id`를 여러 번 둘 수 있다.

마지막 규칙은 중요하다.
예를 들어 같은 프로젝트 안에 `Backend` 포지션이 두 개 있어도, 조건이 다르면 별도 `proposal_position`으로 표현할 수 있어야 한다.
따라서 `UNIQUE (proposal_id, position_id)`는 두지 않는 쪽이 안전하다.

## 5.4 proposal_position_skills

실제 추천 기준이 되는 스킬 요구사항이다.
이번 명세에서 가장 중요한 조인 테이블 중 하나다.

권장 필드는 아래와 같다.

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `id` | bigint | Y | PK |
| `proposal_position_id` | bigint | Y | 모집 단위 |
| `skill_id` | bigint | Y | 요구 스킬 |
| `importance` | enum(`REQUIRED`, `PREFERRED`) | Y | 중요도 |
| `created_at` | timestamp | Y | 생성 시각 |
| `modified_at` | timestamp | Y | 수정 시각 |

규칙은 아래와 같다.

- `UNIQUE (proposal_position_id, skill_id)`를 둔다.
- 추천 엔진의 스킬 매칭은 이 테이블을 기준으로 계산한다.

## 5.5 position_skills

ERD 초안의 `position_skill`은 유지할 수 있지만 역할을 제한해야 한다.

권장 역할은 아래와 같다.

- 직무 마스터의 기본 추천 스킬 템플릿
- AI 브리프 생성 시 초깃값 제공
- 수동 입력 폼 기본값 제공

중요한 점은 아래와 같다.

- `position_skill`만으로 실제 추천을 수행하면 안 된다.
- 실제 추천의 source of truth는 `proposal_position_skills`여야 한다.

즉, `position_skill`은 템플릿이고 `proposal_position_skill`은 실제값이다.

## 5.6 resume_skills

추천 엔진 입력을 위해 `resume_skill`도 필요하다.
현재 코드에는 `resume`과 `skill`은 있지만 아직 연결 테이블이 없다.

권장 필드는 아래와 같다.

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `id` | bigint | Y | PK |
| `resume_id` | bigint | Y | 이력서 |
| `skill_id` | bigint | Y | 보유 스킬 |
| `proficiency_level` | enum(`BEGINNER`, `INTERMEDIATE`, `ADVANCED`) | Y | 숙련도 |
| `created_at` | timestamp | Y | 생성 시각 |
| `modified_at` | timestamp | Y | 수정 시각 |

규칙은 아래와 같다.

- `UNIQUE (resume_id, skill_id)`를 둔다.
- 추천 대상은 `publiclyVisible = true`인 이력서만 포함한다.
- AI 추천 노출 대상은 `aiMatchingEnabled = true`인 이력서만 포함한다.
- 직접 지원은 `aiMatchingEnabled = false`여도 허용할 수 있다.

## 5.7 matchings

지원과 요청을 하나의 흐름으로 다루는 핵심 집합이다.

권장 필드는 아래와 같다.

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `id` | bigint | Y | PK |
| `proposal_position_id` | bigint | Y | 대상 모집 단위 |
| `resume_id` | bigint | Y | 대상 이력서 |
| `initiator_type` | enum(`CLIENT`, `FREELANCER`) | Y | 시작 주체 |
| `requested_by_member_id` | bigint | Y | 요청 또는 지원을 시작한 회원 |
| `status` | enum(`REQUESTED`, `ACCEPTED`, `REJECTED`, `CANCELED`) | Y | 요청 상태 |
| `participation_status` | enum(`DISCUSSING`, `ACTIVE`, `COMPLETED`) | N | 매칭 성립 후 참여 진행 상태 |
| `requested_at` | timestamp | Y | 요청 시각 |
| `responded_at` | timestamp | N | 수락/거절 시각 |
| `accepted_at` | timestamp | N | 수락 시각 |
| `started_at` | timestamp | N | 시작 시각 |
| `completed_at` | timestamp | N | 종료 시각 |
| `canceled_at` | timestamp | N | 취소 시각 |
| `created_at` | timestamp | Y | 생성 시각 |
| `modified_at` | timestamp | Y | 수정 시각 |
| `created_by` | varchar | N | 감사 정보 필요 시 |
| `modified_by` | varchar | N | 감사 정보 필요 시 |

핵심 규칙은 아래와 같다.

- 한 `proposal_position`과 한 `resume` 조합에는 동시에 하나의 활성 매칭만 허용한다.
- `status = REQUESTED`일 때만 응답 가능하다.
- `status = ACCEPTED`일 때만 `participation_status`를 가질 수 있다.
- `participation_status = DISCUSSING`에서 `ACTIVE`, `COMPLETED`로 가는 전이는 양측 동의가 필요하다.
- 연락처 공개 시점은 `status = ACCEPTED` 이후다.
- `accepted`된 매칭 수가 `headcount`에 도달하면 해당 `proposal_position`은 신규 매칭을 막아야 한다.

`participation_status`를 별도 컬럼으로 분리한 이유는 아래와 같다.

- 요청/수락 흐름과 실제 참여 진행 흐름은 성격이 다르다.
- 하나의 `status` 컬럼에 `REQUESTED`, `ACCEPTED`, `IN_PROGRESS`, `COMPLETED`를 모두 넣으면 의미가 섞인다.

## 5.8 profile_image / proposal_attachments / resume_attachments

기존 `StoredFile` 패턴은 유지한다.

권장 방향은 아래와 같다.

- 프로필 이미지는 `member` 기준 1:1에 가깝게 유지
- 이력서 첨부파일은 `resume` 기준 다대다 또는 1:N
- 제안서 첨부파일은 `proposal` 기준 다대다 또는 1:N

현재 파일 저장 서비스는 이미 `proposal` 전용 파일 경로를 지원하므로, 제안서 첨부파일은 빠르게 붙일 수 있다.

## 6. 상태 전이

## 6.1 Proposal 상태

```text
WRITING -> DONE
DONE -> WRITING   (추천 입력에 영향이 있는 변경 시)
```

권장 규칙은 아래와 같다.

- `WRITING`: 자동 저장, 수동 저장 가능
- `DONE`: 마지막으로 제출된 추천 기준이 유효한 상태
- `DONE`에서 추천 입력을 바꾸면 `WRITING`으로 되돌린 뒤 재제출
- `DONE`에서 첨부파일, 표시 순서 같은 비핵심 부가 정보를 바꾸는 것은 유지 가능하다

MVP에서 추천 입력에 영향이 있는 변경은 아래로 본다.

- `proposal.title`
- `proposal.raw_input_text`
- `proposal.overview`
- `proposal.total_budget_min`, `proposal.total_budget_max`
- `proposal_position` 추가 또는 삭제
- `proposal_position.position_id`
- `proposal_position.title`
- `proposal_position.requirement_summary`
- `proposal_position.headcount`
- `proposal_position.unit_budget_min`, `proposal_position.unit_budget_max`
- `proposal_position.work_type`
- `proposal_position.career_years_min`, `proposal_position.career_years_max`
- `proposal_position_skill` 추가, 삭제, 중요도 변경

MVP에서 추천 입력에 영향이 없는 변경의 예시는 아래와 같다.

- 제안서 첨부파일 추가, 삭제
- `proposal_position.sort_order`

## 6.2 ProposalPosition 상태

```text
OPEN -> CLOSED
CLOSED -> OPEN
```

표시용 파생 상태는 아래와 같다.

- `FULL`: 저장값이 아니라 `accepted_count >= headcount`일 때 계산

## 6.3 Matching 상태

```text
REQUESTED -> ACCEPTED
REQUESTED -> REJECTED
REQUESTED -> CANCELED
```

## 6.4 참여 상태

```text
DISCUSSING -> ACTIVE -> COMPLETED
```

이 전이는 `status = ACCEPTED`인 매칭에만 적용된다.

## 7. 핵심 도메인 규칙

이번 도메인에서 반드시 지켜야 하는 규칙은 아래와 같다.

1. 추천 엔진의 실제 요구 스킬은 `proposal_position_skill` 기준이다.
2. 직무 마스터의 스킬 템플릿은 추천의 참고값일 뿐 실제값이 아니다.
3. 연락처는 매칭 수락 이후에만 공개한다.
4. `resume.publiclyVisible = false`면 검색과 추천 결과에서 제외한다.
5. `resume.aiMatchingEnabled = false`면 AI 추천 결과에서 제외하지만 직접 지원은 허용 가능하다.
6. `proposal_position`이 닫혔거나 정원이 찼으면 신규 요청을 막는다.
7. 전체 예산과 포지션 예산은 필드명부터 다르게 둬서 의미를 분리한다.
8. `proposal`의 상태와 `matching`의 상태를 섞지 않는다.
9. MVP에서는 프로젝트 전체 상태를 저장하지 않고 매칭별 참여 이력만 관리한다.
10. 서비스 역할은 별도 필드가 아니라 `Resume` 존재 여부와 공개 플래그로 파생한다.

## 8. 추천 엔진 입력 기준

MVP 추천 엔진이 실제로 읽어야 하는 필드는 아래와 같다.

### 이력서 쪽

- `resume.career_years`
- `resume.preferred_work_type`
- `resume.publicly_visible`
- `resume.ai_matching_enabled`
- `resume_skill.skill_id`
- `resume_skill.proficiency_level`
- `career` JSON의 상세 경력

### 제안서 포지션 쪽

- `proposal_position.position_id`
- `proposal_position.work_type`
- `proposal_position.career_years_min`
- `proposal_position.career_years_max`
- `proposal_position_skill.skill_id`
- `proposal_position_skill.importance`
- `proposal_position.requirement_summary`

이 기준이면 추천 파이프라인은 아래처럼 단순화할 수 있다.

1. 공개 이력서만 필터링
2. 추천 노출 허용 이력서만 AI 추천 대상으로 필터링
3. `work_type`, `career_years`, 필수 스킬로 하드 필터
4. 남은 후보를 점수화
5. Top 3에 대해서만 LLM 설명 생성

## 9. 대안과 선택 이유

## 9.1 `position`이 회원 소유인 모델

채택하지 않는다.

이유는 아래와 같다.

- 현재 논의에서 `position`은 직무 마스터다.
- 회원이 소유하는 순간 공용 기준값이 아니라 사용자 데이터가 된다.
- 같은 직무명에 대한 중복 데이터가 늘어난다.

## 9.2 `position_skill`만 두고 `proposal_position_skill`을 두지 않는 모델

채택하지 않는다.

이유는 아래와 같다.

- 같은 `Backend`라도 프로젝트마다 요구 스택이 다르다.
- 직무 마스터 스킬만으로는 실제 모집 조건을 표현할 수 없다.
- 추천 근거 설명이 부정확해진다.

## 9.3 `proposal_position.status = OPEN / FULL / CLOSED`를 모두 저장하는 모델

권장하지 않는다.

이유는 아래와 같다.

- `FULL`은 headcount와 accepted matching 수로 계산 가능한 상태다.
- 저장 상태로 두면 정합성 유지 비용이 커진다.

## 9.4 `matching.status` 하나에 모든 상태를 넣는 모델

권장하지 않는다.

예시는 아래와 같다.

- `REQUESTED`
- `ACCEPTED`
- `REJECTED`
- `IN_PROGRESS`
- `COMPLETED`

이 모델은 요청 처리와 참여 이력을 섞는다.
MVP에서는 `status`와 `participation_status`를 분리하는 쪽이 더 유지보수에 유리하다.

## 9.5 별도 `project` 테이블을 두는 모델

MVP에서는 채택하지 않는다.

이유는 아래와 같다.

- 현재 요구사항의 핵심은 프로젝트 전체 운영보다 클라이언트와 프리랜서 사이의 참여 이력 관리다.
- `proposal_position + resume` 조합에 매칭과 참여 상태를 함께 두면 필요한 이력을 충분히 표현할 수 있다.
- 지금 시점에 `project`를 도입하면 상태 동기화와 참조 관계만 늘고, 저장해야 할 공통 실행 데이터는 아직 없다.
- 공통 일정, 채팅, 산출물, 계약, 정산처럼 여러 참여자를 묶는 데이터가 생길 때 도입하는 편이 자연스럽다.

## 10. 구현 시 주의할 점

- `proposal_position` 수락 경쟁은 동시성 이슈가 있으므로 수락 시점에 락 또는 정원 재확인이 필요하다.
- `DONE` 제안서의 추천 입력 변경 시 기존 추천 캐시를 반드시 무효화해야 한다.
- `DONE` 제안서의 비핵심 수정은 상태를 유지하더라도 추천 캐시 무효화 대상이 아니어야 한다.
- `proposal_position_skill`, `resume_skill`은 추천 성능과 정확도에 직결되므로 초기에 정규화해야 한다.
- `Resume` 없는 회원은 프리랜서 전용 기능에 진입하지 못하게 화면과 서비스 계층에서 함께 막아야 한다.
- 제안서 첨부파일과 이력서 첨부파일은 기존 `StoredFile` 패턴을 재사용하면 된다.

## 11. 명세 기준 우선순위

이번 명세에서 구현 우선순위가 높은 항목은 아래와 같다.

1. `proposal`, `proposal_position`, `proposal_position_skill`
2. `resume_skill`
3. `matching`
4. `Resume` 존재 기반 프리랜서 진입 규칙 반영
5. AI 브리프와 추천 캐시
