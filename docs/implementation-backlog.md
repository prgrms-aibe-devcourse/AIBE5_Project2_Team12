# IT-da 구현 백로그

이 문서는 [project-overview.md](./project-overview.md)와 [domain-spec.md](./domain-spec.md)를 기준으로 MVP 구현 순서를 정리한 실행 백로그다.
2026-04-19 기준으로는 현재 ERD 초안과 이번에 함께 반영하는 추천 도메인 테이블 기준으로 범위를 다시 정리했다.

## 1. 구현 원칙

- 현재 리포의 Spring Boot + JPA + 세션 인증 패턴을 유지한다.
- 한 번에 AI 기능까지 다 붙이지 않고, 도메인과 상태 모델을 먼저 고정한다.
- 추천 엔진은 처음부터 벡터 DB를 전제로 하지 않는다.
- 추천 엔진은 외부 MQ를 전제로 하지 않는다.
- 제안서, 매칭, 추천은 각각 독립적으로 검증 가능한 단계로 쪼갠다.
- 현재 ERD 범위와 이번에 함께 반영하는 추천 도메인 테이블 범위 안에서만 백로그를 작성한다.

## 2. 현재 전제

이번 백로그는 아래 가정으로 작성한다.

- 모든 활성 회원은 클라이언트 기능을 사용할 수 있다.
- `resume`가 존재하고 `resume.status = ACTIVE`이면 프리랜서로서 직접 지원과 매칭 흐름에 참여할 수 있다.
- `resume.writing_status = DONE`이고 `resume.publicly_visible = true`여야 검색/목록 노출 대상이 된다.
- 현재 ERD에는 `members.memo`, `resumes.status`, `resumes.writing_status`, `resumes.portfolio_url`, `profile_image`, `resume_attachments`가 존재한다.
- AI 추천 노출은 검색/목록 노출 조건에 더해 `resume.ai_matching_enabled`로 제어하고, 직접 지원 허용 여부와는 분리한다.
- `proposal.raw_input_text`는 AI 브리프 원본 입력을 저장한다.
- `proposal.total_budget_*`는 전체 프로젝트 예산이다.
- `proposal_position.unit_budget_*`는 1인 기준 예산이다.
- `proposal.total_budget_*`는 제안서 폼에서 직접 입력하지 않고 포지션 예산 합산값으로 계산한다.
- `proposal.status`는 `WRITING`, `MATCHING`, `COMPLETE`다.
- `proposal_position.status`는 `OPEN`, `FULL`, `CLOSED`다.
- `proposal_position`은 `title`, `work_type`, `expected_period`, `career_min/max_years`, `work_place`를 포함하는 상세 모집 단위다.
- `proposal_position.status = FULL`은 `matching.status in (ACCEPTED, IN_PROGRESS)` 수가 `head_count`에 도달했다는 뜻이고, `PROPOSED`는 정원을 점유하지 않는다.
- `matching`은 `proposal_position_id`, `resume_id`, `client_member_id`, `freelancer_member_id`, `status`, `contract_date`, `complete_date`를 유지하되, `ACCEPTED -> IN_PROGRESS`는 양측 계약 시작 확인을 조건으로 둔다.
- `contract_date`는 수락 시각이 아니라 양측 계약 시작 확인이 모두 끝난 시각이다.
- `IN_PROGRESS -> COMPLETED`는 양측 후기 작성과 완료 확인을 조건으로 둔다.
- `complete_date`는 완료 요청 시각이 아니라 후기 작성과 완료 확인 조건이 모두 충족된 시각이다.
- 활성 매칭은 `PROPOSED`, `ACCEPTED`, `IN_PROGRESS`로 본다.
- `client_member_id`, `freelancer_member_id`는 각각 제안서 소유 회원, 이력서 소유 회원과 일치해야 한다.
- 취소 요청은 `ACCEPTED`에서는 상대방 확인 또는 24시간 경과 시 자동 취소, `IN_PROGRESS`에서는 상대방 명시 확인으로만 취소한다.
- 현재 구현에서 후기는 `matching` 자체의 참여자별 필드로 저장한다.
- 상대방 후기는 `COMPLETED` 이후에만 공개한다.
- 현재 ERD에는 `proposal_position_skills`가 존재하고, 요구 스킬의 정규화 source로 사용한다.
- 현재 애플리케이션에는 `/files/proposal/**` 저장 경로가 있지만, ERD에는 `proposal_attachments`가 없어서 제안서 파일은 아직 정식 도메인 연관 자산이 아니다.
- 현재 구현 스키마는 `matching` 자체의 참여자별 계약 시작, 취소, 후기, 완료 확인 필드 기준으로 진행한다.
- 현재 ERD에는 `initiator_type`, `participation_status`가 없다.
- 현재 인증 principal에는 `memberId`가 없으므로 후기 작성/완료 확인 검증에는 principal 확장 또는 `email` 기반 회원 재조회가 필요하다.
- 현재 ERD와 코드 모두 `members.profile_image_id` 기준으로 프로필 이미지 1:1을 관리한다.
- `resume_attachments(resume_id, display_order)`, `resume_skills(resume_id, skill_id)`, `proposal_position_skills(proposal_position_id, skill_id)`는 중복 없이 관리한다.
- 계약 시작 확인 / 완료 확인 버튼 모델은 현재 MVP 백로그에 포함한다.
- `StoredFile.contentType`은 MIME 문자열(`String` / `varchar`)로 유지하고, enum 대신 허용 MIME type 검증으로 다룬다.
- 현재 ERD와 코드 모두 `Member`가 `ProfileImage` 1:1 연관관계 주인이고, `ProfileImage`는 `StoredFile`을 참조하는 별도 엔티티다.
- `ProfileImage`를 제거하고 `StoredFile`로 통합할지 여부는 프로필 이미지 서비스 책임을 먼저 정리한 뒤 다시 결정한다.

## 2.1 이번 스프린트에서 바로 나와야 하는 산출물

이번 문서 기준으로 팀이 실제로 만들어야 하는 최소 산출물은 아래와 같다.

- `Member`, `Resume`, `ProfileImage`, `ResumeAttachment`, `StoredFile`, `ResumeSkill` 정렬
- `Position`, `Proposal`, `ProposalPosition`, `ProposalPositionSkill`, `Matching` 엔티티/리포지토리
- 제안서 저장/모집 시작 흐름
- 매칭 요청/수락/계약 시작 확인/진행/완료 흐름
- `MatchingAttachment`, `MatchingReview` 모델
- 추천 엔진의 하드 필터 + Top 3 응답 뼈대
- 상태 전이와 노출 조건을 검증하는 테스트 세트

## 2.2 2026-04-19 구현 메모

- Phase 1의 프리랜서 프로필/이력서 흐름은 기본 구현이 들어가 있다.
- Phase 2와 Phase 4의 제안서 저장, 대시보드, 작성/수정 화면, 포지션 모달 UI는 기본 구현이 들어가 있다.
- 현재 남은 큰 축은 AI 인터뷰 위젯의 실제 동작, AI 브리프 스키마 정렬, 매칭 라이프사이클 완성이다.

## 3. 단계별 계획

### Phase 1. 프리랜서 진입 조건과 추천 입력 기초 정리

목표는 제안서와 매칭이 붙을 수 있는 최소한의 사용자 축을 만드는 것이다.

작업 항목은 아래와 같다.

- 프리랜서 진입 규칙을 `canApplyDirectly`, `canBeListed`, `canBeRecommended` 기준으로 분리
- `Member.memo` 반영 여부와 노출 범위 정리
- `Resume.status`, `Resume.writingStatus`, `Resume.portfolioUrl` 반영
- `ProfileImage` 서비스 책임과 `StoredFile` 경계 정리
- `ResumeAttachment` 엔티티/리포지토리 추가
- 프리랜서 전용 화면/서비스 접근 제어 구현
- `resume_skill` 엔티티, 리포지토리, 테스트 추가
- 기존 `Resume`와 연결되는 스킬 등록 API 또는 서비스 추가
- `StoredFile.contentType` 허용 MIME type 검증 정리
- 현재 코드 기준 `nickname`, `phone`, `careerYears`, `fileUrl` 제약 반영 여부 점검

검증은 아래와 같다.

- 회원 생성 테스트
- 프리랜서 이력서 + 스킬 등록 테스트
- 프로필 이미지 / 이력서 첨부 연결 테스트
- `profile_image`, `resume_attachments` 유니크 제약 테스트
- 중복 스킬 등록 방지 테스트
- 전화번호 정규화, 기본 닉네임, `fileUrl` prefix 제약 테스트
- `publiclyVisible`, `aiMatchingEnabled` 해석 테스트
- `status`, `writingStatus`가 추천 노출 조건에 미치는 영향 테스트

종료 산출물은 아래와 같다.

- 프리랜서 프로필 관련 엔티티와 리포지토리
- 추천 노출 가능 여부를 판정하는 서비스 규칙
- 파일 자산 연결 구조

### Phase 2. 직무 마스터와 제안서 도메인 구현

목표는 클라이언트가 ERD 기준 제안서를 작성하고 저장할 수 있게 하는 것이다.

작업 항목은 아래와 같다.

- `Position` 엔티티 및 초기 시드 설계
- `ProposalPositionSkill` 엔티티, 리포지토리, 테스트 추가
- `Proposal` 엔티티, 리포지토리, 테스트 추가
- `ProposalPosition` 엔티티, 리포지토리, 테스트 추가
- 제안서 저장/수정/모집 시작 서비스 추가
- `WRITING`, `MATCHING`, `COMPLETE` 상태 전이 규칙 구현
- `proposal_position_skills` 중복 입력 방지 규칙 구현

권장 API 범위는 아래와 같다.

- 제안서 생성
- 제안서 임시 저장
- 제안서 모집 시작
- 제안서 완료 처리
- 제안서 조회
- 제안서 포지션 추가/수정/삭제

검증은 아래와 같다.

- 제안서 저장 및 상태 전이 테스트
- 예산, 인원, 상태 검증 테스트
- `MATCHING` 상태 진입 전 최소 입력 검증 테스트
- `proposal_position_skills` 중복 방지 테스트
- 같은 제안서 안에서도 같은 직무 마스터를 여러 번 사용할 수 있고 `title`로 구분되는지 테스트

종료 산출물은 아래와 같다.

- 직무/모집 단위 스킬 구조
- 제안서와 모집 단위 저장 구조
- `WRITING -> MATCHING -> COMPLETE` 상태 전이 서비스

### Phase 3. AI 브리프를 현재 스키마에 연결 (완료)

목표는 자유 입력을 현재 ERD 기준 제안서 필드에 매핑하는 것이다.

현재 기준으로 AI 브리프 요청/응답 DTO, structured output schema, `Proposal` / `ProposalPosition`
매핑 로직은 구현 완료됐다. 남은 후속 작업은 AI 인터뷰 위젯 연결과 입력 품질 고도화다.

작업 항목은 아래와 같다.

- AI 브리프 요청/응답 DTO 정의
- 외부 LLM API 연동 어댑터 추가
- 사용자 자유 입력을 `proposal.raw_input_text`에 저장
- 응답을 `Proposal`, `ProposalPosition` 구조로 변환
- `description`, `expected_period`, `position_id`, `title`, `work_type`, `head_count`, `unit_budget_*` 매핑 로직 구현
- 실패 시 수동 입력 fallback 구현

중요한 제약은 아래와 같다.

- 현재 ERD에는 `raw_input_text`가 있고, `overview`는 없다.
- 현재 ERD에는 `proposal_position_skills`가 있다.
- 따라서 AI 브리프 결과 중 구조화된 요구 스킬은 `proposal_position_skills`까지 저장하도록 설계해야 한다.
- `raw_input_text`는 재생성, 비교, 신뢰 확보를 위한 원문 보존 필드로 사용한다.

검증은 아래와 같다.

- 응답 파싱 단위 테스트
- LLM 실패 시 fallback 테스트
- 구조화 결과를 현재 스키마에 맞게 저장 가능한 형태로 변환하는 테스트

종료 산출물은 아래와 같다.

- AI 브리프 어댑터
- 제안서/모집 단위 매핑 로직
- 실패 시 수동 입력 fallback 정책

### Phase 4. 제안서 화면과 기본 운영 흐름 연결

목표는 클라이언트가 실제로 제안서를 작성하고 관리할 수 있게 하는 것이다.

작업 항목은 아래와 같다.

- 클라이언트 대시보드에서 제안서 목록/상세/수정 화면 연결
- `WRITING`, `MATCHING`, `COMPLETE` 상태 배지와 액션 연결
- 제안서 포지션 반복 입력 UI 연결
- AI 브리프 생성 버튼과 수동 입력 fallback 연결
- 추천 결과 진입 조건을 `MATCHING` 상태에 맞춰 연결

검증은 아래와 같다.

- 목록/상세/수정 페이지 수동 확인
- `WRITING -> MATCHING -> COMPLETE` 흐름 수동 검증
- 추천 진입 가능 상태 검증

종료 산출물은 아래와 같다.

- 클라이언트 제안서 UI 기본 흐름
- 상태 배지와 액션 연결
- AI 브리프 생성 진입점

### Phase 5. 매칭 MVP

목표는 요청, 수락, 계약 시작 확인, 진행, 완료를 현재 상태 모델 위에서 구현하는 것이다.

작업 항목은 아래와 같다.

- `Matching` 엔티티, 리포지토리, 테스트 추가
- `status`, `contract_date`, `complete_date` 구현
- 클라이언트 요청 플로우 구현
- 프리랜서 직접 지원 플로우 구현
- 수락/거절/취소 처리 구현
- 양측 계약 시작 확인 플로우 구현
- 양측 계약 시작 확인 완료 시 `IN_PROGRESS` 전이 및 `contract_date` 기록 구현
- 취소 요청/철회/확인 플로우 구현
- `ACCEPTED` 취소 요청 24시간 자동 취소 처리 구현
- 상호 후기 작성 구현
- 후기 작성 화면 조회 시 `matching + principal` 기준으로 작성 가능 여부, 상대방 정보, 기존 후기 여부 계산 구현
- 완료 조건 충족 시 `COMPLETED` 전이 및 `complete_date` 기록 구현
- 연락처 공개 시점 제어 구현
- 정원 도달 시 추가 요청 차단 구현
- 수락/취소/완료 시 `proposal_position.status` 재계산 구현

현재 모델의 제약은 아래와 같다.

- `initiator_type`이 없으므로 요청 주체는 API 엔드포인트나 감사 로그에서 구분해야 한다.
- `participation_status`가 없으므로 요청 처리와 진행 상태가 `matching.status` 하나에 모두 들어간다.
- 활성 매칭은 `PROPOSED`, `ACCEPTED`, `IN_PROGRESS`이며 같은 `proposal_position + resume` 조합에 동시에 하나만 존재해야 한다.
- 참여자 판정은 `proposal_position -> proposal`, `resume -> member` 역추적 대신 `matching.client_member_id`, `matching.freelancer_member_id`를 기준으로 처리한다.
- 정원은 `ACCEPTED`, `IN_PROGRESS`만 점유하고 `PROPOSED`는 점유하지 않는다.
- 정원 여유가 다시 생기면 `proposal_position.status`는 `OPEN`으로 되돌릴 수 있어야 한다.
- 계약 시작 확인, 취소 요청/확인, 후기 작성, 완료 확인을 상태 게이트로 사용한다.
- 계약서/완료 증빙 업로드와 별도 리뷰 테이블은 현재 구현 범위에서 보류한다.
- 현재 principal에는 `memberId`가 없으므로 후기 작성/완료 확인 검증 전에 인증 컨텍스트 정리가 필요하다.

권장 API 범위는 아래와 같다.

- 후보 요청 생성
- 프리랜서 지원 생성
- 요청 수락/거절
- 요청 취소
- 계약 시작 확인
- 진행 시작
- 취소 요청/철회/확인
- 후기 작성
- 완료 처리
- 매칭 목록 조회

검증은 아래와 같다.

- 동일 `proposal_position + resume` 활성 매칭 중복 방지 테스트
- `client_member_id`, `freelancer_member_id`가 각각 제안서/이력서 소유자와 일치하는지 테스트
- 수락 시 정원 재검증 테스트
- `PROPOSED`가 정원을 점유하지 않는지 테스트
- 수락 전 연락처 비노출 테스트
- `ACCEPTED` 상태에서 양측 계약 시작 확인이 모두 없으면 `IN_PROGRESS`로 갈 수 없는지 테스트
- 양측 계약 시작 확인 완료 시 `contract_date`가 기록되는지 테스트
- `FULL`, `CLOSED` 상태에서 신규 매칭 생성 불가 테스트
- 양측 후기 작성과 완료 확인이 모두 없으면 `COMPLETED`로 갈 수 없는지 테스트
- 후기 작성 시 `matching.client_member_id`, `matching.freelancer_member_id`, 로그인 회원 기준으로 작성 가능 여부가 계산되는지 테스트
- 같은 참여자가 같은 `matching`에 후기를 중복 작성할 수 없는지 테스트
- `IN_PROGRESS`가 아니면 후기 작성이 불가능한지 테스트
- 완료 조건 충족 시 `complete_date`가 기록되는지 테스트
- 수락 취소/완료 후 `FULL -> OPEN` 재계산 테스트
- `status` 단일 필드 전이 테스트

종료 산출물은 아래와 같다.

- 요청/지원/수락/계약 시작 확인/진행/완료 API
- 취소 요청/철회/확인 API와 화면 DTO
- 매칭 후기 작성 API와 화면 DTO
- 연락처 공개 시점 제어
- 정원 기반 매칭 차단 규칙
- 계약 시작 확인/취소 요청/후기/완료 확인 기반 상태 게이트

### Phase 6. 추천 엔진 MVP

목표는 제출된 제안서에 대해 Top 3 추천을 만드는 것이다.

작업 항목은 아래와 같다.

- 하드 필터 규칙 구현
- 점수화 규칙 구현
- Top 3 선정 로직 구현
- 추천 결과 저장 모델 구현
- 추천 결과 캐시 설계
- 이력서 임베딩 캐시 설계
- Top 3에 대한 LLM 설명 생성 연결
- 추천 결과 조회 API 구현

MVP 운영 원칙은 아래와 같다.

- `pgvector` extension은 도입하지 않고 `resume_embeddings.embedding_vector`를 `jsonb`로 저장한다.
- 외부 MQ는 도입하지 않고 `recommendation_results.llm_status` 업데이트 기반으로 설명 생성 흐름을 관리한다.
- `proposal_position` 임베딩은 별도 테이블로 저장하지 않고 요청 시 계산한다.

현재 하드 필터 기준은 아래를 우선한다.

- `resume.status = ACTIVE`
- `resume.writing_status = DONE`
- `resume.publicly_visible = true`
- `resume.ai_matching_enabled = true`
- `proposal_position_skills.importance = ESSENTIAL`

현재 점수화 기준은 아래를 우선한다.

- `embedding similarity`
- `preferred_work_type`와 `proposal_position.work_type`의 호환성
- 우대 스킬 겹침 정도
- `career_years`

현재 모델의 제약은 아래와 같다.

- `proposal_position_skills`는 현재 스킬 ID와 중요도만 표현하므로 세부 요구 수준은 담기 어렵다.
- 직무 마스터 `position`에는 기본 스킬 템플릿이 없어서 생성 보조 재사용성이 낮다.
- `proposal_position`에 경력 최소/최대 범위가 추가됐지만, 초기 추천 단계에서는 `career_years`를 강한 하드 필터보다 점수 항목으로 해석하는 편이 안전하다.

검증은 아래와 같다.

- 하드 필터 테스트
- 점수 계산 테스트
- 캐시 적중/무효화 테스트
- 제안서 `MATCHING` 전환 시 추천 가능 테스트

종료 산출물은 아래와 같다.

- 추천 후보군 하드 필터
- Top 3 선정 로직
- `recommendation_runs`, `recommendation_results` 기반 결과 저장
- `resume_embeddings` 기반 이력서 임베딩 캐시
- 설명 가능한 추천 응답 포맷

### Phase 7. 운영 화면과 품질 관리

목표는 운영자가 데이터 품질과 추천 품질을 점검할 수 있게 하는 것이다.

작업 항목은 아래와 같다.

- 운영자 권한 확인
- 직무/스킬 시드 관리 화면
- 추천 결과 확인 화면
- 추천 실패 케이스 조회
- 공개/비공개 이력서 상태 점검 화면

이 단계는 MVP 핵심 기능이 안정화된 뒤 진행해도 된다.

## 4. 현재 ERD 기준 보류 항목

현재 ERD에 없어서 이번 백로그에서 제외한 항목은 아래와 같다.

- `proposal_attachments` 또는 정식 제안서 파일 연관 모델
- `matching.initiator_type`
- `matching.participation_status`
- 파일 기반 계약서/완료 증빙 모델
- `DONE -> WRITING` 재오픈 규칙

이 항목들은 ERD가 바뀌는 시점에 다시 백로그로 올린다.

## 5. 병렬화 가능한 작업

아래 작업은 병렬화 가능하다.

- `Position` 시드 준비와 `Proposal` 엔티티 구현
- `resume_skill` 구현과 추천 점수 함수 설계
- 제안서 화면 UI 작업과 AI 브리프 DTO 정의

아래 작업은 병렬화하면 안 된다.

- `ProposalPositionSkill` 없이 추천 점수 계산 구현
- `ProposalPosition` 상태 모델 구현 전 매칭 수락 규칙 구현

## 6. 팀 리뷰 체크포인트

PR 리뷰나 중간 점검 때는 아래 질문을 공통으로 본다.

1. ERD에 없는 필드를 코드에 먼저 추가하지 않았는가
2. 프리랜서 진입, 검색 노출, AI 추천 노출 규칙을 서로 다른 조건으로 구현했는가
3. `proposal`과 `proposal_position`의 예산 의미를 섞지 않았는가
4. `matching.status` 하나에 현재 허용한 상태만 사용하고 있는가
5. 추천 노출 조건이 `status`, `writing_status`, `publicly_visible`, `ai_matching_enabled`를 모두 반영하는가
6. `FULL` 판정과 활성 매칭 판정을 같은 기준으로 재사용하는가
7. 프로필/이력서/스킬 연결 테이블의 유니크 제약을 코드와 DB 양쪽에서 놓치지 않았는가
8. `contract_date`, `complete_date`를 각각 계약 시작 확인 완료 시각, 완료 조건 충족 시각으로 일관되게 쓰고 있는가
9. `IN_PROGRESS`, `COMPLETED` 전이 조건이 계약 시작 확인/후기/완료 확인 규칙과 일치하는가

## 7. 첫 구현 순서

가장 먼저 손대야 할 실제 구현 순서는 아래와 같다.

1. 프리랜서 역할 파생 규칙과 현재 코드 기반 값 제약 정리
2. `resume_skill` 추가
3. `Position`, `Proposal`, `ProposalPosition`, `ProposalPositionSkill` 엔티티 추가
4. 제안서 저장/모집 시작 서비스 구현
5. `Matching` 구현

즉, 첫 코드 작업은 AI가 아니라 도메인 테이블과 상태 모델이다.

## 8. 권장 검증 순서

검증은 아래 순서를 권장한다.

1. 도메인 단위 테스트
2. JPA 리포지토리 테스트
3. 서비스 통합 테스트
4. 컨트롤러/폼 흐름 테스트
5. 마지막에 AI 연동 모킹 테스트

이유는 아래와 같다.

- AI 연동 이전에도 제안서와 매칭 도메인은 자체적으로 완결되어야 한다.
- 추천 품질 문제와 도메인 무결성 문제를 분리해서 볼 수 있다.

## 9. 주요 리스크

현재 기준 주요 리스크는 아래와 같다.

- 직접 지원 가능 여부, 검색/목록 노출 여부, AI 추천 노출 여부를 같은 의미로 잘못 해석하면 화면과 권한 설계가 흔들린다.
- `proposal_position_skills` 입력 품질이 낮으면 추천 근거와 스킬 일치도 설명이 흔들린다.
- `Resume.status`, `Resume.writingStatus`, `profile_image`, `resume_attachments`를 늦게 반영하면 이력서 화면 구조와 추천 필터 조건이 다시 흔들릴 수 있다.
- `matching.status` 하나에 요청 처리와 진행 이력을 모두 넣으면 상태 전이 버그가 빠르게 늘어난다.
- `proposal_position` 정원 수락 경쟁과 `FULL -> OPEN` 재계산은 동시성 문제를 만든다.
- 계약 시작 확인, 취소 확인, 후기 작성, 완료 확인이 한쪽에서만 멈추면 장기 체류 상태가 쉽게 생긴다.
- `MATCHING` 상태 제안서의 수정 정책이 현재 ERD에 없어서 UX 규칙을 따로 정해야 한다.

## 10. 완료 기준

이번 백로그 기준 MVP 핵심 완료 조건은 아래와 같다.

- 프리랜서가 이력서와 스킬을 등록할 수 있다.
- 클라이언트가 제안서를 작성하고 `MATCHING` 상태로 전환할 수 있다.
- 제안서 안에 여러 `proposal_position`을 둘 수 있다.
- 제안서 제출 시 추천 가능한 입력 구조가 DB에 저장된다.
- 클라이언트 요청과 프리랜서 지원이 `matching`으로 관리된다.
- 수락 이후에만 연락처가 공개된다.
- 양측 계약 시작 확인 후에만 `IN_PROGRESS`로 전이된다.
- 양측 후기 작성과 완료 확인 후에만 `COMPLETED`로 전이된다.
- 하드 필터 + 점수화 + Top 3 설명 흐름이 동작한다.
