package com.generic4.itda.config;

import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkill;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.domain.recommendation.RecommendationResult;
import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.constant.RecommendationAlgorithm;
import com.generic4.itda.domain.recommendation.vo.HardFilterStat;
import com.generic4.itda.domain.recommendation.vo.ReasonFacts;
import com.generic4.itda.domain.resume.CareerEmploymentType;
import com.generic4.itda.domain.resume.CareerItemPayload;
import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Proficiency;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeSkill;
import com.generic4.itda.domain.resume.ResumeStatus;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.repository.MatchingRepository;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.repository.PositionRepository;
import com.generic4.itda.repository.ProposalRepository;
import com.generic4.itda.repository.RecommendationResultRepository;
import com.generic4.itda.repository.RecommendationRunRepository;
import com.generic4.itda.repository.ResumeRepository;
import com.generic4.itda.repository.SkillRepository;
import com.generic4.itda.service.ResumeEmbeddingService;
import com.generic4.itda.service.recommend.RecommendationFingerprintGenerator;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@Profile("!test")
@Transactional
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class SeedDataInitializer implements ApplicationRunner {

    private static final String SEED_CLIENT_EMAIL = "seed.client@itda.local";
    private static final String SEED_BACKEND_EMAIL = "seed.backend@itda.local";
    private static final String SEED_FULLSTACK_EMAIL = "seed.fullstack@itda.local";
    private static final String SEED_AI_EMAIL = "seed.ai@itda.local";
    private static final String SEED_PLATFORM_BACKEND_EMAIL = "seed.platform@itda.local";
    private static final String SEED_DATA_BACKEND_EMAIL = "seed.data@itda.local";
    private static final String SEED_FRONTEND_EMAIL = "seed.frontend@itda.local";
    private static final String SEED_DESIGN_SYSTEM_EMAIL = "seed.design@itda.local";
    private static final String SEED_MLOPS_EMAIL = "seed.mlops@itda.local";
    private static final String SEED_AI_PRODUCT_EMAIL = "seed.ai-product@itda.local";
    private static final String SEED_JUNIOR_BACKEND_EMAIL = "seed.junior@itda.local";
    private static final String SEED_SITE_ONLY_BACKEND_EMAIL = "seed.siteonly@itda.local";
    private static final String SEED_OPTOUT_EMAIL = "seed.optout@itda.local";
    private static final String SEED_HIDDEN_EMAIL = "seed.hidden@itda.local";

    private static final String MATCHING_PROPOSAL_TITLE = "[SEED] AI 프리랜서 추천 플랫폼 고도화";
    private static final String WRITING_PROPOSAL_TITLE = "[SEED] 관리자 대시보드 프론트 개편";

    private final MemberRepository memberRepository;
    private final PositionRepository positionRepository;
    private final SkillRepository skillRepository;
    private final ResumeRepository resumeRepository;
    private final ProposalRepository proposalRepository;
    private final MatchingRepository matchingRepository;
    private final RecommendationRunRepository recommendationRunRepository;
    private final RecommendationResultRepository recommendationResultRepository;
    private final RecommendationFingerprintGenerator recommendationFingerprintGenerator;
    private final ResumeEmbeddingService resumeEmbeddingService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.default-password:demo1234}")
    private String defaultPassword;

    @Override
    public void run(ApplicationArguments args) {
        Map<String, Position> positions = ensurePositions();
        Map<String, Skill> skills = ensureSkills();

        Member client = ensureMember(SEED_CLIENT_EMAIL, "시드 클라이언트", "추천 테스트용 발주자", "010-9000-1000");
        Member backend = ensureMember(SEED_BACKEND_EMAIL, "김백엔드", "백엔드 시드", "010-9000-1001");
        Member fullstack = ensureMember(SEED_FULLSTACK_EMAIL, "이풀스택", "풀스택 시드", "010-9000-1002");
        Member aiEngineer = ensureMember(SEED_AI_EMAIL, "박에이아이", "AI 시드", "010-9000-1003");
        Member platformBackend = ensureMember(SEED_PLATFORM_BACKEND_EMAIL, "정플랫폼", "플랫폼 백엔드 시드", "010-9000-1004");
        Member dataBackend = ensureMember(SEED_DATA_BACKEND_EMAIL, "오데이터", "데이터 백엔드 시드", "010-9000-1005");
        Member frontend = ensureMember(SEED_FRONTEND_EMAIL, "한프론트", "프론트 시드", "010-9000-1006");
        Member designSystem = ensureMember(SEED_DESIGN_SYSTEM_EMAIL, "윤디자인", "디자인 시스템 시드", "010-9000-1007");
        Member mlops = ensureMember(SEED_MLOPS_EMAIL, "임엠엘옵스", "MLOps 시드", "010-9000-1008");
        Member aiProduct = ensureMember(SEED_AI_PRODUCT_EMAIL, "서에이아이", "AI 제품화 시드", "010-9000-1009");
        Member juniorBackend = ensureMember(SEED_JUNIOR_BACKEND_EMAIL, "신주니어", "주니어 백엔드 시드", "010-9000-1010");
        Member siteOnlyBackend = ensureMember(SEED_SITE_ONLY_BACKEND_EMAIL, "조온사이트", "온사이트 백엔드 시드", "010-9000-1011");
        Member optOut = ensureMember(SEED_OPTOUT_EMAIL, "권옵트아웃", "옵트아웃 시드", "010-9000-1012");
        Member hidden = ensureMember(SEED_HIDDEN_EMAIL, "최히든", "비공개 시드", "010-9000-1013");

        Resume backendResume = ensureResume(
                backend,
                """
                추천/매칭과 B2B SaaS 도메인에서 Java/Spring 기반 API, 배치, 캐시 전략을 설계해 온 백엔드 개발자입니다.
                운영 장애 분석, 데이터 모델링, 성능 튜닝을 함께 맡으며 트래픽이 큰 서비스의 안정성을 높여왔습니다.
                """,
                (byte) 6,
                careerPayload(
                        careerItem(
                                "ITDA Labs",
                                "시니어 백엔드 개발자",
                                CareerEmploymentType.FULL_TIME,
                                "2023-01",
                                null,
                                true,
                                "추천/매칭 API와 운영용 백오피스를 설계하고, Redis 캐시 전략과 Querydsl 기반 검색 성능 개선을 주도했습니다.",
                                List.of("Java", "Spring Boot", "PostgreSQL", "Redis", "Querydsl", "Docker")
                        ),
                        careerItem(
                                "B2B Cloud",
                                "백엔드 개발자",
                                CareerEmploymentType.FULL_TIME,
                                "2020-04",
                                "2022-12",
                                false,
                                "정산/권한 관리 도메인 API와 배치를 개발하고 GitHub Actions 기반 배포 파이프라인을 운영했습니다.",
                                List.of("Java", "Spring", "JPA", "MySQL", "GitHub Actions", "Linux")
                        ),
                        careerItem(
                                "Data Commerce",
                                "백엔드 엔지니어",
                                CareerEmploymentType.FULL_TIME,
                                "2018-03",
                                "2020-03",
                                false,
                                "주문/회원 서비스를 마이그레이션하면서 PostgreSQL 스키마 개선과 REST API 표준화를 담당했습니다.",
                                List.of("Java", "Spring Boot", "PostgreSQL", "REST API")
                        )
                ),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "https://backend.seed.itda.local",
                true,
                true,
                skillLevels(
                        skillLevel("Java", Proficiency.ADVANCED),
                        skillLevel("Spring", Proficiency.ADVANCED),
                        skillLevel("Spring Boot", Proficiency.ADVANCED),
                        skillLevel("PostgreSQL", Proficiency.ADVANCED),
                        skillLevel("Redis", Proficiency.ADVANCED),
                        skillLevel("JPA", Proficiency.ADVANCED),
                        skillLevel("Querydsl", Proficiency.INTERMEDIATE),
                        skillLevel("REST API", Proficiency.ADVANCED),
                        skillLevel("Docker", Proficiency.INTERMEDIATE),
                        skillLevel("GitHub Actions", Proficiency.INTERMEDIATE),
                        skillLevel("Linux", Proficiency.INTERMEDIATE)
                ),
                skills
        );

        Resume fullstackResume = ensureResume(
                fullstack,
                """
                관리자 백오피스, 고객용 웹, API 서버를 함께 개발해 온 풀스택 개발자입니다.
                프론트엔드 생산성과 백엔드 운영을 같이 챙기며 초기 제품의 기능 실험과 운영 고도화에 익숙합니다.
                """,
                (byte) 5,
                careerPayload(
                        careerItem(
                                "Product Studio",
                                "풀스택 리드",
                                CareerEmploymentType.FULL_TIME,
                                "2022-05",
                                null,
                                true,
                                "운영 대시보드와 고객용 웹을 함께 개발하면서 React/TypeScript 프론트와 Spring API 서버를 동시에 운영했습니다.",
                                List.of("React", "TypeScript", "Next.js", "Java", "Spring", "Docker")
                        ),
                        careerItem(
                                "Commerce Tools",
                                "프론트엔드 개발자",
                                CareerEmploymentType.FULL_TIME,
                                "2020-01",
                                "2022-04",
                                false,
                                "관리자 화면 IA를 개편하고 공통 컴포넌트 및 배포 파이프라인을 정리했습니다.",
                                List.of("React", "TypeScript", "Figma", "Docker")
                        ),
                        careerItem(
                                "Agency Works",
                                "웹 개발자",
                                CareerEmploymentType.FREELANCE,
                                "2019-01",
                                "2019-12",
                                false,
                                "다수의 기업 랜딩 페이지와 예약/문의 폼을 구축하며 프론트엔드와 간단한 백엔드 연동을 담당했습니다.",
                                List.of("JavaScript", "HTML", "CSS", "REST API")
                        )
                ),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "https://fullstack.seed.itda.local",
                true,
                true,
                skillLevels(
                        skillLevel("Java", Proficiency.INTERMEDIATE),
                        skillLevel("Spring", Proficiency.INTERMEDIATE),
                        skillLevel("React", Proficiency.ADVANCED),
                        skillLevel("TypeScript", Proficiency.ADVANCED),
                        skillLevel("Next.js", Proficiency.INTERMEDIATE),
                        skillLevel("JavaScript", Proficiency.ADVANCED),
                        skillLevel("HTML", Proficiency.ADVANCED),
                        skillLevel("CSS", Proficiency.INTERMEDIATE),
                        skillLevel("Docker", Proficiency.INTERMEDIATE),
                        skillLevel("REST API", Proficiency.INTERMEDIATE),
                        skillLevel("Figma", Proficiency.BEGINNER)
                ),
                skills
        );

        Resume aiResume = ensureResume(
                aiEngineer,
                """
                LLM 기반 추천, RAG, 추론 API 운영을 맡아 온 AI 엔지니어입니다.
                프롬프트 실험 설계부터 FastAPI 배포, 로그 수집, 평가 지표 설계까지 제품화 흐름을 경험했습니다.
                """,
                (byte) 4,
                careerPayload(
                        careerItem(
                                "AI Matching Team",
                                "AI 엔지니어",
                                CareerEmploymentType.CONTRACT,
                                "2023-02",
                                null,
                                true,
                                "추천 설명 생성, 검색 증강, 실험 로그 적재 파이프라인을 구현하고 추론 서버를 운영했습니다.",
                                List.of("Python", "FastAPI", "LLM", "LangChain", "AWS", "PostgreSQL")
                        ),
                        careerItem(
                                "Data Insight Lab",
                                "ML 엔지니어",
                                CareerEmploymentType.FULL_TIME,
                                "2021-01",
                                "2023-01",
                                false,
                                "문서 분류 모델과 임베딩 검색 기능을 구축하고 PyTorch 실험 코드를 서비스 배치로 이관했습니다.",
                                List.of("Python", "PyTorch", "PostgreSQL", "Docker")
                        )
                ),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "https://ai.seed.itda.local",
                true,
                true,
                skillLevels(
                        skillLevel("Python", Proficiency.ADVANCED),
                        skillLevel("FastAPI", Proficiency.ADVANCED),
                        skillLevel("LLM", Proficiency.ADVANCED),
                        skillLevel("LangChain", Proficiency.INTERMEDIATE),
                        skillLevel("PostgreSQL", Proficiency.INTERMEDIATE),
                        skillLevel("AWS", Proficiency.INTERMEDIATE),
                        skillLevel("Docker", Proficiency.INTERMEDIATE),
                        skillLevel("PyTorch", Proficiency.INTERMEDIATE)
                ),
                skills
        );

        Resume platformBackendResume = ensureResume(
                platformBackend,
                """
                플랫폼/인프라와 서비스 백엔드를 함께 맡아 온 하이브리드형 백엔드 개발자입니다.
                추천 API처럼 읽기 부하가 높은 서비스에서 캐시, 메시징, 배포 자동화를 설계하는 일을 주로 해왔습니다.
                """,
                (byte) 8,
                careerPayload(
                        careerItem(
                                "Platform Works",
                                "플랫폼 백엔드 리드",
                                CareerEmploymentType.FULL_TIME,
                                "2022-06",
                                null,
                                true,
                                "대용량 트래픽 API의 캐시 계층과 Kafka 기반 비동기 처리를 설계하고 배포 자동화를 운영했습니다.",
                                List.of("Java", "Spring Boot", "Redis", "Kafka", "Docker", "Kubernetes")
                        ),
                        careerItem(
                                "Cloud Ops Studio",
                                "백엔드 엔지니어",
                                CareerEmploymentType.FULL_TIME,
                                "2019-05",
                                "2022-05",
                                false,
                                "멀티 테넌트 SaaS의 인증/권한 API와 PostgreSQL 튜닝, CI/CD 파이프라인 개선을 담당했습니다.",
                                List.of("Java", "Spring", "PostgreSQL", "GitHub Actions", "Linux")
                        ),
                        careerItem(
                                "Retail Data Hub",
                                "서버 개발자",
                                CareerEmploymentType.FULL_TIME,
                                "2017-01",
                                "2019-04",
                                false,
                                "상품/재고 동기화 배치와 검색 연동 기능을 구축했습니다.",
                                List.of("Java", "Spring Boot", "MySQL", "Elasticsearch")
                        )
                ),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "https://platform.seed.itda.local",
                true,
                true,
                skillLevels(
                        skillLevel("Java", Proficiency.ADVANCED),
                        skillLevel("Spring", Proficiency.ADVANCED),
                        skillLevel("Spring Boot", Proficiency.ADVANCED),
                        skillLevel("PostgreSQL", Proficiency.ADVANCED),
                        skillLevel("Redis", Proficiency.ADVANCED),
                        skillLevel("Kafka", Proficiency.INTERMEDIATE),
                        skillLevel("Docker", Proficiency.ADVANCED),
                        skillLevel("Kubernetes", Proficiency.INTERMEDIATE),
                        skillLevel("GitHub Actions", Proficiency.INTERMEDIATE),
                        skillLevel("Linux", Proficiency.ADVANCED)
                ),
                skills
        );

        ensureResume(
                dataBackend,
                """
                데이터 집약형 서비스에서 조회 성능, 검색 품질, 배치 처리까지 담당해 온 백엔드 개발자입니다.
                SQL 최적화와 Elasticsearch 연동, Querydsl 기반 검색 기능 구현 경험이 강점입니다.
                """,
                (byte) 5,
                careerPayload(
                        careerItem(
                                "Search Commerce",
                                "백엔드 개발자",
                                CareerEmploymentType.FULL_TIME,
                                "2022-01",
                                null,
                                true,
                                "추천/검색 API를 개발하며 Elasticsearch 색인 구조와 PostgreSQL 조회 성능을 함께 튜닝했습니다.",
                                List.of("Java", "Spring Boot", "PostgreSQL", "Elasticsearch", "Querydsl")
                        ),
                        careerItem(
                                "Growth Data Team",
                                "데이터 백엔드 엔지니어",
                                CareerEmploymentType.FULL_TIME,
                                "2020-03",
                                "2021-12",
                                false,
                                "로그 적재/정제 배치와 운영 대시보드용 API를 개발했습니다.",
                                List.of("Java", "Spring", "Kafka", "JPA", "REST API")
                        )
                ),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "https://data.seed.itda.local",
                true,
                true,
                skillLevels(
                        skillLevel("Java", Proficiency.ADVANCED),
                        skillLevel("Spring", Proficiency.ADVANCED),
                        skillLevel("Spring Boot", Proficiency.INTERMEDIATE),
                        skillLevel("PostgreSQL", Proficiency.ADVANCED),
                        skillLevel("Elasticsearch", Proficiency.INTERMEDIATE),
                        skillLevel("Querydsl", Proficiency.INTERMEDIATE),
                        skillLevel("JPA", Proficiency.INTERMEDIATE),
                        skillLevel("Kafka", Proficiency.INTERMEDIATE),
                        skillLevel("REST API", Proficiency.ADVANCED)
                ),
                skills
        );

        ensureResume(
                frontend,
                """
                React/TypeScript 기반 운영 화면과 고객용 웹을 주로 구축해 온 프론트엔드 개발자입니다.
                복잡한 폼, 데이터 테이블, 디자인 시스템 정리와 같은 운영자 경험 개선 작업에 강점이 있습니다.
                """,
                (byte) 4,
                careerPayload(
                        careerItem(
                                "Ops Dashboard Team",
                                "프론트엔드 개발자",
                                CareerEmploymentType.FULL_TIME,
                                "2023-01",
                                null,
                                true,
                                "운영자용 대시보드의 필터/리스트/상세 흐름을 재구성하고 반응형 UI를 개선했습니다.",
                                List.of("React", "TypeScript", "Next.js", "Tailwind CSS", "Figma")
                        ),
                        careerItem(
                                "Design Sprint Lab",
                                "웹 퍼블리셔 겸 프론트엔드 개발자",
                                CareerEmploymentType.FULL_TIME,
                                "2021-01",
                                "2022-12",
                                false,
                                "디자인 시안을 기준으로 공통 레이아웃과 폼 컴포넌트를 구현했습니다.",
                                List.of("HTML", "CSS", "JavaScript", "React")
                        )
                ),
                WorkType.REMOTE,
                ResumeWritingStatus.DONE,
                "https://frontend.seed.itda.local",
                true,
                true,
                skillLevels(
                        skillLevel("React", Proficiency.ADVANCED),
                        skillLevel("TypeScript", Proficiency.ADVANCED),
                        skillLevel("Next.js", Proficiency.ADVANCED),
                        skillLevel("JavaScript", Proficiency.ADVANCED),
                        skillLevel("HTML", Proficiency.ADVANCED),
                        skillLevel("CSS", Proficiency.ADVANCED),
                        skillLevel("Tailwind CSS", Proficiency.INTERMEDIATE),
                        skillLevel("Figma", Proficiency.INTERMEDIATE),
                        skillLevel("Docker", Proficiency.BEGINNER)
                ),
                skills
        );

        ensureResume(
                designSystem,
                """
                프론트엔드와 디자인 시스템 구축을 함께 경험한 UI 플랫폼 개발자입니다.
                운영 화면의 일관성을 높이기 위한 공통 컴포넌트와 토큰 관리, QA 협업 흐름에 익숙합니다.
                """,
                (byte) 6,
                careerPayload(
                        careerItem(
                                "Design Platform",
                                "프론트엔드 플랫폼 개발자",
                                CareerEmploymentType.FULL_TIME,
                                "2022-02",
                                null,
                                true,
                                "관리자/고객용 화면에서 재사용하는 디자인 토큰과 공통 React 컴포넌트를 구축했습니다.",
                                List.of("React", "TypeScript", "Figma", "Tailwind CSS", "Docker")
                        ),
                        careerItem(
                                "Brand Commerce",
                                "프론트엔드 개발자",
                                CareerEmploymentType.FULL_TIME,
                                "2019-11",
                                "2022-01",
                                false,
                                "Next.js 기반 커머스 프론트와 백오피스를 함께 개발했습니다.",
                                List.of("React", "Next.js", "TypeScript", "JavaScript")
                        )
                ),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "https://design.seed.itda.local",
                true,
                true,
                skillLevels(
                        skillLevel("React", Proficiency.ADVANCED),
                        skillLevel("TypeScript", Proficiency.ADVANCED),
                        skillLevel("Next.js", Proficiency.INTERMEDIATE),
                        skillLevel("JavaScript", Proficiency.ADVANCED),
                        skillLevel("Tailwind CSS", Proficiency.ADVANCED),
                        skillLevel("Figma", Proficiency.ADVANCED),
                        skillLevel("HTML", Proficiency.INTERMEDIATE),
                        skillLevel("CSS", Proficiency.INTERMEDIATE),
                        skillLevel("Docker", Proficiency.INTERMEDIATE)
                ),
                skills
        );

        ensureResume(
                mlops,
                """
                LLM 추론 서비스와 운영 자동화를 함께 맡아 온 MLOps 성향의 AI 엔지니어입니다.
                FastAPI 추론 서버, 실험 로그 파이프라인, 배포 자동화를 연결해 모델 기능을 서비스로 안착시키는 일을 해왔습니다.
                """,
                (byte) 6,
                careerPayload(
                        careerItem(
                                "Inference Ops",
                                "AI 플랫폼 엔지니어",
                                CareerEmploymentType.FULL_TIME,
                                "2022-04",
                                null,
                                true,
                                "FastAPI 기반 추론 API와 실험 결과 수집, 배포 자동화를 운영했습니다.",
                                List.of("Python", "FastAPI", "AWS", "Docker", "GitHub Actions", "Linux")
                        ),
                        careerItem(
                                "LLM Service Lab",
                                "ML 엔지니어",
                                CareerEmploymentType.FULL_TIME,
                                "2020-02",
                                "2022-03",
                                false,
                                "문서 임베딩 검색과 요약 기능을 구현하며 LangChain 기반 워크플로를 서비스에 연결했습니다.",
                                List.of("Python", "LLM", "LangChain", "PostgreSQL", "PyTorch")
                        )
                ),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "https://mlops.seed.itda.local",
                true,
                true,
                skillLevels(
                        skillLevel("Python", Proficiency.ADVANCED),
                        skillLevel("FastAPI", Proficiency.ADVANCED),
                        skillLevel("LLM", Proficiency.ADVANCED),
                        skillLevel("LangChain", Proficiency.INTERMEDIATE),
                        skillLevel("AWS", Proficiency.ADVANCED),
                        skillLevel("Docker", Proficiency.ADVANCED),
                        skillLevel("GitHub Actions", Proficiency.INTERMEDIATE),
                        skillLevel("Linux", Proficiency.ADVANCED),
                        skillLevel("PostgreSQL", Proficiency.INTERMEDIATE),
                        skillLevel("PyTorch", Proficiency.INTERMEDIATE)
                ),
                skills
        );

        ensureResume(
                aiProduct,
                """
                LLM 기능을 실제 제품 플로우에 녹여내는 데 강점이 있는 AI 애플리케이션 엔지니어입니다.
                추천 설명 생성, 관리자 검수 화면, 실험 도구를 함께 만들며 AI 기능의 제품화를 주도했습니다.
                """,
                (byte) 3,
                careerPayload(
                        careerItem(
                                "AI Product Squad",
                                "AI 애플리케이션 엔지니어",
                                CareerEmploymentType.FULL_TIME,
                                "2023-05",
                                null,
                                true,
                                "추천 설명 생성 기능과 운영 검수 화면, 실험용 FastAPI API를 함께 개발했습니다.",
                                List.of("Python", "FastAPI", "LLM", "AWS", "React", "TypeScript")
                        ),
                        careerItem(
                                "Prompt Studio",
                                "프로덕트 엔지니어",
                                CareerEmploymentType.CONTRACT,
                                "2022-02",
                                "2023-04",
                                false,
                                "프롬프트 실험 도구와 문서 요약 기능을 구축했습니다.",
                                List.of("Python", "LangChain", "PostgreSQL", "Docker")
                        )
                ),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "https://ai-product.seed.itda.local",
                true,
                true,
                skillLevels(
                        skillLevel("Python", Proficiency.ADVANCED),
                        skillLevel("FastAPI", Proficiency.INTERMEDIATE),
                        skillLevel("LLM", Proficiency.ADVANCED),
                        skillLevel("LangChain", Proficiency.INTERMEDIATE),
                        skillLevel("AWS", Proficiency.INTERMEDIATE),
                        skillLevel("React", Proficiency.INTERMEDIATE),
                        skillLevel("TypeScript", Proficiency.INTERMEDIATE),
                        skillLevel("Docker", Proficiency.INTERMEDIATE),
                        skillLevel("PostgreSQL", Proficiency.INTERMEDIATE)
                ),
                skills
        );

        ensureResume(
                juniorBackend,
                """
                Java/Spring 기반 API 개발을 막 시작한 주니어 백엔드 개발자입니다.
                운영 경험은 짧지만 기본 CRUD, 테스트 코드, 배포 자동화 학습 프로젝트를 수행했습니다.
                """,
                (byte) 1,
                careerPayload(
                        careerItem(
                                "Starter API Team",
                                "백엔드 개발자",
                                CareerEmploymentType.FULL_TIME,
                                "2025-03",
                                null,
                                true,
                                "사내 운영 도구용 CRUD API와 배치성 알림 기능을 구현했습니다.",
                                List.of("Java", "Spring Boot", "PostgreSQL", "GitHub Actions")
                        )
                ),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "https://junior.seed.itda.local",
                true,
                true,
                skillLevels(
                        skillLevel("Java", Proficiency.INTERMEDIATE),
                        skillLevel("Spring", Proficiency.INTERMEDIATE),
                        skillLevel("Spring Boot", Proficiency.INTERMEDIATE),
                        skillLevel("PostgreSQL", Proficiency.BEGINNER),
                        skillLevel("GitHub Actions", Proficiency.BEGINNER)
                ),
                skills
        );

        ensureResume(
                siteOnlyBackend,
                """
                오프라인 협업과 현장 대응이 많은 프로젝트를 주로 수행해 온 온사이트 중심 백엔드 개발자입니다.
                시스템 운영과 고객사 커뮤니케이션에는 강하지만, 원격/하이브리드 선호도는 낮은 편입니다.
                """,
                (byte) 7,
                careerPayload(
                        careerItem(
                                "Enterprise Delivery",
                                "백엔드 개발자",
                                CareerEmploymentType.FULL_TIME,
                                "2021-07",
                                null,
                                true,
                                "고객사 상주 환경에서 Java/Spring 기반 업무 시스템과 배치 기능을 운영했습니다.",
                                List.of("Java", "Spring", "Oracle", "Redis", "Linux")
                        ),
                        careerItem(
                                "SI Partners",
                                "서버 개발자",
                                CareerEmploymentType.FULL_TIME,
                                "2018-01",
                                "2021-06",
                                false,
                                "공공기관 시스템 API와 관리자 화면 연계 기능을 구축했습니다.",
                                List.of("Java", "Spring Boot", "Oracle", "REST API")
                        )
                ),
                WorkType.SITE,
                ResumeWritingStatus.DONE,
                "https://siteonly.seed.itda.local",
                true,
                true,
                skillLevels(
                        skillLevel("Java", Proficiency.ADVANCED),
                        skillLevel("Spring", Proficiency.ADVANCED),
                        skillLevel("Spring Boot", Proficiency.INTERMEDIATE),
                        skillLevel("Oracle", Proficiency.ADVANCED),
                        skillLevel("Redis", Proficiency.INTERMEDIATE),
                        skillLevel("REST API", Proficiency.INTERMEDIATE),
                        skillLevel("Linux", Proficiency.INTERMEDIATE)
                ),
                skills
        );

        ensureResume(
                optOut,
                """
                추천/매칭 도메인 경험은 충분하지만, 현재는 수동 제안만 받고 있어 AI 추천 노출을 꺼둔 백엔드 개발자입니다.
                후보 풀 필터 동작을 확인하기 위한 옵트아웃 케이스입니다.
                """,
                (byte) 5,
                careerPayload(
                        careerItem(
                                "Private Matching Team",
                                "백엔드 개발자",
                                CareerEmploymentType.FULL_TIME,
                                "2022-08",
                                null,
                                true,
                                "매칭/추천 플랫폼의 운영 API를 개발했지만 현재는 직접 제안만 검토하고 있습니다.",
                                List.of("Java", "Spring", "PostgreSQL", "Redis")
                        ),
                        careerItem(
                                "Core API Studio",
                                "백엔드 엔지니어",
                                CareerEmploymentType.FULL_TIME,
                                "2020-01",
                                "2022-07",
                                false,
                                "공통 인증/회원 API를 운영했습니다.",
                                List.of("Java", "Spring Boot", "MySQL", "JPA")
                        )
                ),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "https://optout.seed.itda.local",
                true,
                false,
                skillLevels(
                        skillLevel("Java", Proficiency.ADVANCED),
                        skillLevel("Spring", Proficiency.ADVANCED),
                        skillLevel("Spring Boot", Proficiency.INTERMEDIATE),
                        skillLevel("PostgreSQL", Proficiency.INTERMEDIATE),
                        skillLevel("Redis", Proficiency.INTERMEDIATE),
                        skillLevel("JPA", Proficiency.INTERMEDIATE)
                ),
                skills
        );

        ensureResume(
                hidden,
                """
                화면 구현 경험은 충분하지만 아직 외부 공개 전 검수 중인 프론트엔드 개발자입니다.
                공개 여부와 AI 추천 허용 여부 필터를 확인하기 위한 비공개 케이스입니다.
                """,
                (byte) 2,
                careerPayload(
                        careerItem(
                                "Stealth UI Team",
                                "프론트엔드 개발자",
                                CareerEmploymentType.FREELANCE,
                                "2024-01",
                                null,
                                true,
                                "초기 대시보드 프로토타입과 공통 컴포넌트를 개발하고 UX 검수 대응을 진행했습니다.",
                                List.of("React", "TypeScript", "Figma")
                        )
                ),
                WorkType.REMOTE,
                ResumeWritingStatus.DONE,
                "https://hidden.seed.itda.local",
                false,
                false,
                skillLevels(
                        skillLevel("React", Proficiency.INTERMEDIATE),
                        skillLevel("TypeScript", Proficiency.INTERMEDIATE),
                        skillLevel("Figma", Proficiency.INTERMEDIATE)
                ),
                skills
        );

        Proposal matchingProposal = ensureProposal(
                client,
                MATCHING_PROPOSAL_TITLE,
                """
                AI로 프로젝트 요구사항을 브리프화하고 프리랜서 추천까지 연결하는 플랫폼을 고도화하려고 합니다.
                추천 설명, 필터링, 결과 비교 흐름까지 한 번에 다듬을 수 있는 팀이 필요합니다.
                """,
                "추천 엔진 MVP를 운영 가능한 수준으로 끌어올리는 고도화 프로젝트",
                19_000_000L,
                25_000_000L,
                16L,
                true
        );
        ProposalPosition backendPos1 = ensureProposalPosition(
                matchingProposal,
                positions.get("백엔드 개발자"),
                "추천 API 백엔드 개발자",
                ProposalWorkType.HYBRID,
                2L,
                6_000_000L,
                8_000_000L,
                14L,
                4,
                8,
                "서울 강남 / 주 2회 협업",
                skillRequirements(
                        skillRequirement("Java", ProposalPositionSkillImportance.ESSENTIAL),
                        skillRequirement("Spring", ProposalPositionSkillImportance.ESSENTIAL),
                        skillRequirement("PostgreSQL", ProposalPositionSkillImportance.PREFERENCE),
                        skillRequirement("Redis", ProposalPositionSkillImportance.PREFERENCE)
                ),
                skills
        );
        ProposalPosition aiPos = ensureProposalPosition(
                matchingProposal,
                positions.get("AI 엔지니어"),
                "추천 모델/프롬프트 AI 엔지니어",
                ProposalWorkType.HYBRID,
                1L,
                7_000_000L,
                9_000_000L,
                16L,
                3,
                5,
                "서울 강남 / 주 2회 협업",
                skillRequirements(
                        skillRequirement("Python", ProposalPositionSkillImportance.ESSENTIAL),
                        skillRequirement("LLM", ProposalPositionSkillImportance.ESSENTIAL),
                        skillRequirement("AWS", ProposalPositionSkillImportance.PREFERENCE)
                ),
                skills
        );

        Proposal writingProposal = ensureProposal(
                client,
                WRITING_PROPOSAL_TITLE,
                """
                내부 운영 대시보드의 프론트 개편을 준비 중입니다.
                아직 초안 단계라 추천 실행 전 상태로 테스트할 수 있어야 합니다.
                """,
                "운영자용 프론트 대시보드 개편 제안서 초안",
                12_000_000L,
                18_000_000L,
                8L,
                false
        );
        ensureProposalPosition(
                writingProposal,
                positions.get("프론트엔드 개발자"),
                "운영 대시보드 프론트엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                5_000_000L,
                7_000_000L,
                8L,
                2,
                4,
                null,
                skillRequirements(
                        skillRequirement("React", ProposalPositionSkillImportance.ESSENTIAL),
                        skillRequirement("TypeScript", ProposalPositionSkillImportance.ESSENTIAL),
                        skillRequirement("Docker", ProposalPositionSkillImportance.PREFERENCE)
                ),
                skills
        );

        // ── Matching 시드 ────────────────────────────────────
        // matchingProposal의 포지션에 프리랜서 매칭 이력 생성
        // → seed.backend / seed.ai 로그인 시 해당 제안서 상세 페이지 접근 가능
        ensureMatching(backendResume, backendPos1, client, backend, MatchingStatus.PROPOSED);
        ensureMatching(aiResume,      aiPos,       client, aiEngineer, MatchingStatus.ACCEPTED);

        // ── Recommendation 시드 ────────────────────────────────
        // backendPos1 기준 추천 결과 Top 3가 항상 노출되도록 실행(run) + 결과(result)를 준비한다.
        ensureComputedRecommendationResults(
                backendPos1,
                List.of(
                        backendResume,
                        platformBackendResume,
                        fullstackResume
                )
        );

        log.info(
                """
                Seed data is ready.
                Seed client email={} matchingProposalId={} writingProposalId={}
                Seed public candidate emails=[{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}]
                Filter scenario resumes=[hidden:{}, optOut:{}]
                Matching seeds: backend→backendPos1(PROPOSED), ai→aiPos(ACCEPTED)
                """,
                client.getEmail().getValue(),
                matchingProposal.getId(),
                writingProposal.getId(),
                backend.getEmail().getValue(),
                fullstack.getEmail().getValue(),
                aiEngineer.getEmail().getValue(),
                platformBackend.getEmail().getValue(),
                dataBackend.getEmail().getValue(),
                frontend.getEmail().getValue(),
                designSystem.getEmail().getValue(),
                mlops.getEmail().getValue(),
                aiProduct.getEmail().getValue(),
                juniorBackend.getEmail().getValue(),
                siteOnlyBackend.getEmail().getValue(),
                hidden.getEmail().getValue(),
                optOut.getEmail().getValue()
        );
    }

    private Map<String, Position> ensurePositions() {
        Map<String, Position> positions = new LinkedHashMap<>();
        positions.put("백엔드 개발자", ensurePosition("백엔드 개발자"));
        positions.put("프론트엔드 개발자", ensurePosition("프론트엔드 개발자"));
        positions.put("AI 엔지니어", ensurePosition("AI 엔지니어"));
        return positions;
    }

    private Map<String, Skill> ensureSkills() {
        Map<String, Skill> skills = new LinkedHashMap<>();
        skills.put("React", ensureSkill("React", "컴포넌트 기반 사용자 인터페이스 구현 역량"));
        skills.put("Vue", ensureSkill("Vue", "Vue.js 기반 사용자 인터페이스 구현 역량"));
        skills.put("Angular", ensureSkill("Angular", "Angular 기반 프론트엔드 애플리케이션 개발 역량"));
        skills.put("Next.js", ensureSkill("Next.js", "React 기반 SSR/풀스택 웹 애플리케이션 개발 역량"));
        skills.put("TypeScript", ensureSkill("TypeScript", "타입 안전성을 갖춘 프론트엔드 및 백엔드 개발 역량"));
        skills.put("JavaScript", ensureSkill("JavaScript", "웹 프론트엔드와 Node.js 생태계 개발 역량"));
        skills.put("HTML", ensureSkill("HTML", "웹 문서 구조 설계 및 마크업 구현 역량"));
        skills.put("CSS", ensureSkill("CSS", "웹 화면 스타일링 및 반응형 레이아웃 구현 역량"));
        skills.put("Tailwind CSS", ensureSkill("Tailwind CSS", "유틸리티 클래스 기반 UI 스타일링 역량"));
        skills.put("Java", ensureSkill("Java", "Spring 기반 서버 구현에 사용하는 백엔드 핵심 언어"));
        skills.put("Spring", ensureSkill("Spring", "Spring Framework 기반 서비스 개발 역량"));
        skills.put("Spring Boot", ensureSkill("Spring Boot", "Spring Boot 기반 백엔드 서비스 개발 역량"));
        skills.put("Node.js", ensureSkill("Node.js", "JavaScript 런타임 기반 서버 개발 역량"));
        skills.put("Express", ensureSkill("Express", "Express 기반 Node.js 웹 서버 및 API 개발 역량"));
        skills.put("NestJS", ensureSkill("NestJS", "NestJS 기반 구조화된 Node.js 백엔드 개발 역량"));
        skills.put("Python", ensureSkill("Python", "백엔드, 데이터, AI 서비스와 스크립팅 개발 역량"));
        skills.put("Django", ensureSkill("Django", "Django 기반 Python 웹 애플리케이션 개발 역량"));
        skills.put("FastAPI", ensureSkill("FastAPI", "FastAPI 기반 Python API 서버 개발 역량"));
        skills.put("JPA", ensureSkill("JPA", "Java ORM 기반 데이터 접근 계층 설계 및 구현 역량"));
        skills.put("Querydsl", ensureSkill("Querydsl", "타입 안전한 동적 쿼리 작성 및 조회 최적화 역량"));
        skills.put("REST API", ensureSkill("REST API", "RESTful API 설계 및 구현 역량"));
        skills.put("GraphQL", ensureSkill("GraphQL", "GraphQL API 설계 및 클라이언트 연동 역량"));
        skills.put("MySQL", ensureSkill("MySQL", "MySQL 기반 관계형 데이터베이스 설계 및 운영 역량"));
        skills.put("PostgreSQL", ensureSkill("PostgreSQL", "운영형 관계형 데이터베이스 설계 및 튜닝"));
        skills.put("MongoDB", ensureSkill("MongoDB", "문서형 NoSQL 데이터베이스 설계 및 활용 역량"));
        skills.put("Redis", ensureSkill("Redis", "캐시 및 세션, 큐 기반 성능 개선 역량"));
        skills.put("Oracle", ensureSkill("Oracle", "Oracle Database 기반 데이터베이스 설계 및 운영 역량"));
        skills.put("MsSQL", ensureSkill("MsSQL", "Microsoft SQL Server 기반 데이터베이스 설계 및 운영 역량"));
        skills.put("Elasticsearch", ensureSkill("Elasticsearch", "검색 엔진 기반 색인, 검색, 로그 분석 구현 역량"));
        skills.put("AWS", ensureSkill("AWS", "클라우드 인프라 운영 및 배포 역량"));
        skills.put("Docker", ensureSkill("Docker", "로컬/운영 환경 컨테이너 기반 배포 역량"));
        skills.put("Kubernetes", ensureSkill("Kubernetes", "컨테이너 오케스트레이션 및 운영 자동화 역량"));
        skills.put("GitHub Actions", ensureSkill("GitHub Actions", "GitHub Actions 기반 CI/CD 파이프라인 구축 역량"));
        skills.put("Nginx", ensureSkill("Nginx", "웹 서버, 리버스 프록시, 로드밸런싱 구성 역량"));
        skills.put("Git", ensureSkill("Git", "버전 관리 및 협업 브랜치 전략 활용 역량"));
        skills.put("CI/CD", ensureSkill("CI/CD", "지속적 통합과 배포 자동화 파이프라인 구성 역량"));
        skills.put("Kafka", ensureSkill("Kafka", "분산 메시징 기반 이벤트 스트리밍 시스템 구현 역량"));
        skills.put("Jenkins", ensureSkill("Jenkins", "Jenkins 기반 빌드 및 배포 자동화 역량"));
        skills.put("GCP", ensureSkill("GCP", "Google Cloud Platform 기반 클라우드 인프라 운영 역량"));
        skills.put("Azure", ensureSkill("Azure", "Microsoft Azure 기반 클라우드 인프라 운영 역량"));
        skills.put("Linux", ensureSkill("Linux", "Linux 서버 운영 및 배포 환경 관리 역량"));
        skills.put("Flutter", ensureSkill("Flutter", "Flutter 기반 크로스플랫폼 모바일 앱 개발 역량"));
        skills.put("React Native", ensureSkill("React Native", "React Native 기반 크로스플랫폼 모바일 앱 개발 역량"));
        skills.put("Swift", ensureSkill("Swift", "Swift 기반 iOS 애플리케이션 개발 역량"));
        skills.put("Kotlin", ensureSkill("Kotlin", "Kotlin 기반 Android 및 JVM 애플리케이션 개발 역량"));
        skills.put("PyTorch", ensureSkill("PyTorch", "PyTorch 기반 딥러닝 모델 개발 및 실험 역량"));
        skills.put("TensorFlow", ensureSkill("TensorFlow", "TensorFlow 기반 머신러닝 모델 개발 및 운영 역량"));
        skills.put("LangChain", ensureSkill("LangChain", "LangChain 기반 LLM 애플리케이션 및 RAG 워크플로 구현 역량"));
        skills.put("LLM", ensureSkill("LLM", "대규모 언어 모델 연동 및 프롬프트 설계 역량"));
        skills.put("Figma", ensureSkill("Figma", "UI/UX 설계와 협업용 디자인 산출물 작성 역량"));
        return skills;
    }

    private Member ensureMember(String email, String name, String nickname, String phone) {
        Member member = Optional.ofNullable(memberRepository.findByEmail_Value(email))
                .orElseGet(() -> memberRepository.save(
                        Member.create(
                                email,
                                passwordEncoder.encode(defaultPassword),
                                name,
                                nickname,
                                null,
                                phone
                        )
                ));

        member.changeHashedPassword(passwordEncoder.encode(defaultPassword));
        member.update(name, nickname, phone);
        return member;
    }

    private Resume ensureResume(
            Member member,
            String introduction,
            byte careerYears,
            CareerPayload career,
            WorkType preferredWorkType,
            ResumeWritingStatus writingStatus,
            String portfolioUrl,
            boolean publiclyVisible,
            boolean aiMatchingEnabled,
            List<SeedSkillLevel> skillLevels,
            Map<String, Skill> skills
    ) {
        Resume resume = resumeRepository.findByMemberId(member.getId())
                .orElseGet(() -> resumeRepository.save(
                        Resume.create(
                                member,
                                introduction,
                                careerYears,
                                career,
                                preferredWorkType,
                                writingStatus,
                                portfolioUrl
                        )
                ));

        resume.update(introduction, careerYears, career, preferredWorkType, writingStatus, portfolioUrl);

        if (resume.getStatus() == ResumeStatus.INACTIVE) {
            resume.restore();
        }
        if (resume.isPubliclyVisible() != publiclyVisible) {
            resume.togglePubliclyVisible();
        }
        if (resume.isAiMatchingEnabled() != aiMatchingEnabled) {
            resume.toggleAiMatchingEnabled();
        }

        for (SeedSkillLevel skillLevel : skillLevels) {
            Skill skill = getRequiredSkill(skills, skillLevel.skillName());
            Optional<ResumeSkill> existingSkill = resume.getSkills().stream()
                    .filter(resumeSkill -> resumeSkill.getSkill().getName().equals(skillLevel.skillName()))
                    .findFirst();

            if (existingSkill.isPresent()) {
                existingSkill.get().update(skill, skillLevel.proficiency());
                continue;
            }

            resume.addSkill(skill, skillLevel.proficiency());
        }

        List<String> desiredSkillNames = skillLevels.stream()
                .map(SeedSkillLevel::skillName)
                .toList();
        List<Skill> removableSkills = resume.getSkills().stream()
                .map(ResumeSkill::getSkill)
                .filter(skill -> !desiredSkillNames.contains(skill.getName()))
                .toList();
        removableSkills.forEach(resume::removeSkill);

        Resume savedResume = resumeRepository.save(resume);
        refreshResumeEmbedding(savedResume);
        return savedResume;
    }

    private Proposal ensureProposal(
            Member member,
            String title,
            String rawInputText,
            String description,
            Long totalBudgetMin,
            Long totalBudgetMax,
            Long expectedPeriod,
            boolean matching
    ) {
        Proposal proposal = proposalRepository.findByMemberIdAndTitle(member.getId(), title)
                // Proposal.positions is LAZY. To make the seed idempotent we must ensure positions are loaded,
                // otherwise `proposal.getPositions()` may look empty and we would try to insert duplicates.
                .map(existing -> proposalRepository.findWithPositionsById(existing.getId()).orElse(existing))
                .orElseGet(() -> proposalRepository.save(
                        Proposal.create(
                                member,
                                title,
                                rawInputText,
                                description,
                                totalBudgetMin,
                                totalBudgetMax,
                                expectedPeriod
                        )
                ));

        proposal.update(title, rawInputText, description, totalBudgetMin, totalBudgetMax, expectedPeriod);

        if (matching && proposal.getStatus().name().equals("WRITING")) {
            proposal.startMatching();
        }

        return proposalRepository.save(proposal);
    }

    private ProposalPosition ensureProposalPosition(
            Proposal proposal,
            Position position,
            String title,
            ProposalWorkType workType,
            Long headCount,
            Long unitBudgetMin,
            Long unitBudgetMax,
            Long expectedPeriod,
            Integer careerMinYears,
            Integer careerMaxYears,
            String workPlace,
            List<SeedSkillRequirement> skillRequirements,
            Map<String, Skill> skills
    ) {
        ProposalPosition proposalPosition = proposal.getPositions().stream()
                .filter(existing -> hasSamePositionIdentity(existing, position))
                .findFirst()
                .orElseGet(() -> proposal.addPosition(
                        position,
                        title,
                        workType,
                        headCount,
                        unitBudgetMin,
                        unitBudgetMax,
                        expectedPeriod,
                        careerMinYears,
                        careerMaxYears,
                        workPlace
                ));

        proposalPosition.update(
                position,
                title,
                workType,
                headCount,
                unitBudgetMin,
                unitBudgetMax,
                expectedPeriod,
                careerMinYears,
                careerMaxYears,
                workPlace
        );

        for (SeedSkillRequirement requirement : skillRequirements) {
            Skill skill = getRequiredSkill(skills, requirement.skillName());
            Optional<ProposalPositionSkill> existingSkill = proposalPosition.getSkills().stream()
                    .filter(proposalSkill -> proposalSkill.getSkill().getName().equals(requirement.skillName()))
                    .findFirst();

            if (existingSkill.isPresent()) {
                existingSkill.get().changeImportance(requirement.importance());
                continue;
            }

            proposalPosition.addSkill(skill, requirement.importance());
        }

        // ProposalPosition은 Proposal에 의해 생성되지만, Matching 생성 시점에는 proposalPosition.id가 필요하다.
        // saveAndFlush가 merge로 동작하면 반환된 엔티티가 영속 상태가 되므로,
        // 반환값 기준으로 다시 ProposalPosition을 찾아 id가 채워진 인스턴스를 리턴한다.
        Proposal savedProposal = proposalRepository.saveAndFlush(proposal);
        Proposal hydrated = proposalRepository.findWithPositionsById(savedProposal.getId()).orElse(savedProposal);

        return hydrated.getPositions().stream()
                .filter(existing -> hasSamePositionIdentity(existing, position))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("시드 포지션 저장에 실패했습니다. proposalId=" + savedProposal.getId()));
    }

    private boolean hasSamePositionIdentity(ProposalPosition existing, Position position) {
        boolean samePosition = existing.getPosition().getId() != null && position.getId() != null
                ? existing.getPosition().getId().equals(position.getId())
                : existing.getPosition().getName().equals(position.getName());
        // DB unique constraint is (proposal_id, position_id). One proposal cannot contain multiple
        // ProposalPositions with the same Position, so treat "same Position" as identity and update
        // the rest of fields (title, budgets, skills...) in-place.
        return samePosition;
    }

    private Skill getRequiredSkill(Map<String, Skill> skills, String skillName) {
        Skill skill = skills.get(skillName);
        if (skill == null) {
            throw new IllegalStateException("시드 스킬을 찾을 수 없습니다. skillName=" + skillName);
        }
        return skill;
    }

    private void refreshResumeEmbedding(Resume resume) {
        try {
            resumeEmbeddingService.createOrRefresh(resume);
        } catch (Exception e) {
            log.error(
                    "시드 이력서 임베딩 생성/갱신 실패. resumeId={} email={}",
                    resume.getId(),
                    resume.getMember().getEmail().getValue(),
                    e
            );
        }
    }

    private Matching ensureMatching(
            Resume resume,
            ProposalPosition proposalPosition,
            Member clientMember,
            Member freelancerMember,
            MatchingStatus desiredStatus
    ) {
        Matching matching = matchingRepository
                .findByProposalPosition_Proposal_IdAndFreelancerMember_Email_Value(
                        proposalPosition.getProposal().getId(),
                        freelancerMember.getEmail().getValue())
                .stream()
                .filter(m -> m.getProposalPosition().getId().equals(proposalPosition.getId()))
                .findFirst()
                .orElseGet(() -> matchingRepository.save(
                        Matching.create(resume, proposalPosition, clientMember, freelancerMember)
                ));

        // 저장된 매칭 상태가 PROPOSED이고 원하는 상태가 ACCEPTED인 경우 수락 처리
        if (matching.getStatus() == MatchingStatus.PROPOSED && desiredStatus == MatchingStatus.ACCEPTED) {
            matching.accept();
        }
        return matching;
    }

    private void ensureComputedRecommendationResults(
            ProposalPosition proposalPosition,
            List<Resume> candidateResumes
    ) {
        List<Resume> uniqueCandidates = deduplicateResumesById(candidateResumes);

        RecommendationAlgorithm algorithm = RecommendationAlgorithm.HEURISTIC_V1;
        int topK = 3;
        String fingerprint = recommendationFingerprintGenerator.generate(proposalPosition, algorithm, topK);

        RecommendationRun run = recommendationRunRepository
                .findByProposalPosition_IdAndRequestFingerprintAndAlgorithm(
                        proposalPosition.getId(),
                        fingerprint,
                        algorithm
                )
                .orElse(null);

        if (run != null && run.isFailed()) {
            recommendationResultRepository.deleteAll(recommendationResultRepository.findByRunIdWithResume(run.getId()));
            recommendationRunRepository.delete(run);
            run = null;
        }

        if (run == null) {
            run = recommendationRunRepository.save(
                    RecommendationRun.create(proposalPosition, fingerprint, algorithm, topK)
            );
        }

        if (!run.isComputed()) {
            if (run.isPending()) {
                run.markRunning();
            }
            if (run.isRunning()) {
                run.markCompleted(new HardFilterStat(20, uniqueCandidates.size()));
            }
            run = recommendationRunRepository.save(run);
        }

        // 결과는 매번 같은 형태로 유지(시드 idempotency 목적)
        // 주의: insert/delete flush 순서에 따라 unique constraint가 터질 수 있어 bulk delete로 먼저 정리한다.
        recommendationResultRepository.deleteAllByRecommendationRun_Id(run.getId());
        recommendationResultRepository.flush();

        for (int i = 0; i < Math.min(topK, uniqueCandidates.size()); i++) {
            Resume resume = uniqueCandidates.get(i);
            int rank = i + 1;

            BigDecimal finalScore = switch (rank) {
                case 1 -> new BigDecimal("0.9500");
                case 2 -> new BigDecimal("0.8700");
                default -> new BigDecimal("0.7900");
            };
            BigDecimal embeddingScore = switch (rank) {
                case 1 -> new BigDecimal("0.9100");
                case 2 -> new BigDecimal("0.8400");
                default -> new BigDecimal("0.7700");
            };

            List<String> requiredSkillNames = proposalPosition.getSkills().stream()
                    .map(ProposalPositionSkill::getSkill)
                    .map(Skill::getName)
                    .distinct()
                    .toList();

            List<String> matchedSkills = resume.getSkills().stream()
                    .map(ResumeSkill::getSkill)
                    .map(Skill::getName)
                    .filter(requiredSkillNames::contains)
                    .distinct()
                    .sorted()
                    .toList();

            SeedRecommendationNarrative narrative = seedNarrativeFor(rank, resume, proposalPosition.getTitle());
            List<String> highlights = createSeedHighlights(matchedSkills, resume, embeddingScore);

            ReasonFacts facts = new ReasonFacts(
                    matchedSkills,
                    narrative.matchedDomains(),
                    resume.getCareerYears() != null ? resume.getCareerYears().intValue() : 0,
                    highlights
            );

            RecommendationResult result = RecommendationResult.create(
                    run,
                    resume,
                    rank,
                    finalScore,
                    embeddingScore,
                    facts
            );

            result.markLlmReady(narrative.llmReason());
            recommendationResultRepository.save(result);
        }
    }

    private List<Resume> deduplicateResumesById(List<Resume> candidateResumes) {
        if (candidateResumes == null || candidateResumes.isEmpty()) {
            return List.of();
        }

        Map<Long, Resume> deduplicated = new LinkedHashMap<>();
        for (Resume resume : candidateResumes) {
            if (resume == null) {
                continue;
            }
            if (resume.getId() == null) {
                continue;
            }
            deduplicated.putIfAbsent(resume.getId(), resume);
        }
        return List.copyOf(deduplicated.values());
    }

    private List<String> createSeedHighlights(
            List<String> matchedSkills,
            Resume resume,
            BigDecimal embeddingScore
    ) {
        List<String> highlights = new java.util.ArrayList<>();

        if (matchedSkills != null && !matchedSkills.isEmpty()) {
            highlights.add("공통 스킬 " + matchedSkills.size() + "개 보유");
        }

        int careerYears = resume != null && resume.getCareerYears() != null ? resume.getCareerYears().intValue() : 0;
        if (careerYears > 0) {
            highlights.add("관련 경력 " + careerYears + "년");
        }

        if (embeddingScore != null && embeddingScore.compareTo(new BigDecimal("0.8000")) >= 0) {
            highlights.add("요구 조건과 높은 유사도");
        }

        return highlights;
    }

    private SeedRecommendationNarrative seedNarrativeFor(int rank, Resume resume, String positionTitle) {
        String name = resume.getMember().getNickname() != null && !resume.getMember().getNickname().isBlank()
                ? resume.getMember().getNickname()
                : resume.getMember().getName();
        String years = resume.getCareerYears() != null ? resume.getCareerYears().toString() : "?";
        String email = resume.getMember().getEmail().getValue();

        if (SEED_BACKEND_EMAIL.equals(email)) {
            return new SeedRecommendationNarrative(
                    List.of("추천/매칭", "백엔드", "운영"),
                    List.of(
                            "Spring 기반 추천/매칭 API 설계 및 운영 경험",
                            "PostgreSQL/Redis로 조회 성능·캐시 최적화 경험",
                            "장애 대응 및 지표 기반 성능 튜닝 경험"
                    ),
                    """
                    %s 후보는 %s 포지션에 필요한 Java/Spring 기반의 백엔드 설계·운영 경험이 풍부합니다.
                    특히 추천/매칭 API를 운영 환경에서 튜닝한 경험이 있어 트래픽 증가 구간에서도 안정적으로 대응할 가능성이 높습니다.
                    PostgreSQL/Redis를 함께 사용한 이력이 있어 데이터 접근 패턴 설계와 캐시 전략 수립에도 강점이 있습니다. (경력 %s년)
                    """.formatted(name, positionTitle, years).trim()
            );
        }

        if (SEED_PLATFORM_BACKEND_EMAIL.equals(email)) {
            return new SeedRecommendationNarrative(
                    List.of("플랫폼", "백엔드", "운영 자동화"),
                    List.of(
                            "Redis/Kafka 기반 고트래픽 백엔드 운영 경험",
                            "Docker/Kubernetes/GitHub Actions 기반 배포 자동화 경험",
                            "PostgreSQL 튜닝과 플랫폼 운영 협업 경험"
                    ),
                    """
                    %s 후보는 %s 포지션에서 필요한 백엔드 구현 역량에 더해 플랫폼 운영 관점까지 함께 가져갈 수 있는 인력입니다.
                    Redis/Kafka를 활용한 비동기 처리와 캐시 설계 경험이 있어, 추천 API 고도화 과정에서 병목 구간을 구조적으로 개선할 가능성이 높습니다.
                    배포 자동화와 운영 안정화 경험까지 갖추고 있어 초기 고도화 이후의 운영 단계까지 연결해 기여할 수 있습니다. (경력 %s년)
                    """.formatted(name, positionTitle, years).trim()
            );
        }

        if (SEED_FULLSTACK_EMAIL.equals(email)) {
            return new SeedRecommendationNarrative(
                    List.of("백엔드", "관리자/대시보드", "DevOps"),
                    List.of(
                            "API 서버와 관리자 화면을 함께 구축한 풀스택 경험",
                            "Docker 기반 배포/운영 경험으로 개발-배포 흐름 대응",
                            "백엔드(Spring)와 프론트(React/TS) 협업 관점 강점"
                    ),
                    """
                    %s 후보는 백엔드(Spring)와 프론트(React/TypeScript)를 모두 경험해 팀 협업/커뮤니케이션 비용을 낮출 수 있습니다.
                    추천 API 자체 개발뿐 아니라 운영/배포 파이프라인(Docker) 관점에서도 도움을 줄 수 있어, 초기 고도화 단계에서 유연하게 역할을 확장할 수 있습니다.
                    핵심 백엔드 스킬셋(Java/Spring)을 보유하고 있어 포지션 요구사항과의 기본 적합도도 충분합니다. (경력 %s년)
                    """.formatted(name, years).trim()
            );
        }

        if (SEED_AI_EMAIL.equals(email) || SEED_MLOPS_EMAIL.equals(email) || SEED_AI_PRODUCT_EMAIL.equals(email)) {
            return new SeedRecommendationNarrative(
                    List.of("데이터", "AI", "플랫폼"),
                    List.of(
                            "LLM 기반 추천/설명 생성 파이프라인 구현 경험",
                            "Python/AWS 기반 추론 서비스 운영 경험",
                            "PostgreSQL 기반 데이터 모델링 및 분석 경험"
                    ),
                    """
                    %s 후보는 LLM/추천 파이프라인 경험이 있어, 추천 품질 개선(설명 생성/실험/지표 설계) 측면에서 강점을 보입니다.
                    이번 포지션이 순수 백엔드 중심이라면 매칭 강도는 다소 낮을 수 있으나, 추천 기능 고도화/실험을 병행한다면 기여도가 커질 수 있습니다.
                    데이터 저장소(PostgreSQL) 경험이 있어 백엔드팀과의 협업 접점도 확보 가능합니다. (경력 %s년)
                    """.formatted(name, years).trim()
            );
        }

        if (rank == 1) {
            return new SeedRecommendationNarrative(
                    List.of("백엔드", "운영", "협업"),
                    List.of(
                            "핵심 요구 스킬과 직접 맞닿는 실무 경험",
                            "운영 환경에서의 기능 개선 및 안정화 경험",
                            "프로젝트 초반 적응이 빠른 범용 기술 스택"
                    ),
                    """
                    %s 후보는 %s 포지션의 핵심 요구사항과 직접 맞닿는 경험을 갖고 있어 빠르게 전력화될 가능성이 높습니다.
                    운영 환경에서 기능 개선과 안정화 작업을 함께 수행한 이력이 있어 실무 투입 이후의 리스크도 낮은 편입니다. (경력 %s년)
                    """.formatted(name, positionTitle, years).trim()
            );
        }

        return new SeedRecommendationNarrative(
                List.of("범용 개발", "협업", "운영"),
                List.of(
                        "실무형 기능 개발과 운영 경험 보유",
                        "프로젝트 상황에 맞춘 역할 확장 가능",
                        "협업 비용이 낮은 범용 기술 스택"
                ),
                """
                %s 후보는 특정 단일 역할에만 한정되지 않고, 기능 개발과 운영을 함께 수행할 수 있는 범용 역량을 갖추고 있습니다.
                현재 %s 포지션이 요구하는 핵심 기술과의 직접 접점이 있어 온보딩 부담이 낮고, 주변 업무까지 유연하게 확장할 수 있습니다. (경력 %s년)
                """.formatted(name, positionTitle, years).trim()
        );
    }

    private record SeedRecommendationNarrative(
            List<String> matchedDomains,
            List<String> highlights,
            String llmReason
    ) {
    }

    private Position ensurePosition(String name) {
        return positionRepository.findByName(name)
                .orElseGet(() -> positionRepository.save(Position.create(name)));
    }

    private Skill ensureSkill(String name, String description) {
        Skill skill = skillRepository.findByName(name)
                .orElseGet(() -> skillRepository.save(Skill.create(name, description)));

        skill.update(name, description);
        return skill;
    }

    private CareerPayload careerPayload(CareerItemPayload... items) {
        CareerPayload payload = new CareerPayload();
        payload.setItems(List.of(items));
        return payload;
    }

    private CareerItemPayload careerItem(
            String companyName,
            String position,
            CareerEmploymentType employmentType,
            String startYearMonth,
            String endYearMonth,
            boolean currentlyWorking,
            String summary,
            List<String> techStack
    ) {
        CareerItemPayload item = new CareerItemPayload();
        item.setCompanyName(companyName);
        item.setPosition(position);
        item.setEmploymentType(employmentType);
        item.setStartYearMonth(startYearMonth);
        item.setEndYearMonth(endYearMonth);
        item.setCurrentlyWorking(currentlyWorking);
        item.setSummary(summary);
        item.setTechStack(techStack);
        return item;
    }

    private List<SeedSkillLevel> skillLevels(SeedSkillLevel... levels) {
        return List.of(levels);
    }

    private SeedSkillLevel skillLevel(String skillName, Proficiency proficiency) {
        return new SeedSkillLevel(skillName, proficiency);
    }

    private List<SeedSkillRequirement> skillRequirements(SeedSkillRequirement... requirements) {
        return List.of(requirements);
    }

    private SeedSkillRequirement skillRequirement(String skillName, ProposalPositionSkillImportance importance) {
        return new SeedSkillRequirement(skillName, importance);
    }

    private record SeedSkillLevel(String skillName, Proficiency proficiency) {
    }

    private record SeedSkillRequirement(String skillName, ProposalPositionSkillImportance importance) {
    }
}
