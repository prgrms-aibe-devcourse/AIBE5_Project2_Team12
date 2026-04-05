package com.generic4.itda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.generic4.itda.domain.Member;
import com.generic4.itda.domain.constant.UserRole;
import com.generic4.itda.domain.constant.UserStatus;
import com.generic4.itda.domain.constant.UserType;
import com.generic4.itda.dto.member.MemberSignUpForm;
import com.generic4.itda.exception.DuplicateEmailException;
import com.generic4.itda.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    @Captor
    private ArgumentCaptor<Member> memberCaptor;

    @Test
    @DisplayName("회원가입 시 기본 role, type, status로 회원을 저장한다")
    void signUpWithDefaultMemberAttributes() {
        MemberSignUpForm form = new MemberSignUpForm();
        form.setEmail("new-user@example.com");
        form.setPassword("password123!");
        form.setName("홍길동");
        form.setNickname(" ");
        form.setPhone("010-1234-5678");

        given(memberRepository.existsByEmail_Value(form.getEmail())).willReturn(false);
        given(passwordEncoder.encode(form.getPassword())).willReturn("encoded-password");

        memberService.signUp(form);

        then(memberRepository).should().save(memberCaptor.capture());
        Member savedMember = memberCaptor.getValue();

        assertThat(savedMember.getEmail().getValue()).isEqualTo("new-user@example.com");
        assertThat(savedMember.getHashedPassword()).isEqualTo("encoded-password");
        assertThat(savedMember.getName()).isEqualTo("홍길동");
        assertThat(savedMember.getNickname()).isEqualTo("홍길동");
        assertThat(savedMember.getPhone().getValue()).isEqualTo("01012345678");
        assertThat(savedMember.getRole()).isEqualTo(UserRole.USER);
        assertThat(savedMember.getType()).isEqualTo(UserType.INDIVIDUAL);
        assertThat(savedMember.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("중복 이메일이면 회원가입을 중단한다")
    void failWhenEmailAlreadyExists() {
        MemberSignUpForm form = new MemberSignUpForm();
        form.setEmail("duplicated@example.com");
        form.setPassword("password123!");
        form.setName("홍길동");
        form.setPhone("010-1234-5678");

        given(memberRepository.existsByEmail_Value(form.getEmail())).willReturn(true);

        assertThatThrownBy(() -> memberService.signUp(form))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("이미 사용 중인 이메일입니다.");

        then(passwordEncoder).should(never()).encode(any());
        then(memberRepository).should(never()).save(any());
    }
}
