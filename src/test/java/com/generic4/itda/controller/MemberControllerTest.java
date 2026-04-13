package com.generic4.itda.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.generic4.itda.annotation.ControllerTest;
import com.generic4.itda.exception.DuplicateEmailException;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerTest(MemberController.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    @DisplayName("회원가입 페이지를 렌더링한다")
    void renderSignupPage() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attributeExists("signUpForm"));
    }

    @Test
    @DisplayName("정상 입력이면 회원가입 후 로그인 페이지로 리다이렉트한다")
    void signUpWithValidInput() throws Exception {
        mockMvc.perform(post("/signup")
                        .with(csrf())
                        .param("email", "new-user@example.com")
                        .param("password", "password123!")
                        .param("name", "홍길동")
                        .param("nickname", "길동")
                        .param("phone", "010-1234-5678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?signupSuccess=true"));

        then(memberService).should().signUp(any());
    }

    @Test
    @DisplayName("중복 이메일이면 회원가입 페이지에 field error를 노출한다")
    void showFieldErrorWhenEmailIsDuplicated() throws Exception {
        doThrow(new DuplicateEmailException("이미 사용 중인 이메일입니다."))
                .when(memberService)
                .signUp(any());

        mockMvc.perform(post("/signup")
                        .with(csrf())
                        .param("email", "duplicated@example.com")
                        .param("password", "password123!")
                        .param("name", "홍길동")
                        .param("nickname", "길동")
                        .param("phone", "010-1234-5678"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attributeHasFieldErrors("signUpForm", "email"));
    }

    @Test
    @DisplayName("검증 오류가 있으면 회원가입 서비스는 호출되지 않는다")
    void doNotCallServiceWhenValidationFails() throws Exception {
        mockMvc.perform(post("/signup")
                        .with(csrf())
                        .param("email", "bad-email")
                        .param("password", "short")
                        .param("name", "")
                        .param("phone", "010 1234 5678"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attributeHasFieldErrors("signUpForm", "email", "password", "name", "phone"));

        then(memberService).should(never()).signUp(any());
    }

    @Test
    @DisplayName("지원하지 않는 숫자-only 연락처 형식은 DTO 검증에서 차단한다")
    void blockUnsupportedNumericPhoneAtValidationLayer() throws Exception {
        mockMvc.perform(post("/signup")
                        .with(csrf())
                        .param("email", "new-user@example.com")
                        .param("password", "password123!")
                        .param("name", "홍길동")
                        .param("nickname", "길동")
                        .param("phone", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attributeHasFieldErrors("signUpForm", "phone"));

        then(memberService).should(never()).signUp(any());
    }
}
