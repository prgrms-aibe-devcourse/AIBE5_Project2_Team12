package com.generic4.itda.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import com.generic4.itda.dto.resume.ResumeForm;
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
    // GET /resumes
    // =========================================================

    @Nested
    @DisplayName("GET /resumes")
    class IndexTest {

        @Test
        @DisplayName("이력서가 없으면 /resumes/new 로 리다이렉트한다")
        void redirectsToNewWhenNoResume() throws Exception {
            given(resumeService.findByEmail("user@example.com"))
                    .willThrow(new IllegalStateException("이력서가 존재하지 않습니다."));

            mockMvc.perform(get("/resumes").with(authentication(auth)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/resumes/new"));
        }

        @Test
        @DisplayName("이력서가 있으면 /resumes/me 로 리다이렉트한다")
        void redirectsToMeWhenResumeExists() throws Exception {
            Resume resume = mockResume();
            given(resumeService.findByEmail("user@example.com")).willReturn(resume);

            mockMvc.perform(get("/resumes").with(authentication(auth)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/resumes/me"));
        }
    }

    // =========================================================
    // GET /resumes/me  (읽기 전용 조회)
    // =========================================================

    @Nested
    @DisplayName("GET /resumes/me")
    class ViewTest {

        @Test
        @DisplayName("이력서를 읽기 전용 모드(editable=false)로 렌더링한다")
        void renderReadOnlyView() throws Exception {
            Resume resume = mockResume();
            given(resumeService.findByEmail("user@example.com")).willReturn(resume);

            mockMvc.perform(get("/resumes/me").with(authentication(auth)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("freelancer/resumeForm"))
                    .andExpect(model().attribute("isNew", false))
                    .andExpect(model().attribute("editable", false));
        }

        @Test
        @DisplayName("폼 데이터가 이력서 내용으로 채워지고 resume 모델도 포함된다")
        void formIsPopulatedWithResumeData() throws Exception {
            Resume resume = mockResume();
            given(resumeService.findByEmail("user@example.com")).willReturn(resume);

            mockMvc.perform(get("/resumes/me").with(authentication(auth)))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("resumeForm", "resume"));
        }
    }

    // =========================================================
    // GET /resumes/new
    // =========================================================

    @Nested
    @DisplayName("GET /resumes/new")
    class NewFormTest {

        @Test
        @DisplayName("이력서가 없으면 신규 작성 폼을 렌더링한다")
        void renderFormWhenNoResume() throws Exception {
            given(resumeService.findByEmail("user@example.com"))
                    .willThrow(new IllegalStateException("이력서가 존재하지 않습니다."));

            mockMvc.perform(get("/resumes/new").with(authentication(auth)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("freelancer/resumeForm"))
                    .andExpect(model().attributeExists("resumeForm", "resumeSkillForm"))
                    .andExpect(model().attribute("isNew", true));
        }

        @Test
        @DisplayName("이미 이력서가 있으면 /resumes/me 로 리다이렉트한다")
        void redirectsToEditWhenResumeAlreadyExists() throws Exception {
            Resume resume = mockResume();
            given(resumeService.findByEmail("user@example.com")).willReturn(resume);

            mockMvc.perform(get("/resumes/new").with(authentication(auth)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/resumes/me"));
        }
    }

    // =========================================================
    // POST /resumes/new
    // =========================================================

    @Nested
    @DisplayName("POST /resumes/new")
    class CreateTest {

        @Test
        @DisplayName("유효한 입력이면 이력서를 생성하고 /resumes/me 로 리다이렉트한다")
        void createAndRedirectOnValidInput() throws Exception {
            mockMvc.perform(post("/resumes/new").with(authentication(auth)).with(csrf())
                            .param("introduction", "안녕하세요, 백엔드 개발자입니다.")
                            .param("careerYears", "3")
                            .param("career.version", "1")
                            .param("writingStatus", "DONE")
                            .param("preferredWorkType", "REMOTE")
                            .param("portfolioUrl", ""))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/resumes/me"));

            then(resumeService).should().create(eq("user@example.com"), any(ResumeForm.class));
        }

        @Test
        @DisplayName("자기소개가 비어있으면 검증 오류로 폼을 다시 렌더링하고 서비스를 호출하지 않는다")
        void showValidationErrorWhenIntroductionIsBlank() throws Exception {
            mockMvc.perform(post("/resumes/new").with(authentication(auth)).with(csrf())
                            .param("introduction", "")
                            .param("careerYears", "3")
                            .param("career.version", "1")
                            .param("writingStatus", "DONE"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("freelancer/resumeForm"))
                    .andExpect(model().attributeHasFieldErrors("resumeForm", "introduction"));

            then(resumeService).should(never()).create(anyString(), any(ResumeForm.class));
        }

        @Test
        @DisplayName("경력 연차가 범위를 초과하면 검증 오류로 폼을 다시 렌더링한다")
        void showValidationErrorWhenCareerYearsIsOutOfRange() throws Exception {
            mockMvc.perform(post("/resumes/new").with(authentication(auth)).with(csrf())
                            .param("introduction", "안녕하세요.")
                            .param("careerYears", "99")   // max=50 초과
                            .param("career.version", "1")
                            .param("writingStatus", "DONE"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("freelancer/resumeForm"))
                    .andExpect(model().attributeHasFieldErrors("resumeForm", "careerYears"));

            then(resumeService).should(never()).create(anyString(), any(ResumeForm.class));
        }

        @Test
        @DisplayName("서비스에서 예외가 발생하면 전역 오류와 함께 폼을 다시 렌더링한다")
        void showGlobalErrorWhenServiceThrows() throws Exception {
            doThrow(new IllegalStateException("이미 이력서가 존재합니다."))
                    .when(resumeService).create(any(String.class), any(ResumeForm.class));

            mockMvc.perform(post("/resumes/new").with(authentication(auth)).with(csrf())
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
    // GET /resumes/edit  (항상 편집 모드)
    // =========================================================

    @Nested
    @DisplayName("GET /resumes/edit")
    class EditFormTest {

        @Test
        @DisplayName("편집 폼을 항상 editable=true 로 렌더링한다")
        void renderEditableForm() throws Exception {
            Resume resume = mockResume();
            given(resumeService.findByEmail("user@example.com")).willReturn(resume);

            mockMvc.perform(get("/resumes/edit").with(authentication(auth)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("freelancer/resumeForm"))
                    .andExpect(model().attribute("isNew", false))
                    .andExpect(model().attribute("editable", true));
        }

        @Test
        @DisplayName("폼 데이터가 이력서 내용으로 채워지고 필수 모델 속성이 포함된다")
        void formIsPopulatedWithResumeData() throws Exception {
            Resume resume = mockResume();
            given(resumeService.findByEmail("user@example.com")).willReturn(resume);

            mockMvc.perform(get("/resumes/edit").with(authentication(auth)))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("resumeForm", "resume", "resumeSkillForm"))
                    .andExpect(model().attributeExists("workTypes", "proficiencies", "writingStatuses"));
        }
    }

    // =========================================================
    // POST /resumes/edit
    // =========================================================

    @Nested
    @DisplayName("POST /resumes/edit")
    class UpdateTest {

        @Test
        @DisplayName("유효한 입력이면 이력서를 수정하고 /resumes/me 로 리다이렉트한다")
        void updateAndRedirectToMeOnValidInput() throws Exception {
            mockMvc.perform(post("/resumes/edit").with(authentication(auth)).with(csrf())
                            .param("introduction", "수정된 자기소개입니다.")
                            .param("careerYears", "5")
                            .param("career.version", "1")
                            .param("writingStatus", "DONE")
                            .param("preferredWorkType", "HYBRID")
                            .param("publiclyVisible", "true")
                            .param("aiMatchingEnabled", "false"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/resumes/me"));

            then(resumeService).should().update(any(String.class), any(ResumeForm.class));
        }

        @Test
        @DisplayName("검증 오류가 있으면 editable=true 로 폼을 다시 렌더링하고 서비스를 호출하지 않는다")
        void showFormWithEditableTrueOnValidationError() throws Exception {
            Resume resume = mockResume();
            // 검증 오류 시 컨트롤러가 resume을 다시 조회함
            given(resumeService.findByEmail("user@example.com")).willReturn(resume);

            mockMvc.perform(post("/resumes/edit").with(authentication(auth)).with(csrf())
                            .param("introduction", "")   // 필수값 누락
                            .param("careerYears", "5")
                            .param("career.version", "1")
                            .param("writingStatus", "DONE"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("freelancer/resumeForm"))
                    .andExpect(model().attribute("editable", true))
                    .andExpect(model().attributeHasFieldErrors("resumeForm", "introduction"));

            then(resumeService).should(never()).update(any(String.class), any(ResumeForm.class));
        }
    }

    // =========================================================
    // POST /resumes/skill/add
    // =========================================================

    @Nested
    @DisplayName("POST /resumes/skill/add")
    class AddSkillTest {

        @Test
        @DisplayName("스킬 추가 성공 후 skillSection fragment를 반환한다")
        void returnFragmentAfterAddingSkill() throws Exception {
            // given
            Resume resume = mock(Resume.class);
            given(resumeService.findByEmail("user@example.com")).willReturn(resume);

            // when & then
            mockMvc.perform(post("/resumes/skill/add").with(authentication(auth)).with(csrf())
                            .param("skillId", "1")
                            .param("proficiency", "INTERMEDIATE"))
                    .andExpect(status().isOk())
                    // 1. 컨트롤러가 반환하는 View Name 확인
                    .andExpect(view().name("freelancer/resumeForm :: #skillSection"))
                    // 2. 컨트롤러에서 넣은 모델 속성 확인
                    .andExpect(model().attribute("isEditing", true))
                    .andExpect(model().attributeExists("resume", "resumeSkillForm"));

            then(resumeService).should().addSkill("user@example.com", 1L, Proficiency.INTERMEDIATE);
        }

        @Test
        @DisplayName("스킬 ID가 없으면 검증 오류와 함께 skillSection fragment를 반환한다")
        void returnFragmentWithValidationErrorWhenSkillIdIsMissing() throws Exception {
            // given
            Resume resume = mock(Resume.class);
            given(resumeService.findByEmail("user@example.com")).willReturn(resume);

            // when & then
            mockMvc.perform(post("/resumes/skill/add").with(authentication(auth)).with(csrf())
                            .param("proficiency", "INTERMEDIATE")) // skillId 누락
                    .andExpect(status().isOk())
                    .andExpect(view().name("freelancer/resumeForm :: #skillSection"))
                    .andExpect(model().attributeHasFieldErrors("resumeSkillForm", "skillId"));

            then(resumeService).should(never()).addSkill(any(), any(), any());
        }

        @Test
        @DisplayName("이미 등록된 스킬이면 필드 오류와 함께 skillSection fragment를 반환한다")
        void returnFragmentWithErrorWhenSkillAlreadyExists() throws Exception {
            // given
            Resume resume = mock(Resume.class);
            given(resumeService.findByEmail("user@example.com")).willReturn(resume);

            // 비즈니스 예외 상황 시뮬레이션
            doThrow(new IllegalStateException("이미 등록된 스킬입니다."))
                    .when(resumeService).addSkill(any(), any(), any());

            // when & then
            mockMvc.perform(post("/resumes/skill/add").with(authentication(auth)).with(csrf())
                            .param("skillId", "1")
                            .param("proficiency", "INTERMEDIATE"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("freelancer/resumeForm :: #skillSection"))
                    // rejectValue를 통해 skillId 필드에 에러가 담겼는지 확인
                    .andExpect(model().attributeHasFieldErrors("resumeSkillForm", "skillId"));
        }
    }

    // =========================================================
    // POST /resumes/skill/remove
    // =========================================================

    @Nested
    @DisplayName("POST /resumes/skill/remove")
    class RemoveSkillTest {

        @Test
        @DisplayName("스킬 삭제 성공 후 skillSection fragment 를 반환한다")
        void returnFragmentAfterRemovingSkill() throws Exception {
            Resume resume = mockResume();
            given(resumeService.findByEmail("user@example.com")).willReturn(resume);

            mockMvc.perform(post("/resumes/skill/remove").with(authentication(auth)).with(csrf())
                            .param("skillId", "2"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("freelancer/resumeForm :: #skillSection"))
                    .andExpect(model().attribute("isEditing", true));

            then(resumeService).should().removeSkill("user@example.com", 2L);
        }

        @Test
        @DisplayName("skillId가 없으면 서비스 호출 없이 skillSection fragment 를 반환한다")
        void returnFragmentWithoutRemoveWhenSkillIdIsNull() throws Exception {
            Resume resume = mockResume();
            given(resumeService.findByEmail("user@example.com")).willReturn(resume);

            mockMvc.perform(post("/resumes/skill/remove").with(authentication(auth)).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("freelancer/resumeForm :: #skillSection"));

            then(resumeService).should(never()).removeSkill(any(), any());
        }
    }

    // =========================================================
    // POST /resumes/skill/update
    // =========================================================

    @Nested
    @DisplayName("POST /resumes/skill/update")
    class UpdateSkillTest {

        @Test
        @DisplayName("숙련도 변경 성공 후 skillSection fragment 를 반환한다")
        void returnFragmentAfterUpdatingSkill() throws Exception {
            Resume resume = mockResume();
            given(resumeService.findByEmail("user@example.com")).willReturn(resume);

            mockMvc.perform(post("/resumes/skill/update").with(authentication(auth)).with(csrf())
                            .param("skillId", "3")
                            .param("proficiency", "ADVANCED"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("freelancer/resumeForm :: #skillSection"))
                    .andExpect(model().attribute("isEditing", true));

            then(resumeService).should().updateSkill("user@example.com", 3L, Proficiency.ADVANCED);
        }
    }

    // =========================================================
    // POST /resumes/career/add
    // =========================================================

    @Nested
    @DisplayName("POST /resumes/career/add")
    class AddCareerTest {

        @Test
        @DisplayName("종료 연월이 시작 연월보다 빠르면 400과 endYearMonth 오류를 반환한다")
        void return400WhenEndYearMonthIsBeforeStartYearMonth() throws Exception {
            mockMvc.perform(post("/resumes/career/add").with(authentication(auth)).with(csrf())
                            .param("companyName", "ACME")
                            .param("position", "Backend Engineer")
                            .param("employmentType", "FULL_TIME")
                            .param("startYearMonth", "2024-05")
                            .param("endYearMonth", "2024-04")
                            .param("currentlyWorking", "false"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.endYearMonth").exists());

            then(resumeService).should(never()).addCareer(any(), any());
        }

        @Test
        @DisplayName("재직중이 아닌데 종료 연월이 비어있으면 400과 endYearMonth 오류를 반환한다")
        void return400WhenEndYearMonthIsMissing() throws Exception {
            mockMvc.perform(post("/resumes/career/add").with(authentication(auth)).with(csrf())
                            .param("companyName", "ACME")
                            .param("position", "Backend Engineer")
                            .param("employmentType", "FULL_TIME")
                            .param("startYearMonth", "2024-05")
                            .param("endYearMonth", "")
                            .param("currentlyWorking", "false"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.endYearMonth").exists());

            then(resumeService).should(never()).addCareer(any(), any());
        }
    }
}
