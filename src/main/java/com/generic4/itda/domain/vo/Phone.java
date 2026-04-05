package com.generic4.itda.domain.vo;

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
public class Phone {

    private static final String RAW_PHONE_REGEX =
            "^(01[0-9]-\\d{3,4}-\\d{4}|02-\\d{3,4}-\\d{4}|0[3-9][0-9]-\\d{3,4}-\\d{4}|1\\d{3}-\\d{4}|\\d+)$";

    private static final String NORMALIZED_PHONE_REGEX =
            "^(010\\d{8}|01[1-9]\\d{7,8}|02\\d{7,8}|0[3-9]\\d{8,9}|1\\d{7})$";

    @Column(name = "phone", nullable = false, length = 50)
    private String value;

    public Phone(String value) {
        Assert.hasText(value, "연락처는 필수값입니다.");
        Assert.isTrue(value.matches(RAW_PHONE_REGEX), "연락처 형식이 올바르지 않습니다.");

        String normalized = normalize(value);

        Assert.isTrue(normalized.matches(NORMALIZED_PHONE_REGEX), "지원하지 않는 연락처 형식입니다.");

        this.value = normalized;
    }

    private String normalize(String value) {
        return value.replace("-", "");
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Phone phone)) {
            return false;
        }
        return Objects.equals(value, phone.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}