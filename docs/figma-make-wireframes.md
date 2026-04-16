# IT-da Figma Make 와이어프레임 프롬프트

이 문서는 `IT-da`의 MVP 화면을 Figma Make로 빠르게 와이어프레임화하기 위한 프롬프트 모음이다.
기준 문서는 [project-overview.md](/Users/ilhyeon/Documents/study/devcourse/backend/projects/IT-da/docs/project-overview.md)와 [domain-spec.md](/Users/ilhyeon/Documents/study/devcourse/backend/projects/IT-da/docs/domain-spec.md)다.

## 1. 사용 방식

- 먼저 아래 `마스터 프롬프트`를 Figma Make에 넣어 전체 스타일과 제약을 고정한다.
- 그 다음 화면별 프롬프트를 하나씩 넣어 개별 프레임을 생성한다.
- 이번 문서의 목표는 고충실도 UI가 아니라 정보 구조와 핵심 행동이 보이는 저충실도 와이어프레임이다.
- 색상, 브랜딩, 이미지, 마케팅 카피보다 화면 구조, 상태, CTA, 데이터 블록 구분을 우선한다.

## 2. 빠른 시작용 통합 프롬프트

바로 다음 주 개발 착수용으로는 아래 통합 프롬프트를 먼저 쓰는 편이 낫다.
이 프롬프트는 한 번에 핵심 화면 6개를 같은 스타일과 정보 구조로 생성하도록 요청한다.

```text
Create a set of low-fidelity to mid-fidelity desktop wireframes for an AI-based freelancer matching platform named IT-da.

I need development-ready product wireframes for a Korean B2B SaaS web app, not polished marketing design. The purpose is to align screen structure, states, forms, lists, and actions before implementation starts next week.

Product summary:
- IT-da connects IT project clients and IT freelancers.
- A client starts with a single free-text project description.
- AI turns that into a structured proposal.
- The system recommends top freelancer candidates.
- The client compares recommended candidates and direct applicants, then sends a matching request.
- A freelancer manages a resume/profile, can enable or disable AI recommendation inclusion, can apply directly to projects, and can accept or reject matching requests.
- There is no separate project runtime entity in MVP. Matching records handle request, acceptance, in-progress, and completion states directly.

Critical domain constraints:
- Any active member can act as a client.
- A member with a resume can also act as a freelancer.
- Resume.publiclyVisible controls search and list visibility.
- Resume.aiMatchingEnabled controls inclusion in AI recommendations only. It does not block direct applications.
- Proposal status is WRITING, MATCHING, or COMPLETE.
- Recommendation and applicant comparison are available when proposal status is MATCHING.
- Proposal has total project budget.
- Proposal position has per-person unit budget and headcount.
- Proposal position status is OPEN, FULL, or CLOSED.
- Matching uses a single status flow rather than a separate participation_status field.

Visual and UX constraints:
- Desktop-first web application.
- Low-fidelity or mid-fidelity wireframes only.
- Use grayscale with very restrained accent color.
- No gradients, glassmorphism, illustrations, stock photos, or polished marketing visuals.
- Keep copy in Korean.
- Make everything feel realistic for Thymeleaf + Tailwind + Alpine.js + Lucide implementation.
- Prioritize headers, sidebars, tables, cards, forms, chips, filters, comparison trays, status badges, empty states, and inline helper text.
- Reuse a consistent layout system and repeated component patterns across all screens.
- Show practical B2B SaaS structure, not concept-art UI.

Create these 6 screens as separate frames in one file:

1. Landing / role entry
- Top nav with logo, login, signup.
- Hero with one-sentence value proposition.
- Two strong CTA cards: "클라이언트로 시작하기", "프리랜서로 시작하기".
- A simple 4-step service flow:
  - 프로젝트 입력
  - AI 브리프 구조화
  - 후보 추천 및 비교
  - 매칭 및 참여 관리
- Brief feature blocks for AI 브리프, 설명 가능한 추천, 이력서 기반 프로필 관리.

2. Client dashboard / proposal list
- Left sidebar: 대시보드, 제안서, 추천 결과, 매칭 관리, 프로필.
- Top bar with page title, search, user menu.
- KPI cards: 작성 중 제안서, 제출 완료 제안서, 진행 중 매칭, 종료된 참여.
- Primary CTA: "새 프로젝트 만들기".
- Proposal list with title, WRITING/MATCHING/COMPLETE status, number of positions, total budget, last modified date, recommendation readiness, actions.
- Clear first-time empty state.

3. AI brief editor / proposal editing
- Two-column layout.
- Left: large free-text input, helper text, AI 브리프 생성 button, save state, last saved time, proposal status.
- Right: structured proposal form with title, description, total budget, project work type, work place, expected period, and repeatable proposal positions.
- Each proposal position block should include:
  - 직무 선택
  - 모집 인원
  - 1인 기준 예산 범위
  - 모집 상태
- Bottom actions: 임시 저장, 매칭 시작, 추천 보기.
- Show inline notice that recommendation is disabled while WRITING and becomes available in MATCHING.
- Show fallback hint for AI generation failure.

4. Recommendation results / candidate comparison
- Header with proposal title, proposal status, selected proposal position filter.
- Controls: 포지션 선택, 전체 프리랜서/미참여 프리랜서 filter, sort option, 제안서 수정.
- Main split layout:
  - left: AI 추천 Top 3
  - right: 지원자 리스트
- Each recommended candidate card should include nickname, participation summary, skill chips, skill match percentage, AI comment preview, AI interview question preview, actions.
- Applicant list should show application date, skills, career years, status, actions.
- Add a sticky comparison tray or comparison side panel for 2-3 candidates.
- Include empty state and “all recommendations rejected” warning state.

5. Freelancer profile / resume management
- Sidebar: 프로필, 이력서, 지원 내역, 매칭 현황.
- Top summary area: profile completeness, publicly visible status, AI recommendation inclusion status, active participation count.
- Main editor sections:
  - 기본 소개
  - 경력 요약
  - 선호 근무 형태
  - 공개 설정
  - 보유 스킬
  - 경력 리스트
  - 포트폴리오 / 첨부 파일
- Public visibility toggle and AI recommendation inclusion toggle must be clearly separated.
- Include helper text stating that AI recommendation inclusion only affects AI recommendation, not direct applications.

6. Matching detail / matching management
- Header with proposal title, proposal position title, freelancer nickname, matching status badge.
- Left column: matching lifecycle stepper (요청 전송, 수락, 진행중, 완료, 거절/취소), current state card, state transition controls.
- Right column: summary cards for contact visibility, budget info, work type, required skills, and an activity log / memo area.
- Make it explicit that contact information is hidden before acceptance and visible after matching acceptance.
- Make it explicit that this screen manages the whole matching lifecycle because MVP has no separate project entity.

Output expectations:
- Create all 6 frames in one coherent system.
- Use consistent spacing, card structure, table patterns, and badge styles.
- Do not over-design.
- Optimize for engineering handoff and implementation clarity.
```

## 2.1 raw_input_text 반영 프롬프트

아래 프롬프트는 현재 ERD에 반영된
`raw_input_text` / `description` 2단 구조를
화면에 명확히 드러내기 위한 개발용 프롬프트다.

핵심 의도는 이렇다.

- `raw_input_text`: 사용자가 처음 입력한 자유 텍스트 원문
- `description`: 실제 제출되는 본문이자 사용자가 편집하는 최종 제안서 내용

즉, `description`은 상세 전용이 아니라 작성 화면에서도 바로 보이고 수정 가능해야 한다.

```text
Create low-fidelity to mid-fidelity desktop wireframes for IT-da, an AI-based freelancer matching platform for IT projects.

This is not a polished visual design task. I need development-ready wireframes that help the team validate one specific product direction before implementation: keeping only the original AI brief input and the final editable proposal body.
- raw_input_text = the original free-text project input written by the client
- description = the full editable proposal body that will actually be submitted

Important product intent:
- The client should not feel that AI output is hidden until a detail page.
- The client must be able to compare their original input and the full generated proposal body in the editing flow.
- description is the canonical proposal content and must be editable before submission.
- raw_input_text should remain visible enough to support regeneration, comparison, and trust.

Create 4 frames in one consistent grayscale B2B SaaS wireframe style:

1. Client dashboard / proposal list
- Show proposal cards or rows.
- Each item should include:
  - proposal title
  - status badge
  - short description excerpt only, not full description
  - total budget range
  - number of positions
  - last modified date
  - actions such as edit, recommendation results, duplicate
- Make it obvious that list screens use a short description excerpt as preview text.

2. AI brief editor / proposal editing
- Desktop two-column layout.
- Left panel:
  - raw_input_text textarea
  - helper text explaining this is the original user input
  - AI brief generate / regenerate action
  - save state and proposal status
- Right main editing area:
  - editable description field as the main proposal body
  - title
  - total budget range
  - work type
  - work place
  - expected period
  - repeatable proposal positions
- Each proposal position should include:
  - position selector
  - headcount
  - unit budget range
  - position status
- Bottom actions:
  - 임시 저장
  - AI 재생성
  - 매칭 시작
- The screen must clearly show that description is editable now, not hidden for later.

3. Proposal detail / review screen
- Header with proposal title and status
- Main content block with full description
- Secondary metadata blocks for budget, work type, work place, expected period, positions
- Show the original input as a secondary collapsible block or side panel, not as a competing main summary.

4. Recommendation results screen
- Header with proposal title and a compact description excerpt
- Do not show full description inline here
- Show selected proposal position filter
- Left area: AI top 3 candidates
- Right area: direct applicants
- Sticky comparison tray
- This screen should reinforce that the proposal body remains the canonical source, while list-like contexts use a short description excerpt

Design constraints:
- Desktop-first
- Low-fidelity or mid-fidelity only
- Grayscale with restrained accent color
- Korean copy
- Realistic Thymeleaf + Tailwind + Alpine.js + Lucide implementation style
- Prefer tables, cards, forms, chips, helper text, state badges, and summary blocks
- No marketing visuals, gradients, illustrations, or decorative concepts

Output goal:
- Help the team create development-ready wireframes for the current proposal editing model, where raw_input_text is preserved and description is the single canonical proposal body.
```

## 3. 마스터 프롬프트

```text
Create low-fidelity product wireframes for an AI-based freelancer matching platform named IT-da.

The product connects IT project clients and IT freelancers. A client writes a project with a single free-text input, AI structures it into a proposal, the system recommends top freelancer candidates, and the client compares candidates and sends a matching request. A freelancer can register a resume/profile, opt into AI recommendation exposure, apply directly to projects, and accept or reject matching requests. There is no separate project runtime entity in MVP, so the matching record handles proposal, acceptance, progress, and completion lifecycle states directly.

Important domain constraints:
- Any active member can act as a client.
- A member with a resume can also act as a freelancer.
- Resume.publiclyVisible controls search/list visibility.
- Resume.aiMatchingEnabled controls inclusion in AI recommendation only, not direct application.
- There is no separate project runtime entity in MVP. Matching records handle lifecycle history directly.
- Proposal status is WRITING, MATCHING, or COMPLETE.
- Proposal has total project budget.
- Proposal position has per-person unit budget and headcount.
- Proposal position status is OPEN, FULL, or CLOSED.

Design requirements:
- Desktop-first web application wireframes.
- Use grayscale or very restrained color accents only.
- Use clear layout blocks, labels, tables, chips, empty states, and side panels.
- Prioritize IA, hierarchy, forms, cards, filters, and state visibility.
- Keep copy in Korean.
- Avoid marketing hero art, stock photos, decorative illustrations, glassmorphism, gradients, or high-fidelity styling.
- Show realistic product UI with headers, navigation, buttons, form fields, lists, status badges, and comparison modules.
- Make screens feel like a practical B2B SaaS admin/product workflow.
```

## 4. 화면 목록

MVP 기준 추천 화면은 아래 6개다.

1. 랜딩 / 역할 진입
2. 클라이언트 대시보드 / 제안서 목록
3. AI 브리프 작성 / 제안서 편집
4. 추천 결과 / 후보 비교
5. 프리랜서 프로필 / 이력서 관리
6. 매칭 상세 / 매칭 상태 관리

## 5. 화면별 프롬프트

### 4.1 랜딩 / 역할 진입

```text
Create a low-fidelity desktop wireframe for the IT-da landing page.

Purpose:
- Explain the service in one screen.
- Let users quickly enter as client or freelancer.
- Emphasize the main loop: free-text project input -> AI brief -> explainable recommendation -> matching.

Required sections:
- Top navigation with logo, login, sign up.
- Hero section with one-sentence product value.
- Two primary CTA cards side by side:
  - "클라이언트로 시작하기"
  - "프리랜서로 시작하기"
- A simple 4-step service flow diagram:
  - 프로젝트 입력
  - AI 브리프 구조화
  - 후보 추천 및 비교
  - 매칭 및 참여 관리
- Short feature blocks for:
  - AI 브리프
  - 설명 가능한 추천
  - 이력서 기반 프로필 관리
- Footer with simple support/navigation links.

Wireframe rules:
- Keep the layout clean and structured.
- No marketing illustrations.
- Use outlined boxes, arrows, placeholders, and labels.
- Focus on conversion and information hierarchy, not branding polish.
```

### 4.2 클라이언트 대시보드 / 제안서 목록

```text
Create a low-fidelity desktop wireframe for the client dashboard of IT-da.

Purpose:
- Help a client manage multiple proposals at once.
- Show proposal writing state, recommendation readiness, and quick actions.

Required layout:
- Left sidebar navigation with:
  - 대시보드
  - 제안서
  - 추천 결과
  - 매칭 관리
  - 프로필
- Top bar with page title, search, user menu.
- Main dashboard content with:
  - KPI summary cards:
    - 작성 중 제안서
    - 매칭 중 제안서
    - 진행 중 매칭
    - 완료된 제안서
  - "새 프로젝트 만들기" primary button
  - Proposal list table or card list

Each proposal item should show:
- 제목
- 상태 badge: WRITING or MATCHING or COMPLETE
- 포지션 수
- 전체 예산 범위
- 마지막 수정일
- 추천 상태 summary such as:
  - 작성 중
  - 매칭 진행 중
  - 완료
- actions:
  - 편집
  - 추천 보기
  - 복제

Include empty state guidance for first-time users:
- Explain that they can start with a single free-text project input.

Wireframe rules:
- Make status visibility very clear.
- The dashboard should feel operational and list-driven.
- Use table, chips, action buttons, and right-aligned metadata.
```

### 4.3 AI 브리프 작성 / 제안서 편집

```text
Create a low-fidelity desktop wireframe for the IT-da AI brief editor screen.

Purpose:
- Let a client paste a free-text project idea.
- Show AI-generated structured proposal fields.
- Support editing, autosave awareness, and submission.

Required layout:
- Two-column workspace.
- Left column:
  - large free-text input area titled "프로젝트를 자유롭게 설명해 주세요"
  - helper text with example input
  - AI 브리프 생성 button
  - save status area showing:
    - 로컬 자동 저장
    - 마지막 저장 시각
    - 제안서 상태 badge: WRITING or MATCHING or COMPLETE
- Right column:
  - structured form generated by AI
  - fields for:
    - 프로젝트 제목
    - 프로젝트 설명
    - 전체 예산 범위
    - 전체 근무 형태
    - 근무 장소
    - 예상 기간
    - 포지션 섹션 repeatable list

Each proposal position block should include:
- 직무 선택
- 모집 인원
- 1인 기준 예산 범위
- 모집 상태

Bottom actions:
- 임시 저장
- 매칭 시작
- 추천 보기

Important state hints:
- Show that recommendation is enabled only when status is MATCHING.
- Add helper text that current ERD does not store raw input text separately after structured save.
- Include a small fallback area for "AI 생성 실패 시 수동 입력 가능".

Wireframe rules:
- Prioritize form layout and editing flow.
- Make repeated proposal position cards easy to scan.
- No polished visuals; use form blocks, chips, outlines, and helper text.
```

### 4.4 추천 결과 / 후보 비교

```text
Create a low-fidelity desktop wireframe for the IT-da recommendation results screen.

Purpose:
- Help a client compare AI-recommended freelancers and direct applicants.
- Make recommendation reasoning visible.

Required layout:
- Header with proposal title, proposal status, and selected proposal position filter.
- Secondary controls:
  - 포지션 선택 dropdown
  - 전체 프리랜서 / 미참여 프리랜서 filter
  - 정렬 옵션
  - 제안서 수정 button
- Main content split into two sections:
  - left: AI 추천 Top 3
  - right: 지원자 리스트

For each AI recommended candidate card show:
- 닉네임
- 현재 참여 상태 summary
- 핵심 스킬 chips
- 스킬 일치도 percentage visualization
- short AI 코멘트
- AI 추천 면접 질문 2-3개 preview
- actions:
  - 상세 보기
  - 비교에 추가
  - 매칭 요청

For the applicant list show:
- applicant row cards or table
- 지원 일시
- 핵심 스킬
- 경력 연차
- 상태
- 상세 보기 / 비교하기

Include a sticky comparison tray or comparison panel where 2-3 candidates can be compared side by side by:
- 스킬
- 경력
- 선호 근무형태
- 참여 중 프로젝트 여부
- AI 코멘트 요약

Important states:
- Empty state when no recommendations are ready yet.
- Warning state when all recommended candidates were rejected, guiding the client to refine the proposal.

Wireframe rules:
- Focus on decision support, not profile beauty.
- Use cards, badges, comparison table, filters, and side panel patterns.
```

### 4.5 프리랜서 프로필 / 이력서 관리

```text
Create a low-fidelity desktop wireframe for the freelancer profile and resume management screen in IT-da.

Purpose:
- Let a freelancer manage the resume that powers recommendation and direct applications.
- Make visibility settings and profile completeness explicit.

Required layout:
- Left sidebar navigation for freelancer mode:
  - 프로필
  - 이력서
  - 지원 내역
  - 매칭 현황
- Main content with top summary area:
  - 프로필 완성도
  - 추천 노출 상태
  - AI 추천 포함 상태
  - 현재 참여 중 프로젝트 수

Main editor sections:
- 기본 소개
- 경력 요약
- 선호 근무 형태
- 공개 설정
  - 공개 여부 toggle
  - AI 추천 포함 여부 toggle
  - helper text that AI 추천 포함 여부 does not block direct applications
- 보유 스킬 section
  - skill chips
  - 숙련도 선택
- 경력 섹션
  - company
  - position
  - employment type
  - period
  - summary
  - tech stack
- 포트폴리오 / 첨부 파일 section

Right side or bottom panel:
- 추천 대상 조건 안내
- 프로필 미리보기 card
- 직접 지원 가능한 프로젝트 shortcut

Wireframe rules:
- This screen should feel like a structured professional profile editor.
- Use repeatable experience blocks and clear settings areas.
- Keep the distinction between visibility and AI recommendation eligibility explicit.
```

### 4.6 매칭 상세 / 매칭 상태 관리

```text
Create a low-fidelity desktop wireframe for the IT-da matching detail screen.

Purpose:
- Manage the relationship after a matching request is sent or accepted.
- Show the full lifecycle with one matching status field.

Required layout:
- Page header with:
  - proposal title
  - proposal position title
  - freelancer nickname
  - matching status badge
- Main 2-column layout.

Left column:
- Timeline or stepper for matching lifecycle:
  - 요청 전송
  - 수락
  - 진행중
  - 완료
  - 거절 / 취소
- Current state card:
  - 현재 매칭 상태
  - contract_date
  - complete_date
- State transition controls:
  - 요청 취소
  - 수락 대기
  - 진행 시작
  - 완료 처리

Right column:
- Summary cards for:
  - 연락처 공개 상태
  - 예산 정보
  - 근무 형태
  - 요구 스킬 요약
- Notes/activity section:
  - 최근 상태 변경 로그
  - 간단한 메모 입력

Important state rules to reflect:
- Contact information is hidden before acceptance and visible only after matching is accepted.
- There is no separate project entity in MVP; this screen manages matching lifecycle through one status field.

Wireframe rules:
- Make state history and current next action obvious.
- The screen should feel operational, like a workflow detail page.
- Use stepper, badges, logs, and side summary cards.
```

## 6. 추천 실행 순서

처음부터 모든 화면을 한 번에 만들기보다 아래 순서가 안전하다.

1. 랜딩 / 역할 진입
2. 클라이언트 대시보드
3. AI 브리프 작성 / 제안서 편집
4. 추천 결과 / 후보 비교
5. 프리랜서 프로필 / 이력서 관리
6. 매칭 상세 / 참여 상태 관리

이 순서로 가면 제품의 메인 루프가 앞에서부터 자연스럽게 이어진다.

## 7. 다음 확장 후보

현재 문서에는 넣지 않았지만, 다음 화면은 후속으로 붙이기 좋다.

- 로그인 / 회원가입
- 지원 내역 목록
- 운영자 품질 관리 대시보드
- 추천 실패 / 빈 결과 상태 모달
- 모바일 대응 흐름
