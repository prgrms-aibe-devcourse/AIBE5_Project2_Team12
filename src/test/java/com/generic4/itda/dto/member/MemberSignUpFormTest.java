package com.generic4.itda.dto.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.generic4.itda.domain.member.Email;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberSignUpFormTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("지원하는 숫자-only 연락처 형식은 검증에 성공한다")
    void validatePhoneWithoutHyphen() {
        MemberSignUpForm form = createValidForm();
        form.setPhone("01012345678");

        Set<ConstraintViolation<MemberSignUpForm>> violations = validator.validate(form);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("지원하지 않는 숫자-only 연락처 형식은 DTO 검증에서 실패한다")
    void failWhenPhoneDigitsDoNotMatchSupportedFormat() {
        MemberSignUpForm form = createValidForm();
        form.setPhone("123");

        Set<String> messages = validator.validate(form).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertThat(messages).contains("연락처 형식이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("회원가입 DTO에서 허용하는 대표 이메일 형식은 도메인에서도 허용한다")
    void allowRepresentativeEmailsInBothDtoAndDomain() {
        assertEmailAcceptedByDtoAndDomain("new-user@example.com");
        assertEmailAcceptedByDtoAndDomain("user.name+tag@example.co.kr");
    }

    private MemberSignUpForm createValidForm() {
        MemberSignUpForm form = new MemberSignUpForm();
        form.setEmail("new-user@example.com");
        form.setPassword("password123!");
        form.setName("홍길동");
        form.setNickname("길동");
        form.setPhone("010-1234-5678");
        return form;
    }

    private void assertEmailAcceptedByDtoAndDomain(String email) {
        MemberSignUpForm form = createValidForm();
        form.setEmail(email);

        Set<String> messages = validator.validate(form).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertThat(messages).doesNotContain("이메일 형식이 올바르지 않습니다.");
        assertThatCode(() -> new Email(email)).doesNotThrowAnyException();
    }
}
