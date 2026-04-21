package com.generic4.itda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.resume.CareerEmploymentType;
import com.generic4.itda.domain.resume.CareerItemPayload;
import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Proficiency;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.dto.resume.ResumeForm;
import com.generic4.itda.dto.resume.ResumeSkillItemForm;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.repository.ResumeRepository;
import com.generic4.itda.repository.SkillRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    private static final String EMAIL = "user@test.com";
    private static final Long MEMBER_ID = 1L;
    private static final Long RESUME_ID = 10L;
    private static final Long SKILL_ID = 100L;

    @InjectMocks
    private ResumeService resumeService;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ResumeEmbeddingService resumeEmbeddingService;

    private ResumeForm form;
    private Member member;

    @BeforeEach
    void setUp() {
        form = new ResumeForm();
        form.setIntroduction("수정된 자기소개");
        form.setCareerYears((byte) 5);
        form.setCareer(new CareerPayload());
        form.setPreferredWorkType(WorkType.REMOTE);
        form.setWritingStatus(ResumeWritingStatus.WRITING);
        form.setPortfolioUrl("https://example.com/portfolio");
        form.setPubliclyVisible(true);
        form.setAiMatchingEnabled(true);

        member = Member.create(EMAIL, "hashed-password", "테스터", null, null, "010-1234-5678");
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
    }

    @Test
    @DisplayName("이력서 생성 시 저장된 이력서를 반환하고 임베딩 갱신을 요청한다")
    void create_Success() {
        when(resumeRepository.existsByMemberEmailValue(EMAIL)).thenReturn(false);
        when(memberRepository.findByEmail_Value(EMAIL)).thenReturn(member);
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Resume saved = resumeService.create(EMAIL, form);

        ArgumentCaptor<Resume> resumeCaptor = ArgumentCaptor.forClass(Resume.class);
        verify(resumeRepository).save(resumeCaptor.capture());
        Resume captured = resumeCaptor.getValue();

        assertThat(saved).isSameAs(captured);
        assertThat(saved.getMember()).isSameAs(member);
        assertThat(saved.getIntroduction()).isEqualTo(form.getIntroduction());
        assertThat(saved.getCareerYears()).isEqualTo(form.getCareerYears());
        assertThat(saved.getCareer()).isSameAs(form.getCareer());
        assertThat(saved.getPreferredWorkType()).isEqualTo(form.getPreferredWorkType());
        assertThat(saved.getWritingStatus()).isEqualTo(form.getWritingStatus());
        assertThat(saved.getPortfolioUrl()).isEqualTo(form.getPortfolioUrl());
        verify(resumeEmbeddingService).createOrRefresh(saved);
    }

    @Test
    @DisplayName("이력서 생성 후 임베딩 갱신이 실패해도 생성은 성공한다")
    void create_Success_WhenEmbeddingRefreshFails() {
        when(resumeRepository.existsByMemberEmailValue(EMAIL)).thenReturn(false);
        when(memberRepository.findByEmail_Value(EMAIL)).thenReturn(member);
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("embedding failed"))
                .when(resumeEmbeddingService)
                .createOrRefresh(any(Resume.class));

        assertThatCode(() -> resumeService.create(EMAIL, form))
                .doesNotThrowAnyException();

        verify(resumeRepository).save(any(Resume.class));
        verify(resumeEmbeddingService).createOrRefresh(any(Resume.class));
    }

    @Test
    @DisplayName("이력서 생성 시 전달된 스킬 목록을 함께 저장한다")
    void create_Success_WithSkills() {
        when(resumeRepository.existsByMemberEmailValue(EMAIL)).thenReturn(false);
        when(memberRepository.findByEmail_Value(EMAIL)).thenReturn(member);
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Skill java = Skill.create("Java", null);
        when(skillRepository.findById(1L)).thenReturn(Optional.of(java));

        ResumeSkillItemForm item = new ResumeSkillItemForm();
        item.setSkillId(1L);
        item.setProficiency(Proficiency.INTERMEDIATE);
        form.setSkillItems(List.of(item));

        Resume created = resumeService.create(EMAIL, form);

        assertThat(created.getSkills()).isNotEmpty();
        verify(skillRepository).findById(1L);
        verify(resumeRepository).save(any(Resume.class));
    }

    @Test
    @DisplayName("이력서 생성 실패 - 이미 존재하면 회원 조회를 하지 않음")
    void create_Fail_AlreadyExists() {
        when(resumeRepository.existsByMemberEmailValue(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> resumeService.create(EMAIL, form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 이력서가 존재합니다.");

        verify(memberRepository, never()).findByEmail_Value(anyString());
        verify(resumeRepository, never()).save(any(Resume.class));
        verifyNoInteractions(resumeEmbeddingService);
    }

    @Test
    @DisplayName("이력서 수정 시 기존 경력을 유지하고 공개 여부와 AI 추천 여부를 토글한다")
    void update_Success() {
        Resume resume = org.mockito.Mockito.mock(Resume.class);
        CareerPayload storedCareer = new CareerPayload();

        when(memberRepository.findByEmail_Value(EMAIL)).thenReturn(member);
        when(resumeRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(resume));
        when(resume.getCareer()).thenReturn(storedCareer);
        when(resume.isPubliclyVisible()).thenReturn(true);
        when(resume.isAiMatchingEnabled()).thenReturn(true);

        form.setPubliclyVisible(false);
        form.setAiMatchingEnabled(false);

        resumeService.update(EMAIL, form);

        verify(resume).update(
                form.getIntroduction(),
                form.getCareerYears(),
                storedCareer,
                form.getPreferredWorkType(),
                form.getWritingStatus(),
                form.getPortfolioUrl()
        );
        verify(resume).togglePubliclyVisible();
        verify(resume).toggleAiMatchingEnabled();
        verify(resumeEmbeddingService).createOrRefresh(resume);
    }

    @Test
    @DisplayName("이력서 수정 시 공개 여부와 AI 추천 여부가 같으면 토글하지 않는다")
    void update_DoesNotToggle_WhenFlagsAreUnchanged() {
        Resume resume = org.mockito.Mockito.mock(Resume.class);
        CareerPayload storedCareer = new CareerPayload();

        when(memberRepository.findByEmail_Value(EMAIL)).thenReturn(member);
        when(resumeRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(resume));
        when(resume.getCareer()).thenReturn(storedCareer);
        when(resume.isPubliclyVisible()).thenReturn(true);
        when(resume.isAiMatchingEnabled()).thenReturn(true);

        resumeService.update(EMAIL, form);

        verify(resume, never()).togglePubliclyVisible();
        verify(resume, never()).toggleAiMatchingEnabled();
        verify(resumeEmbeddingService).createOrRefresh(resume);
    }

    @Test
    @DisplayName("이력서 조회는 Fetch Join 메서드를 사용한다")
    void findByEmail_WithFetchJoin() {
        Resume resume = org.mockito.Mockito.mock(Resume.class);
        when(resumeRepository.findByMemberEmailWithDetails(EMAIL)).thenReturn(Optional.of(resume));

        Resume result = resumeService.findByEmail(EMAIL);

        assertThat(result).isSameAs(resume);
        verify(resumeRepository).findByMemberEmailWithDetails(EMAIL);
    }

    @Test
    @DisplayName("전체 스킬 목록 조회는 저장소 결과를 그대로 반환한다")
    void findAllSkills_ReturnsRepositoryResult() {
        Skill java = skill(1L, "Java");
        Skill spring = skill(2L, "Spring");
        when(skillRepository.findAll()).thenReturn(List.of(java, spring));

        List<Skill> result = resumeService.findAllSkills();

        assertThat(result).containsExactly(java, spring);
        verify(skillRepository).findAll();
    }

    @Test
    @DisplayName("스킬 추가 시 이력서에 스킬을 반영하고 임베딩을 갱신한다")
    void addSkill_Success() {
        Resume resume = org.mockito.Mockito.mock(Resume.class);
        Skill skill = skill(SKILL_ID, "Java");

        when(memberRepository.findByEmail_Value(EMAIL)).thenReturn(member);
        when(resumeRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(resume));
        when(skillRepository.findById(SKILL_ID)).thenReturn(Optional.of(skill));

        resumeService.addSkill(EMAIL, SKILL_ID, Proficiency.INTERMEDIATE);

        verify(resume).addSkill(skill, Proficiency.INTERMEDIATE);
        verify(resumeEmbeddingService).createOrRefresh(resume);
    }

    @Test
    @DisplayName("경력 추가 시 career JSON을 갱신하고 임베딩을 갱신한다")
    void addCareer_Success() throws Exception {
        Resume resume = storedResumeWithCareer();
        CareerItemPayload item = careerItem("회사A", "백엔드 개발자", false, "2023-01", "2024-06");

        when(memberRepository.findByEmail_Value(EMAIL)).thenReturn(member);
        when(resumeRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(resume));
        when(objectMapper.writeValueAsString(any(CareerPayload.class))).thenReturn("career-json");

        resumeService.addCareer(EMAIL, item);

        assertThat(resume.getCareer().getItems()).containsExactly(item);
        verify(resumeRepository).updateCareerJson(RESUME_ID, "career-json");
        verify(resumeEmbeddingService).createOrRefresh(resume);
    }

    @Test
    @DisplayName("경력 수정 시 career JSON을 갱신하고 임베딩을 갱신한다")
    void updateCareer_Success() throws Exception {
        CareerItemPayload original = careerItem("회사A", "백엔드 개발자", false, "2022-01", "2023-01");
        Resume resume = storedResumeWithCareer(original);
        CareerItemPayload updated = careerItem("회사B", "플랫폼 개발자", true, "2023-02", null);

        when(memberRepository.findByEmail_Value(EMAIL)).thenReturn(member);
        when(resumeRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(resume));
        when(objectMapper.writeValueAsString(any(CareerPayload.class))).thenReturn("updated-career-json");

        resumeService.updateCareer(EMAIL, 0, updated);

        assertThat(resume.getCareer().getItems()).containsExactly(updated);
        verify(resumeRepository).updateCareerJson(RESUME_ID, "updated-career-json");
        verify(resumeEmbeddingService).createOrRefresh(resume);
    }

    @Test
    @DisplayName("경력 삭제 시 career JSON을 갱신하고 임베딩을 갱신한다")
    void removeCareer_Success() throws Exception {
        CareerItemPayload original = careerItem("회사A", "백엔드 개발자", false, "2022-01", "2023-01");
        Resume resume = storedResumeWithCareer(original);

        when(memberRepository.findByEmail_Value(EMAIL)).thenReturn(member);
        when(resumeRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(resume));
        when(objectMapper.writeValueAsString(any(CareerPayload.class))).thenReturn("removed-career-json");

        resumeService.removeCareer(EMAIL, 0);

        assertThat(resume.getCareer().getItems()).isEmpty();
        verify(resumeRepository).updateCareerJson(RESUME_ID, "removed-career-json");
        verify(resumeEmbeddingService).createOrRefresh(resume);
    }

    @Test
    @DisplayName("경력 JSON 직렬화에 실패하면 updateCareerJson과 임베딩 갱신을 수행하지 않는다")
    void addCareer_Fail_WhenCareerSerializationFails() throws Exception {
        Resume resume = storedResumeWithCareer();
        CareerItemPayload item = careerItem("회사A", "백엔드 개발자", false, "2023-01", "2024-06");

        when(memberRepository.findByEmail_Value(EMAIL)).thenReturn(member);
        when(resumeRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(resume));
        when(objectMapper.writeValueAsString(any(CareerPayload.class)))
                .thenThrow(new JsonProcessingException("json failed") {
                });

        assertThatThrownBy(() -> resumeService.addCareer(EMAIL, item))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("경력 JSON 직렬화에 실패했습니다.");

        verify(resumeRepository, never()).updateCareerJson(anyLong(), anyString());
        verifyNoInteractions(resumeEmbeddingService);
    }

    private Resume storedResumeWithCareer(CareerItemPayload... items) {
        CareerPayload career = new CareerPayload();
        career.setItems(List.of(items));

        Resume resume = Resume.create(
                member,
                "기존 자기소개",
                (byte) 3,
                career,
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "https://example.com/existing"
        );
        ReflectionTestUtils.setField(resume, "id", RESUME_ID);
        return resume;
    }

    private Skill skill(Long id, String name) {
        Skill skill = Skill.create(name, null);
        ReflectionTestUtils.setField(skill, "id", id);
        return skill;
    }

    private CareerItemPayload careerItem(
            String companyName,
            String position,
            boolean currentlyWorking,
            String startYearMonth,
            String endYearMonth
    ) {
        CareerItemPayload item = new CareerItemPayload();
        item.setCompanyName(companyName);
        item.setPosition(position);
        item.setEmploymentType(CareerEmploymentType.FULL_TIME);
        item.setStartYearMonth(startYearMonth);
        item.setEndYearMonth(endYearMonth);
        item.setCurrentlyWorking(currentlyWorking);
        item.setSummary(companyName + " 경력");
        item.setTechStack(List.of("Java", "Spring"));
        return item;
    }
}
