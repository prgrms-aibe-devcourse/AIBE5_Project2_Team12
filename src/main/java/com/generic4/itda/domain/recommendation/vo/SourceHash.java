package com.generic4.itda.domain.recommendation.vo;

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
public class SourceHash {

    @Column(name = "source_hash", length = 128, nullable = false)
    private String value;

    public SourceHash(String value) {
        Assert.hasText(value, "sourceHash는 필수입니다.");

        String normalized = value.trim();
        Assert.isTrue(normalized.length() <= 128, "sourceHash는 128자를 초과할 수 없습니다.");

        this.value = normalized;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SourceHash that)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
