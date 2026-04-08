package com.generic4.itda.domain.member;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Email {

    private static final String EMAIL_REGEX =
            "^(?=.{1,254}$)(?=.{1,64}@)(?!.*\\.\\.)([A-Za-z0-9]+(?:[._%+-][A-Za-z0-9]+)*)@([A-Za-z0-9]+(?:-[A-Za-z0-9]+)*\\.)+[A-Za-z]{2,}$";

    @Column(name = "email", unique = true, nullable = false)
    private String value;

    public Email(String value) {
        Assert.hasText(value, "이메일은 필수값입니다.");
        Assert.isTrue(value.matches(EMAIL_REGEX), "이메일 형식이 올바르지 않습니다.");
        this.value = value.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Email email)) {
            return false;
        }
        return Objects.equals(value, email.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
