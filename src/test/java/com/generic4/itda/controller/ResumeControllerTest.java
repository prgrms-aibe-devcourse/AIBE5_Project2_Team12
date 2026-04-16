package com.generic4.itda.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.generic4.itda.annotation.ControllerTest;
import com.generic4.itda.domain.member.UserRole;
import com.generic4.itda.domain.member.UserStatus;
import com.generic4.itda.domain.member.UserType;
import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Proficiency;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.service.ResumeService;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerTest(ResumeController.class)
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResumeService resumeService;

    @MockitoBean
    private MemberRepository memberRepository;

    @Mock
    private ItDaPrincipal principal;

    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        given(principal.getEmail()).willReturn("user@example.com");
        given(principal.getName()).willReturn("홍길동");
        given(principal.getPhone()).willReturn("010-1234-5678");
        given(principal.getRole()).willReturn(UserRole.USER);
        given(principal.getStatus()).willReturn(UserStatus.ACTIVE);
        given(principal.getType()).willReturn(UserType.INDIVIDUAL);
        given(principal.isEnabled()).willReturn(true);

        given(resumeService.findAllSkills()).willReturn(List.of());

        auth = new UsernamePasswordAuthenticationToken(
                principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    /** 이력서 Mock 객체 생성 헬퍼 */
    private Resume mockResume() {
        Resume resume = mock(Resume.class);
        given(resume.getIntroduction()).willReturn("안녕하세요, 개발자입니다.");
        given(resume.getCareerYears()).willReturn((byte) 3);
        given(resume.getCareer()).willReturn(new CareerPayload());
        given(resume.getPreferredWorkType()).willReturn(WorkType.REMOTE);
        given(resume.getWritingStatus()).willReturn(ResumeWritingStatus.DONE);
        given(resume.getPortfolioUrl()).willReturn("https://portfolio.example.com");
        given(resume.isPubliclyVisible()).willReturn(true);
        given(resume.isAiMatchingEnabled()).willReturn(true);
        given(resume.getSkills()).willReturn(new TreeSet<>());
        given(resume.getAttachments()).willReturn(new ArrayList<>());
        return resume;
    }

    // =========================================================
    // GET /resume
    // =========================================================

    @Nested
    @DisplayName("GET /resume")
    class IndexTest {

        @Test
        @DisplayName("이력서가 없으면 /resume/new 로 리다이렉트한다")
        void redirectsToNewWhenNoResume() throws Exception {
            given(resumeService.findByEmail("user@example.com"))
                    .willThrow(new IllegalStateException("이력서가 존재하지 않습니다."));

            mockMvc.perform(get("/resume").with(authentication(auth)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/resume/new"));
        }

        @Test
        @DisplayName("이력서가 있으면 /resume/edit 로 리다이렉트한다")
        void redirectsToEditWhenResumeExists() throws Exception {
            Resume resume = mockResume();
            given(resumeService.findByEmail("user@example.com")).willReturn(resume);

            mockMvc.perform(get("/resume").with(authentication(auth)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/resume/edit"));
        }
    }

    // =========================================================
    // GET /resume/new
    // =========================================================

    @Nested
    @DisplayName("GET /resume/new")
    class NewFormTest {

        @Test
        @DisplayName("이력서가 없으면 신규 작성 폼을 렌더링한다")
        void renderFormWhenNoResume() throws Exception {
            given(resumeService.findByEmail("user@example.com"))
                    .willThrow(new IllegalStateException("이력서가 존재하지 않습니다."));

            mockMvc.perform(get("/resume/new").with(authentication(auth)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("freelancer/resumeForm"))
                    .andExpect(model().attributeExists("resumeForm", "resumeSkillForm"))
                    .andExpect(model().attribute("isNew", true));
        }

        @Test
        @DisplayName("이미 이력서가 있으면 /resume/edit 로 리다이렉트한다")
        void redirectsToEditWhenResumeAlreadyExists() throws Exception {
            Resume resume = mockResume();
            given(resumeService.findByEmail("user@example.com")).willReturn(resume);

            mockMvc.perform(get("/resume/new").with(authentication(auth)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/resume/edit"));
        }
    }

    // =========================================================
    // POST /resume/new
    // =========================================================

    @Nested
    @DisplayName("POST /resume/new")
    class CreateTest {

        @Test
        @DisplayName("유효한 입력이면 이력서를 생성하고 /resume/edit 로 리다이렉트한다")
        void createAndRedirectOnValidInput() throws Exception {
            mockMvc.perform(post("/resume/new").with(authentication(auth)).with(csrf())
                            .param("introduction", "안녕하세요, 백엔드 개발자입니다.")
                            .param("careerYears", "3")
                            .param("career.version", "1")
                            .param("writingStatus", "DONE")
                            .param("preferredWorkType", "REMOTE")
                            .param("portfolioUrl", ""))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/resume/edit"));

            then(resumeService).should().create(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("자기소개가 비어있으면 검증 오류로 폼을 다시 렌더링하고 서비스를 호출하지 않는다")
        void showValidationErrorWhenIntroductionIsBlank() throws Exception {
            mockMvc.perform(post("/resume/new").with(authentication(auth)).with(csrf())
                            .param("introduction", "")
                            .param("careerYears", "3")
                            .param("career.version", "1")
                            .param("writingStatus", "DONE"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("freelancer/resumeForm"))
                    .andExpect(model().attributeHasFieldErrors("resumeForm", "introduction"));

            then(resumeService).should(never()).create(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("경력 연차가 범위를 초과하면 검증 오류로 폼을 다시 렌더링한다")
        void showValidationErrorWhenCareerYearsIsOutOfRange() throws Exception {
            mockMvc.perform(post("/resume/new").with(authentication(auth)).with(csrf())
                            .param("introduction", "안녕하세요.")
                            .param("careerYears", "99")   // max=50 초과
                            .param("career.version", "1")
                            .param("writingStatus", "DONE"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("freelancer/resumeForm"))
                    .andExpect(model().attributeHasFieldErrors("resumeForm", "careerYears"));

            then(resumeService).should(never()).create(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("서비스에서 예외가 발생하면 전역 오류와 함께 폼을 다시 렌더링한다")
        void showGlobalErrorWhenServiceThrows() throws Exception {
            doThrow(new IllegalStateException("이미 이력서가 존재합니다."))
                    .when(resumeService).create(any(), any(), any(), any(), any(), any(), any());

            mockMvc.perform(post("/resume/new").with(authentication(auth)).with(csrf())
                            .param("introduction", "안녕하세요.")
                            .param("careerYears", "3")
                            .param("career.version", "1")
                            .param("writingStatus", "DONE"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("freelancer/resumeForm"))
                    .andExpect(model().attributeHasErrors("resumeForm"));
        }
    }

    // =========================================================
    // GET /resume/edit
    // =========================================================

    @Nested
    @DisplayName("GET /resume/edit")
    class EditFormTest {

        @Test
        @DisplayName("mode 파라미터가 없으면 읽기 전용 모드(editable=false)로 렌더링한다")
        void renderReadOnlyModeByDefault() throws Exception {
            Resume resume = mockResume();
            given(resumeService.findByEmail("user@example.com")).willReturn(resume);

            mockMvc.perform(get("/resume/edit").with(authentication(auth)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("freelancer/resumeForm"))
                    .andExpect(model().attribute("isNew", false))
                    .andExpect(model().attribute("editable", false));
        }

        @Test
        @DisplayName("mode=modify 이면 편집 모드(editable=true)로 렌더링한다")
        void renderEditModeWhenModeIsModify() throws Exception {
            Resume resume = mockResume();
            given(resumeService.findByEmail("user@example.com")).willReturn(resume);

            mockMvc.perform(get("/resume/edit").param("mode", "modify").with(authentication(auth)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("freelancer/resumeForm"))
                    .andExpect(model().attribute("isNew", false))
                    .andExpect(model().attribute("editable", true));
        }

        @Test
        @DisplayName("폼 데이터가 이력서 내용으로 채워진다")
        void formIsPopulatedWithResumeData() throws Exception {
            Resume resume = mockResume();
            given(resumeService.findByEmail("user@example.com")).willReturn(resume);

            mockMvc.perform(get("/resume/edit").with(authentication(auth)))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("resumeForm", "resume"))
                    .andExpect(model().attributeExists("workTypes", "proficiencies", "writingStatuses"));
        }
    }

    // =========================================================
    // POST /resume/edit
    // =========================================================

    @Nested
    @DisplayName("POST /resume/edit")
    class UpdateTest {

        @Test
        @DisplayName("유효한 입력이면 이력서를 수정하고 /resume/edit 로 리다이렉트한다")
        void updateAndRedirectOnValidInput() throws Exception {
            mockMvc.perform(post("/resume/edit").with(authentication(auth)).with(csrf())
                            .param("introduction", "수정된 자기소개입니다.")
                            .param("careerYears", "5")
                            .param("career.version", "1")
                            .param("writingStatus", "DONE")
                            .param("preferredWorkType", "HYBRID")
                            .param("publiclyVisible", "true")
                            .param("aiMatchingEnabled", "false"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/resume/edit"));

            then(resumeService).should()
                    .update(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), anyBoolean());
        }

        @Test
        @DisplayName("검증 오류가 있으면 editable=true 로 폼을 다시 렌더링하고 서비스를 호출하지 않는다")
        void showFormWithEditableTrueOnValidationError() throws Exception {
            mockMvc.perform(post("/resume/edit").with(authentication(auth)).with(csrf())
                            .param("introduction", "")   // 필수값 누락
                            .param("careerYears", "5")
                            .param("career.version", "1")
                            .param("writingStatus", "DONE"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("freelancer/resumeForm"))
                    .andExpect(model().attribute("editable", true))
                    .andExpect(model().attributeHasFieldErrors("resumeForm", "introduction"));

            then(resumeService).should(never())
                    .update(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), anyBoolean());
        }
    }

    // =========================================================
    // POST /resume/skill/add
    // =========================================================

    @Nested
    @DisplayName("POST /resume/skill/add")
    class AddSkillTest {

        @Test
        @DisplayName("스킬 추가 성공 후 편집 모드(/resume/edit?mode=modify)로 리다이렉트한다")
        void redirectToEditModeAfterAddingSkill() throws Exception {
            mockMvc.perform(post("/resume/skill/add").with(authentication(auth)).with(csrf())
                            .param("skillId", "1")
                            .param("proficiency", "INTERMEDIATE"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/resume/edit?mode=modify"));

            then(resumeService).should().addSkill("user@example.com", 1L, Proficiency.INTERMEDIATE);
        }

        @Test
        @DisplayName("스킬 ID가 없으면 검증 오류로 폼을 다시 렌더링한다")
        void showValidationErrorWhenSkillIdIsMissing() throws Exception {
            Resume resume = mockResume();
            given(resumeService.findByEmail("user@example.com")).willReturn(resume);

            mockMvc.perform(post("/resume/skill/add").with(authentication(auth)).with(csrf())
                            .param("proficiency", "INTERMEDIATE"))   // skillId 누락
                    .andExpect(status().isOk())
                    .andExpect(view().name("freelancer/resumeForm"))
                    .andExpect(model().attributeHasFieldErrors("resumeSkillForm", "skillId"));

            then(resumeService).should(never()).addSkill(any(), any(), any());
        }

        @Test
        @DisplayName("이미 등록된 스킬이면 전역 오류와 함께 폼을 다시 렌더링한다")
        void showGlobalErrorWhenSkillAlreadyExists() throws Exception {
            Resume resume = mockResume();
            given(resumeService.findByEmail("user@example.com")).willReturn(resume);
            doThrow(new IllegalStateException("이미 등록된 스킬입니다."))
                    .when(resumeService).addSkill(any(), any(), any());

            mockMvc.perform(post("/resume/skill/add").with(authentication(auth)).with(csrf())
                            .param("skillId", "1")
                            .param("proficiency", "INTERMEDIATE"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("freelancer/resumeForm"))
                    .andExpect(model().attributeHasErrors("resumeSkillForm"));
        }
    }

    // =========================================================
    // POST /resume/skill/remove
    // =========================================================

    @Nested
    @DisplayName("POST /resume/skill/remove")
    class RemoveSkillTest {

        @Test
        @DisplayName("스킬 삭제 성공 후 편집 모드(/resume/edit?mode=modify)로 리다이렉트한다")
        void redirectToEditModeAfterRemovingSkill() throws Exception {
            mockMvc.perform(post("/resume/skill/remove").with(authentication(auth)).with(csrf())
                            .param("skillId", "2"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/resume/edit?mode=modify"));

            then(resumeService).should().removeSkill("user@example.com", 2L);
        }

        @Test
        @DisplayName("skillId가 없으면 서비스 호출 없이 /resume/edit 로 리다이렉트한다")
        void skipRemoveAndRedirectWhenSkillIdIsNull() throws Exception {
            mockMvc.perform(post("/resume/skill/remove").with(authentication(auth)).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/resume/edit"));

            then(resumeService).should(never()).removeSkill(any(), any());
        }
    }
}
