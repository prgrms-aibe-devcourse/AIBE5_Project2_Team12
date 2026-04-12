# IT-da 결정 로그

이 문서는 팀 논의에서 이미 합의된 내용과 아직 보류한 내용을 빠르게 확인하기 위한 결정 로그다.
상세 필드 정의는 [domain-spec.md](./domain-spec.md), 구현 순서는 [implementation-backlog.md](./implementation-backlog.md)를 기준으로 한다.

## 1. 현재 확정 결정

### 서비스 역할

- `UserRole`은 권한 축으로만 사용한다.
- 모든 활성 회원은 기본적으로 클라이언트 기능을 사용할 수 있다.
- `resume`가 존재하고 `resume.status = ACTIVE`이면 프리랜서로서 직접 지원과 매칭 흐름에 참여할 수 있다.
- `Resume.publiclyVisible`은 검색/목록 노출 여부를 제어하고, 추천 노출의 선행 조건에도 포함된다.
- `Resume.writing_status = DONE`이어야 검색/목록/추천 노출 대상이 된다.
- `Resume.aiMatchingEnabled`는 그 전제 조건을 만족한 이력서 중 AI 추천 후보군 포함 여부만 추가로 제어한다.

### 제안서와 모집 단위

- `proposal`은 프로젝트 전체 수준의 문서다.
- `proposal_position`은 실제 모집 단위다.
- `position`은 공용 직무 마스터다.
- `proposal_position_skill`은 실제 모집 단위별 요구 스킬 집합이다.
- 요구 스킬 테이블의 FK 기준은 `proposal_position.id`다.

### AI 브리프 저장 모델

- `proposal.raw_input_text`는 사용자의 원본 자유 입력을 저장한다.
- `proposal.description`은 사용자가 검토하고 수정하는 최종 제안서 본문이다.
- MVP에서는 `proposal.overview`를 두지 않는다.
- 리스트와 카드의 미리보기 텍스트는 별도 `overview`가 아니라 `description` 발췌본으로 처리한다.

### 예산

- `proposal.total_budget_*`는 전체 프로젝트 예산이다.
- `proposal_position.unit_budget_*`는 1인 기준 예산이다.
- 포지션 전체 예산은 `unit_budget * head_count`로 해석한다.

### 상태 모델

- `proposal.status`: `WRITING`, `MATCHING`, `COMPLETE`
- `proposal_position.status`: `OPEN`, `FULL`, `CLOSED`
- `matching.status`: `PROPOSED`, `ACCEPTED`, `REJECTED`, `IN_PROGRESS`, `COMPLETED`, `CANCELED`
- 활성 매칭은 `PROPOSED`, `ACCEPTED`, `IN_PROGRESS`로 본다.
- `proposal_position.status = FULL`은 `matching.status in (ACCEPTED, IN_PROGRESS)` 수가 `head_count`에 도달했음을 의미한다.
- 수락 취소나 완료로 정원 여유가 다시 생기면 `proposal_position.status`는 `OPEN`으로 재계산할 수 있다.

### 매칭/프로젝트 모델

- MVP에서는 별도 `project` 테이블을 두지 않는다.
- 참여 이력과 진행 흐름은 `matching.status` 단일 필드로 관리한다.
- 연락처는 `matching.status = ACCEPTED` 이후에만 공개한다.
- `matching.contract_date`는 `ACCEPTED` 전이 시각, `matching.complete_date`는 `COMPLETED` 전이 시각으로 해석한다.
- 동일 `proposal_position + resume` 조합에는 동시에 하나의 활성 매칭만 둔다.

### 파일 자산

- 현재 애플리케이션에는 `/files/profile/**`, `/files/resume/**`, `/files/proposal/**` 저장 경로가 있다.
- 다만 ERD에 `proposal_attachments`가 없으므로 제안서 파일은 아직 정식 도메인 연관 자산으로 취급하지 않는다.

## 2. 현재 보류 결정

- `proposal_position` 상세 필드 확장 여부
- 제안서 파일을 정식 도메인 자산으로 승격할지 여부
- `matching.initiator_type`, `requested_by_member_id` 추가 여부
- 시작/종료 양측 승인 모델 도입 여부
- `MATCHING` 상태 제안서 수정 정책

## 3. 구현 시 해석 원칙

- ERD에 없는 확장 설계는 현재 코드 대상으로 보지 않는다.
- 문서와 ERD가 충돌하면 우선 현재 ERD 기준으로 맞춘다.
- 단, 코드에 이미 구현된 제약과 다르면 `domain-spec.md`의 "현재 코드 기준 제약"에 차이를 명시한다.
- 추천 엔진은 `proposal_position_skill`을 정규화된 요구 스킬 source로 사용한다.

## 4. 지금 팀이 자주 확인해야 할 질문

### Q. 클라이언트와 프리랜서를 동시에 할 수 있나?

가능하다. 다만 프리랜서 기능은 `resume` 존재만으로 충분하지 않고, 직접 지원/매칭 참여는 `resume.status = ACTIVE`, 검색/목록 노출은 `writing_status`와 `publiclyVisible`, AI 추천 노출은 여기에 `aiMatchingEnabled`를 추가로 본다.

### Q. AI 추천을 끄면 직접 지원도 막히나?

아니다. `aiMatchingEnabled`는 AI 추천 후보군 제외만 의미한다.

### Q. `FULL`은 언제 되나?

`PROPOSED`는 정원을 차지하지 않고, `ACCEPTED` 또는 `IN_PROGRESS` 매칭 수가 `head_count`에 도달했을 때만 `FULL`이다.

### Q. 제안서 파일은 지금 도메인에서 어디까지 지원하나?

저장 경로는 이미 있지만, 현재 MVP 기준으로는 정식 `proposal_attachments` 모델 없이 임시 업로드 또는 UI 보조 자산으로만 해석한다.

### Q. 매칭이 성립하면 프로젝트가 생성되나?

현재 MVP 기준으로는 아니다. 별도 `project` 테이블 없이 `matching`이 진행 이력을 관리한다.

### Q. 포지션별 요구 스킬을 따로 저장하나?

그렇다. 현재 모델의 요구 스킬 source는 `proposal_position_skill`이다.
