package com.generic4.itda.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/prototypes/recommendations")
public class RecommendationPrototypeController {

    @GetMapping
    public String entry(Model model) {
        addPrototypeFrame(
                model,
                "entry",
                "추천 플로우 진입",
                "추천 실행 전 확인해야 할 제안서 상태, 포지션 맥락, 실행 조건을 한 화면에서 보여주는 mock 진입 화면입니다.",
                "Prototype Entry",
                guide(
                        "추천 실행에 필요한 제안서와 이력서 풀 상태를 빠르게 확인하고, 어떤 결과 화면으로 이어질지 분기합니다.",
                        List.of(
                                "recommendationRun 요약 정보",
                                "proposal / proposalPosition 상태와 예산",
                                "추천 가능 후보 풀 요약과 hard filter 요약",
                                "실행 전 체크리스트 및 mock payload"
                        ),
                        List.of(
                                "포지션 필터 변경",
                                "추천 결과 보기",
                                "결과 없음 상태 보기",
                                "실패 상태 보기"
                        ),
                        List.of(
                                nextFlow("계산 완료 플로우", "/prototypes/recommendations/results"),
                                nextFlow("빈 결과 플로우", "/prototypes/recommendations/empty"),
                                nextFlow("실패 플로우", "/prototypes/recommendations/error")
                        )
                ),
                "recommendationEntry",
                entryPayloadJson()
        );

        model.addAttribute("entryOverview", entryOverview());
        model.addAttribute("proposalSummary", proposalSummary());
        model.addAttribute("positionOptions", positionOptions());
        model.addAttribute("candidatePoolSummary", candidatePoolSummary());
        model.addAttribute("readinessChecklist", readinessChecklist());
        return "prototypes/recommendation/entry";
    }

    @GetMapping("/results")
    public String results(Model model) {
        List<Map<String, Object>> recommendationResults = recommendationResults();
        List<Map<String, Object>> applicants = applicants();

        addPrototypeFrame(
                model,
                "results",
                "추천 결과 목록",
                "AI 추천 Top 후보와 직접 지원자를 카드 기반으로 비교하고, 비교 트레이로 후보 2~3명을 압축해 보는 화면입니다.",
                "Prototype Results",
                guide(
                        "추천 품질과 직접 지원 현황을 함께 보여줘서, 클라이언트가 매칭 요청 전 후보를 빠르게 좁히도록 돕습니다.",
                        List.of(
                                "recommendationRun 상태와 hard filter 통계",
                                "recommendationResults[] 카드 데이터",
                                "applicants[] 직접 지원 카드 데이터",
                                "비교 트레이에 필요한 공통 비교 필드"
                        ),
                        List.of(
                                "포지션 / 후보 상태 / 정렬 필터 변경",
                                "후보 상세 보기",
                                "비교 트레이에 추가 / 제거",
                                "매칭 요청 버튼 확인"
                        ),
                        List.of(
                                nextFlow("후보 상세 화면", "/prototypes/recommendations/results/101"),
                                nextFlow("빈 결과 예외 플로우", "/prototypes/recommendations/empty"),
                                nextFlow("추천 실패 예외 플로우", "/prototypes/recommendations/error")
                        )
                ),
                "recommendationResultsPage",
                resultsPayloadJson()
        );

        model.addAttribute("recommendationRun", recommendationRun());
        model.addAttribute("proposalSummary", proposalSummary());
        model.addAttribute("positionOptions", positionOptions());
        model.addAttribute("sortOptions", sortOptions());
        model.addAttribute("recommendationResults", recommendationResults);
        model.addAttribute("applicants", applicants);
        model.addAttribute("comparisonSeed", List.of(recommendationResults.get(0), recommendationResults.get(1)));
        return "prototypes/recommendation/results";
    }

    @GetMapping("/results/{resultId}")
    public String detail(@PathVariable Long resultId, Model model) {
        List<Map<String, Object>> recommendationResults = recommendationResults();
        Map<String, Object> selectedResult = recommendationResults.stream()
                .filter(result -> ((Number) result.get("id")).longValue() == resultId)
                .findFirst()
                .orElse(recommendationResults.get(0));

        addPrototypeFrame(
                model,
                "detail",
                "추천 결과 상세",
                "한 명의 후보를 깊게 읽으면서 점수 근거, LLM 설명, 이력서 스냅샷, 인터뷰 질문까지 확인하는 상세 화면입니다.",
                "Prototype Detail",
                guide(
                        "클라이언트가 후보의 점수 근거와 이력서 핵심 내용을 검토한 뒤, 매칭 요청 여부를 결정하도록 돕습니다.",
                        List.of(
                                "selected recommendationResult 단건",
                                "freelancer 요약 정보",
                                "reasonFacts / llmStatus / llmReason",
                                "resume snapshot / portfolio / attachment / interviewQuestions"
                        ),
                        List.of(
                                "근거 / 이력서 / LLM 설명 탭 전환",
                                "추천 목록으로 돌아가기",
                                "비교 트레이로 보내기",
                                "매칭 요청 버튼 확인"
                        ),
                        List.of(
                                nextFlow("추천 결과 목록으로 복귀", "/prototypes/recommendations/results"),
                                nextFlow("실패 상태 예시 보기", "/prototypes/recommendations/error")
                        )
                ),
                "recommendationResultDetail",
                detailPayloadJson()
        );

        model.addAttribute("recommendationRun", recommendationRun());
        model.addAttribute("selectedResult", selectedResult);
        model.addAttribute("resumeSnapshot", selectedResult.get("resumeSnapshot"));
        return "prototypes/recommendation/detail";
    }

    @GetMapping("/empty")
    public String empty(Model model) {
        addPrototypeFrame(
                model,
                "empty",
                "결과 없음",
                "추천 실행은 끝났지만 노출 가능한 후보가 남지 않았을 때의 빈 결과 상태입니다.",
                "Prototype Empty",
                guide(
                        "왜 결과가 비었는지 설명하고, 사용자가 제안서 보정 또는 조건 완화를 통해 다음 액션을 취하게 만듭니다.",
                        List.of(
                                "recommendationRun 상태와 최종 candidateCount",
                                "hardFilterStats 요약",
                                "결과가 비게 된 주요 원인 리스트",
                                "대안 액션 CTA"
                        ),
                        List.of(
                                "제안서 수정하러 이동",
                                "필터 설명 확인",
                                "다시 추천 보기"
                        ),
                        List.of(
                                nextFlow("추천 플로우 처음으로", "/prototypes/recommendations"),
                                nextFlow("추천 결과 예시 보기", "/prototypes/recommendations/results")
                        )
                ),
                "recommendationEmptyState",
                emptyPayloadJson()
        );

        model.addAttribute("recommendationRun", emptyRun());
        model.addAttribute("emptyReasons", emptyReasons());
        model.addAttribute("fallbackActions", fallbackActions());
        return "prototypes/recommendation/empty";
    }

    @GetMapping("/error")
    public String error(Model model) {
        addPrototypeFrame(
                model,
                "error",
                "추천 실패 상태",
                "추천 실행 도중 실패했을 때 원인과 운영 액션을 명확히 보여주는 실패 상태 화면입니다.",
                "Prototype Error",
                guide(
                        "실패 사실을 숨기지 않고, 어떤 지점에서 실패했는지와 사용자가 취할 다음 액션을 분명하게 안내합니다.",
                        List.of(
                                "recommendationRun status / errorMessage",
                                "마지막 성공 실행 정보",
                                "재시도 가능 여부와 fallback 액션",
                                "운영 알림용 메타 정보"
                        ),
                        List.of(
                                "다시 시도",
                                "제안서 수정",
                                "마지막 성공 결과 보기"
                        ),
                        List.of(
                                nextFlow("진입 화면으로 돌아가기", "/prototypes/recommendations"),
                                nextFlow("결과 목록 예시 보기", "/prototypes/recommendations/results")
                        )
                ),
                "recommendationErrorState",
                errorPayloadJson()
        );

        model.addAttribute("errorState", errorState());
        model.addAttribute("lastSuccessfulRun", lastSuccessfulRun());
        return "prototypes/recommendation/error";
    }

    private void addPrototypeFrame(
            Model model,
            String activePrototypePage,
            String screenTitle,
            String screenSubtitle,
            String screenEyebrow,
            Map<String, Object> screenGuide,
            String mockPayloadName,
            String mockPayloadJson
    ) {
        model.addAttribute("prototypePages", prototypePages());
        model.addAttribute("activePrototypePage", activePrototypePage);
        model.addAttribute("screenTitle", screenTitle);
        model.addAttribute("screenSubtitle", screenSubtitle);
        model.addAttribute("screenEyebrow", screenEyebrow);
        model.addAttribute("screenGuide", screenGuide);
        model.addAttribute("mockPayloadName", mockPayloadName);
        model.addAttribute("mockPayloadJson", mockPayloadJson);
    }

    private List<Map<String, Object>> prototypePages() {
        return List.of(
                mapOf("key", "entry", "step", "01", "label", "진입", "href", "/prototypes/recommendations"),
                mapOf("key", "results", "step", "02", "label", "결과 목록", "href", "/prototypes/recommendations/results"),
                mapOf("key", "detail", "step", "03", "label", "상세", "href", "/prototypes/recommendations/results/101"),
                mapOf("key", "empty", "step", "04", "label", "결과 없음", "href", "/prototypes/recommendations/empty"),
                mapOf("key", "error", "step", "05", "label", "실패", "href", "/prototypes/recommendations/error")
        );
    }

    private Map<String, Object> entryOverview() {
        return mapOf(
                "runStatus", "MATCHING 준비 완료",
                "runStatusTone", "emerald",
                "headline", "AI 추천 실행 전 제안서 맥락과 후보 풀을 먼저 검토합니다.",
                "summary", "제안서 상태가 MATCHING 이고, 포지션별 후보 풀과 필터 조건이 유효하면 추천 결과 목록으로 바로 이동할 수 있습니다.",
                "quickMetrics", List.of(
                        metric("추천 대상 이력서", "48명", "공개 + AI 추천 허용 + ACTIVE"),
                        metric("이번 실행 topK", "3명", "클라이언트 비교 우선순위"),
                        metric("추천 알고리즘", "HEURISTIC_V1", "임베딩 + 스킬/경력 보정"),
                        metric("마지막 실행", "14분 전", "동일 fingerprint 기준")
                )
        );
    }

    private Map<String, Object> proposalSummary() {
        return mapOf(
                "proposalId", 38L,
                "proposalTitle", "AI 기반 프로젝트 매칭 플랫폼 구축",
                "proposalStatus", "MATCHING",
                "proposalStatusLabel", "매칭 진행",
                "clientName", "팀 IT-da",
                "lastUpdatedAt", "2026.04.16 10:18",
                "positionCount", 2,
                "budgetRange", "월 500만 - 900만 / 1인",
                "positions", List.of(
                        mapOf("value", "backend", "label", "백엔드 개발자", "headCount", 2, "requiredSkills", List.of("Java", "Spring Boot", "MySQL", "AWS")),
                        mapOf("value", "ai", "label", "AI 엔지니어", "headCount", 1, "requiredSkills", List.of("Python", "RAG", "LLM Ops"))
                )
        );
    }

    private List<Map<String, Object>> positionOptions() {
        return List.of(
                mapOf("value", "backend", "label", "백엔드 개발자"),
                mapOf("value", "ai", "label", "AI 엔지니어")
        );
    }

    private Map<String, Object> candidatePoolSummary() {
        return mapOf(
                "totalCount", 48,
                "hardFilterStats", mapOf(
                        "totalCandidates", 48,
                        "afterActiveFilter", 36,
                        "afterVisibilityFilter", 24,
                        "afterAiEnabledFilter", 17
                ),
                "segments", List.of(
                        metric("직접 지원자", "5명", "지원 완료 상태"),
                        metric("즉시 투입 가능", "11명", "활성 프로젝트 0건"),
                        metric("추천 설명 완료", "3명", "LLM READY")
                )
        );
    }

    private List<Map<String, Object>> readinessChecklist() {
        return List.of(
                checklist("제안서 상태가 MATCHING 인가?", true, "WRITING 상태에서는 추천 실행을 막고 helper text만 노출"),
                checklist("포지션별 필수 스킬이 구조화되었는가?", true, "추천 근거 카드의 matchedSkills 계산에 필요"),
                checklist("AI 추천 허용 이력서가 충분한가?", true, "현재 17명"),
                checklist("추천 실패 fallback 이 준비되었는가?", true, "실패 상태 화면에서 재시도 및 제안서 수정 CTA 제공")
        );
    }

    private Map<String, Object> recommendationRun() {
        return mapOf(
                "id", 2201L,
                "proposalPositionId", 3802L,
                "proposalTitle", "AI 기반 프로젝트 매칭 플랫폼 구축",
                "positionLabel", "백엔드 개발자",
                "requestFingerprint", "fp-demo-20260416-backend-v1",
                "algorithm", "HEURISTIC_V1",
                "status", "COMPUTED",
                "statusLabel", "계산 완료",
                "statusTone", "emerald",
                "requestedAt", "2026.04.16 10:30",
                "completedAt", "2026.04.16 10:31",
                "topK", 3,
                "candidateCount", 17,
                "hardFilterStats", mapOf(
                        "totalCandidates", 48,
                        "afterActiveFilter", 36,
                        "afterVisibilityFilter", 24,
                        "afterAiEnabledFilter", 17
                )
        );
    }

    private List<Map<String, Object>> sortOptions() {
        return List.of(
                mapOf("value", "score", "label", "추천 점수순"),
                mapOf("value", "career", "label", "경력 많은 순"),
                mapOf("value", "recent", "label", "최근 활동순")
        );
    }

    private List<Map<String, Object>> recommendationResults() {
        return List.of(
                recommendationCandidate(
                        101L,
                        1,
                        "코드메이븐",
                        "Spring Boot · 추천 플랫폼 7년",
                        94,
                        91,
                        92,
                        "available",
                        "즉시 투입 가능",
                        "진행 중 프로젝트 0건",
                        7,
                        "REMOTE",
                        "원격 선호",
                        "READY",
                        "LLM 설명 완료",
                        "제안서 핵심 스택과 경력 요건을 가장 안정적으로 충족합니다.",
                        "추천 API 설계, 매칭 서비스 운영, AWS 인프라 경험이 동시에 확인됩니다.",
                        List.of("추천 API 재학습 없이 품질을 개선한 경험을 설명해 주세요.", "Spring Boot 기반 비동기 처리 전략을 어떻게 가져가시겠습니까?", "현재 프로젝트에서 가장 큰 리스크를 무엇으로 보시나요?"),
                        List.of("Java", "Spring Boot", "MySQL", "AWS"),
                        List.of("플랫폼", "AI 매칭"),
                        List.of("대규모 매칭 API 설계", "추천 품질 실험 경험", "클라우드 비용 최적화"),
                        List.of(skill("Java", "상"), skill("Spring Boot", "상"), skill("AWS", "중")),
                        "포트폴리오와 기술 블로그를 함께 운영 중",
                        "/prototypes/recommendations/results/101"
                ),
                recommendationCandidate(
                        102L,
                        2,
                        "배포장인",
                        "백엔드 · AWS DevOps 6년",
                        89,
                        86,
                        88,
                        "busy",
                        "2주 후 가능",
                        "진행 중 프로젝트 1건",
                        6,
                        "HYBRID",
                        "하이브리드 선호",
                        "PENDING",
                        "LLM 설명 대기",
                        "인프라 비용과 운영 안정성 측면에서 강점이 큽니다.",
                        "LLM 설명이 아직 생성되지 않았습니다. reasonFacts 기반으로 먼저 검토하는 흐름을 노출합니다.",
                        List.of("배포 자동화 파이프라인에서 가장 중요하게 보는 품질 지표는 무엇인가요?", "현재 프로젝트에 맞는 모니터링 체계를 어떻게 설계하시겠습니까?"),
                        List.of("Java", "AWS", "Docker", "MySQL"),
                        List.of("플랫폼", "운영 자동화"),
                        List.of("CI/CD 표준화", "장애 대응 프로세스 수립", "월 비용 18% 절감"),
                        List.of(skill("AWS", "상"), skill("Docker", "상"), skill("Spring Boot", "중")),
                        "인프라 안정화와 백엔드 협업 경험이 풍부",
                        "/prototypes/recommendations/results/102"
                ),
                recommendationCandidate(
                        103L,
                        3,
                        "리팩터러",
                        "도메인 모델링 · API 설계 5년",
                        84,
                        82,
                        85,
                        "available",
                        "즉시 투입 가능",
                        "진행 중 프로젝트 0건",
                        5,
                        "SITE",
                        "상주 선호",
                        "FAILED",
                        "LLM 설명 실패",
                        "도메인 모델링과 백오피스 설계 경험이 강합니다.",
                        "LLM 생성 실패 상태이므로 reasonFacts와 수동 코멘트를 우선 노출합니다.",
                        List.of("복잡한 요구사항을 도메인 모델로 정리할 때 우선순위를 어떻게 두시나요?", "상태 전이 설계에서 가장 자주 발생하는 실수는 무엇인가요?"),
                        List.of("Java", "DDD", "REST API", "Querydsl"),
                        List.of("B2B SaaS", "백오피스"),
                        List.of("도메인 경계 재정의", "API 명세 정비", "레거시 리팩터링"),
                        List.of(skill("DDD", "상"), skill("Java", "상"), skill("Querydsl", "중")),
                        "복잡한 도메인 요구사항 정리에 강점",
                        "/prototypes/recommendations/results/103"
                )
        );
    }

    private List<Map<String, Object>> applicants() {
        return List.of(
                applicantCandidate(201L, "지원러", "2026.04.16 09:42", 4, "검토 전", "default", List.of("Java", "Spring"), "REMOTE", "원격 선호"),
                applicantCandidate(202L, "서버웨이브", "2026.04.16 08:10", 8, "비교 중", "blue", List.of("Java", "Kotlin", "AWS"), "HYBRID", "하이브리드"),
                applicantCandidate(203L, "풀스택준", "2026.04.15 21:03", 5, "보류", "amber", List.of("Spring", "React", "MySQL"), "SITE", "상주 선호")
        );
    }

    private Map<String, Object> emptyRun() {
        return mapOf(
                "id", 2202L,
                "status", "COMPUTED",
                "statusLabel", "계산 완료",
                "statusTone", "slate",
                "candidateCount", 0,
                "positionLabel", "AI 엔지니어",
                "requestFingerprint", "fp-demo-20260416-ai-v1",
                "hardFilterStats", mapOf(
                        "totalCandidates", 21,
                        "afterActiveFilter", 15,
                        "afterVisibilityFilter", 4,
                        "afterAiEnabledFilter", 0
                )
        );
    }

    private List<Map<String, Object>> emptyReasons() {
        return List.of(
                mapOf("title", "AI 추천 허용 이력서 없음", "detail", "후보 4명 모두 publiclyVisible 은 true 이지만 aiMatchingEnabled 가 false 입니다."),
                mapOf("title", "포지션 스택이 과도하게 좁음", "detail", "Python + RAG + LLM Ops + 온사이트 조합으로 매칭 후보가 줄었습니다."),
                mapOf("title", "최근 활동 이력 부족", "detail", "활성 상태 기준을 통과한 후보도 포지션 적합성이 낮았습니다.")
        );
    }

    private List<Map<String, Object>> fallbackActions() {
        return List.of(
                nextFlow("제안서 조건 완화하기", "/prototypes/recommendations"),
                nextFlow("백엔드 포지션 결과 예시 보기", "/prototypes/recommendations/results"),
                nextFlow("프리랜서 공개 / AI 허용 정책 점검", "/prototypes/recommendations")
        );
    }

    private Map<String, Object> errorState() {
        return mapOf(
                "id", 2203L,
                "status", "FAILED",
                "statusLabel", "실패",
                "statusTone", "rose",
                "positionLabel", "백엔드 개발자",
                "errorCode", "LLM_TIMEOUT",
                "errorMessage", "추천 설명 생성 단계에서 응답 제한 시간을 초과했습니다.",
                "requestedAt", "2026.04.16 10:44",
                "retryable", true,
                "operatorHint", "추천 점수 산출은 완료되었고, LLM 설명 생성만 실패한 상태를 가정한 mock 입니다."
        );
    }

    private Map<String, Object> lastSuccessfulRun() {
        return mapOf(
                "id", 2198L,
                "completedAt", "2026.04.16 10:31",
                "candidateCount", 17,
                "topCandidate", "코드메이븐",
                "detailHref", "/prototypes/recommendations/results"
        );
    }

    private Map<String, Object> guide(
            String purpose,
            List<String> displayData,
            List<String> actions,
            List<Map<String, Object>> nextFlows
    ) {
        return mapOf(
                "purpose", purpose,
                "displayData", displayData,
                "actions", actions,
                "nextFlows", nextFlows
        );
    }

    private Map<String, Object> nextFlow(String label, String href) {
        return mapOf("label", label, "href", href);
    }

    private Map<String, Object> metric(String label, String value, String helper) {
        return mapOf("label", label, "value", value, "helper", helper);
    }

    private Map<String, Object> checklist(String label, boolean checked, String helper) {
        return mapOf("label", label, "checked", checked, "helper", helper);
    }

    private Map<String, Object> skill(String name, String level) {
        return mapOf("name", name, "level", level);
    }

    private Map<String, Object> recommendationCandidate(
            Long id,
            int rank,
            String nickname,
            String headline,
            int finalScore,
            int embeddingScore,
            int matchRate,
            String availabilityKey,
            String availabilityLabel,
            String activeProjects,
            int careerYears,
            String preferredWorkType,
            String preferredWorkTypeLabel,
            String llmStatus,
            String llmStatusLabel,
            String aiComment,
            String llmReason,
            List<String> interviewQuestions,
            List<String> matchedSkills,
            List<String> matchedDomains,
            List<String> highlights,
            List<Map<String, Object>> skills,
            String summaryNote,
            String detailHref
    ) {
        return mapOf(
                "id", id,
                "rank", rank,
                "nickname", nickname,
                "headline", headline,
                "finalScore", finalScore,
                "embeddingScore", embeddingScore,
                "matchRate", matchRate,
                "availabilityKey", availabilityKey,
                "availabilityLabel", availabilityLabel,
                "activeProjects", activeProjects,
                "careerYears", careerYears,
                "preferredWorkType", preferredWorkType,
                "preferredWorkTypeLabel", preferredWorkTypeLabel,
                "llmStatus", llmStatus,
                "llmStatusLabel", llmStatusLabel,
                "aiComment", aiComment,
                "llmReason", llmReason,
                "interviewQuestions", interviewQuestions,
                "matchedSkills", matchedSkills,
                "matchedDomains", matchedDomains,
                "highlights", highlights,
                "skills", skills,
                "summaryNote", summaryNote,
                "detailHref", detailHref,
                "resumeSnapshot", mapOf(
                        "introduction", "추천 플랫폼과 B2B SaaS 백엔드 설계를 주로 맡아 왔고, 최근에는 추천 품질 실험과 운영 자동화에도 참여했습니다.",
                        "portfolioUrl", "https://portfolio.itda.dev/" + id,
                        "attachments", List.of("프로젝트_요약.pdf", "성과지표_정리.pdf"),
                        "publiclyVisible", true,
                        "aiMatchingEnabled", true
                )
        );
    }

    private Map<String, Object> applicantCandidate(
            Long id,
            String nickname,
            String appliedAt,
            int careerYears,
            String reviewStatus,
            String reviewTone,
            List<String> skills,
            String preferredWorkType,
            String preferredWorkTypeLabel
    ) {
        return mapOf(
                "id", id,
                "nickname", nickname,
                "appliedAt", appliedAt,
                "careerYears", careerYears,
                "reviewStatus", reviewStatus,
                "reviewTone", reviewTone,
                "skills", skills,
                "preferredWorkType", preferredWorkType,
                "preferredWorkTypeLabel", preferredWorkTypeLabel
        );
    }

    private String entryPayloadJson() {
        return """
                {
                  "recommendationRun": {
                    "proposalPositionId": 3802,
                    "requestFingerprint": "fp-demo-20260416-backend-v1",
                    "algorithm": "HEURISTIC_V1",
                    "topK": 3,
                    "status": "COMPUTED"
                  },
                  "proposal": {
                    "id": 38,
                    "title": "AI 기반 프로젝트 매칭 플랫폼 구축",
                    "status": "MATCHING"
                  },
                  "proposalPositions": [
                    {
                      "id": 3802,
                      "positionLabel": "백엔드 개발자",
                      "requiredSkills": ["Java", "Spring Boot", "MySQL", "AWS"],
                      "headCount": 2,
                      "budgetRange": "500-900"
                    }
                  ],
                  "candidatePoolSummary": {
                    "totalCandidates": 48,
                    "afterActiveFilter": 36,
                    "afterVisibilityFilter": 24,
                    "afterAiEnabledFilter": 17
                  }
                }
                """;
    }

    private String resultsPayloadJson() {
        return """
                {
                  "recommendationRun": {
                    "id": 2201,
                    "status": "COMPUTED",
                    "candidateCount": 17,
                    "topK": 3,
                    "hardFilterStats": {
                      "totalCandidates": 48,
                      "afterActiveFilter": 36,
                      "afterVisibilityFilter": 24,
                      "afterAiEnabledFilter": 17
                    }
                  },
                  "recommendationResults": [
                    {
                      "id": 101,
                      "rank": 1,
                      "nickname": "코드메이븐",
                      "finalScore": 0.94,
                      "embeddingScore": 0.91,
                      "llmStatus": "READY",
                      "matchedSkills": ["Java", "Spring Boot", "MySQL", "AWS"],
                      "matchedDomains": ["플랫폼", "AI 매칭"],
                      "highlights": ["대규모 매칭 API 설계", "추천 품질 실험 경험"],
                      "interviewQuestions": ["품질 개선 경험", "비동기 처리 전략"]
                    }
                  ],
                  "applicants": [
                    {
                      "id": 201,
                      "nickname": "지원러",
                      "appliedAt": "2026.04.16 09:42",
                      "careerYears": 4,
                      "reviewStatus": "검토 전"
                    }
                  ]
                }
                """;
    }

    private String detailPayloadJson() {
        return """
                {
                  "recommendationResult": {
                    "id": 101,
                    "rank": 1,
                    "finalScore": 0.94,
                    "embeddingScore": 0.91,
                    "llmStatus": "READY",
                    "llmReason": "추천 API 설계, 매칭 서비스 운영, AWS 인프라 경험이 동시에 확인됩니다.",
                    "reasonFacts": {
                      "matchedSkills": ["Java", "Spring Boot", "MySQL", "AWS"],
                      "matchedDomains": ["플랫폼", "AI 매칭"],
                      "careerYears": 7,
                      "highlights": ["추천 품질 실험 경험", "클라우드 비용 최적화"]
                    }
                  },
                  "resumeSnapshot": {
                    "publiclyVisible": true,
                    "aiMatchingEnabled": true,
                    "portfolioUrl": "https://portfolio.itda.dev/101",
                    "attachments": ["프로젝트_요약.pdf", "성과지표_정리.pdf"]
                  }
                }
                """;
    }

    private String emptyPayloadJson() {
        return """
                {
                  "recommendationRun": {
                    "id": 2202,
                    "status": "COMPUTED",
                    "candidateCount": 0
                  },
                  "hardFilterStats": {
                    "totalCandidates": 21,
                    "afterActiveFilter": 15,
                    "afterVisibilityFilter": 4,
                    "afterAiEnabledFilter": 0
                  },
                  "emptyReasons": [
                    "AI 추천 허용 이력서 없음",
                    "포지션 스택이 과도하게 좁음",
                    "최근 활동 이력 부족"
                  ]
                }
                """;
    }

    private String errorPayloadJson() {
        return """
                {
                  "recommendationRun": {
                    "id": 2203,
                    "status": "FAILED",
                    "errorMessage": "추천 설명 생성 단계에서 응답 제한 시간을 초과했습니다."
                  },
                  "lastSuccessfulRun": {
                    "id": 2198,
                    "candidateCount": 17,
                    "topCandidate": "코드메이븐"
                  }
                }
                """;
    }

    private static Map<String, Object> mapOf(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("짝수 개수의 key/value 가 필요합니다.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            result.put((String) keyValues[i], keyValues[i + 1]);
        }
        return result;
    }
}
