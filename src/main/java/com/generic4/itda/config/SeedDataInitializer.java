package com.generic4.itda.config;

import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkill;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.proposal.ProposalWorkType;
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
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.repository.PositionRepository;
import com.generic4.itda.repository.ProposalRepository;
import com.generic4.itda.repository.ResumeRepository;
import com.generic4.itda.repository.SkillRepository;
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
    private static final String SEED_HIDDEN_EMAIL = "seed.hidden@itda.local";

    private static final String MATCHING_PROPOSAL_TITLE = "[SEED] AI 프리랜서 추천 플랫폼 고도화";
    private static final String WRITING_PROPOSAL_TITLE = "[SEED] 관리자 대시보드 프론트 개편";

    private final MemberRepository memberRepository;
    private final PositionRepository positionRepository;
    private final SkillRepository skillRepository;
    private final ResumeRepository resumeRepository;
    private final ProposalRepository proposalRepository;
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
        Member hidden = ensureMember(SEED_HIDDEN_EMAIL, "최히든", "비공개 시드", "010-9000-1004");

        ensureResume(
                backend,
                "Spring Boot와 PostgreSQL 기반 B2B 백엔드 서비스를 설계하고 운영해 온 백엔드 개발자입니다.",
                (byte) 6,
                careerPayload(
                        careerItem(
                                "ITDA Labs",
                                "백엔드 개발자",
                                CareerEmploymentType.FULL_TIME,
                                "2021-03",
                                null,
                                true,
                                "매칭/추천 API와 운영용 백오피스를 설계하고 성능 튜닝을 담당했습니다.",
                                List.of("Java", "Spring", "PostgreSQL", "Redis")
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
                        skillLevel("PostgreSQL", Proficiency.ADVANCED),
                        skillLevel("Redis", Proficiency.INTERMEDIATE)
                ),
                skills
        );

        ensureResume(
                fullstack,
                "백엔드와 프론트를 모두 다루며 관리자 화면과 고객용 서비스 UI를 함께 구축하는 풀스택 개발자입니다.",
                (byte) 5,
                careerPayload(
                        careerItem(
                                "Product Studio",
                                "풀스택 개발자",
                                CareerEmploymentType.FULL_TIME,
                                "2020-07",
                                null,
                                true,
                                "관리자 대시보드와 API 서버를 한 팀에서 개발했습니다.",
                                List.of("Java", "Spring", "React", "TypeScript", "Docker")
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
                        skillLevel("Docker", Proficiency.INTERMEDIATE)
                ),
                skills
        );

        ensureResume(
                aiEngineer,
                "LLM 기능 기획부터 Python 기반 추론 서비스 운영까지 경험한 AI 엔지니어입니다.",
                (byte) 4,
                careerPayload(
                        careerItem(
                                "AI Matching Team",
                                "AI 엔지니어",
                                CareerEmploymentType.CONTRACT,
                                "2022-01",
                                null,
                                true,
                                "추천 설명 생성과 검색 증강 워크플로를 구현했습니다.",
                                List.of("Python", "PostgreSQL", "LLM", "AWS")
                        )
                ),
                WorkType.REMOTE,
                ResumeWritingStatus.DONE,
                "https://ai.seed.itda.local",
                true,
                true,
                skillLevels(
                        skillLevel("Python", Proficiency.ADVANCED),
                        skillLevel("LLM", Proficiency.ADVANCED),
                        skillLevel("PostgreSQL", Proficiency.INTERMEDIATE),
                        skillLevel("AWS", Proficiency.INTERMEDIATE)
                ),
                skills
        );

        ensureResume(
                hidden,
                "화면 구현은 가능하지만 아직 외부 공개 전 검수 중인 프론트엔드 개발자입니다.",
                (byte) 2,
                careerPayload(
                        careerItem(
                                "Stealth UI Team",
                                "프론트엔드 개발자",
                                CareerEmploymentType.FREELANCE,
                                "2024-01",
                                null,
                                true,
                                "초기 대시보드 프로토타입과 공통 컴포넌트를 개발했습니다.",
                                List.of("React", "TypeScript")
                        )
                ),
                WorkType.REMOTE,
                ResumeWritingStatus.DONE,
                "https://hidden.seed.itda.local",
                false,
                false,
                skillLevels(
                        skillLevel("React", Proficiency.INTERMEDIATE),
                        skillLevel("TypeScript", Proficiency.INTERMEDIATE)
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
                19_500_000L,
                25_500_000L,
                16L,
                true
        );
        ensureProposalPosition(
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
        ensureProposalPosition(
                matchingProposal,
                positions.get("백엔드 개발자"),
                "LLM 워크플로 백엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                6_500_000L,
                8_500_000L,
                12L,
                3,
                6,
                null,
                skillRequirements(
                        skillRequirement("Java", ProposalPositionSkillImportance.ESSENTIAL),
                        skillRequirement("Spring", ProposalPositionSkillImportance.ESSENTIAL),
                        skillRequirement("Docker", ProposalPositionSkillImportance.PREFERENCE),
                        skillRequirement("Redis", ProposalPositionSkillImportance.PREFERENCE)
                ),
                skills
        );
        ensureProposalPosition(
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

        log.info(
                """
                Seed data is ready.
                Seed client email={} matchingProposalId={} writingProposalId={}
                Seed freelancer emails=[{}, {}, {}, {}]
                """,
                client.getEmail().getValue(),
                matchingProposal.getId(),
                writingProposal.getId(),
                backend.getEmail().getValue(),
                fullstack.getEmail().getValue(),
                aiEngineer.getEmail().getValue(),
                hidden.getEmail().getValue()
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
        skills.put("Java", ensureSkill("Java", "Spring 기반 서버 구현에 사용하는 백엔드 핵심 언어"));
        skills.put("Spring", ensureSkill("Spring", "Spring Boot 기반 서비스 개발 역량"));
        skills.put("PostgreSQL", ensureSkill("PostgreSQL", "운영형 관계형 데이터베이스 설계 및 튜닝"));
        skills.put("React", ensureSkill("React", "관리자/사용자 화면 프론트엔드 구현 역량"));
        skills.put("TypeScript", ensureSkill("TypeScript", "타입 안전성을 갖춘 프론트엔드 개발 역량"));
        skills.put("Redis", ensureSkill("Redis", "캐시 및 세션, 큐 기반 성능 개선 역량"));
        skills.put("Docker", ensureSkill("Docker", "로컬/운영 환경 컨테이너 기반 배포 역량"));
        skills.put("Python", ensureSkill("Python", "데이터/AI 서비스와 스크립팅 개발 역량"));
        skills.put("AWS", ensureSkill("AWS", "클라우드 인프라 운영 및 배포 역량"));
        skills.put("LLM", ensureSkill("LLM", "대규모 언어 모델 연동 및 프롬프트 설계 역량"));
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
            Skill skill = skills.get(skillLevel.skillName());
            Optional<ResumeSkill> existingSkill = resume.getSkills().stream()
                    .filter(resumeSkill -> resumeSkill.getSkill().getName().equals(skillLevel.skillName()))
                    .findFirst();

            if (existingSkill.isPresent()) {
                existingSkill.get().update(skill, skillLevel.proficiency());
                continue;
            }

            resume.addSkill(skill, skillLevel.proficiency());
        }

        return resumeRepository.save(resume);
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
                .filter(existing -> hasSamePositionIdentity(existing, position, title))
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
            Skill skill = skills.get(requirement.skillName());
            Optional<ProposalPositionSkill> existingSkill = proposalPosition.getSkills().stream()
                    .filter(proposalSkill -> proposalSkill.getSkill().getName().equals(requirement.skillName()))
                    .findFirst();

            if (existingSkill.isPresent()) {
                existingSkill.get().changeImportance(requirement.importance());
                continue;
            }

            proposalPosition.addSkill(skill, requirement.importance());
        }

        proposalRepository.save(proposal);
        return proposalPosition;
    }

    private boolean hasSamePositionIdentity(ProposalPosition existing, Position position, String title) {
        boolean samePosition = existing.getPosition().getId() != null && position.getId() != null
                ? existing.getPosition().getId().equals(position.getId())
                : existing.getPosition().getName().equals(position.getName());
        return samePosition && existing.getTitle().equals(title);
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
