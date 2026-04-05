package com.generic4.itda.domain;

import static org.springframework.util.StringUtils.hasText;

import com.generic4.itda.domain.constant.UserRole;
import com.generic4.itda.domain.constant.UserStatus;
import com.generic4.itda.domain.constant.UserType;
import com.generic4.itda.domain.vo.Email;
import com.generic4.itda.domain.vo.Phone;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.springframework.util.Assert;

@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@NaturalIdCache
@Entity
@Table(indexes = {
        @Index(name = "idx_member_email", columnList = "email"),
        @Index(name = "idx_member_name", columnList = "name"),
        @Index(name = "idx_member_created_at", columnList = "created_at"),
        @Index(name = "idx_member_modified_at", columnList = "modified_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(callSuper = false)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NaturalId(mutable = true)
    @Embedded
    private Email email;

    @Column(nullable = false, length = 100)
    private String hashedPassword;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String nickname;

    @Embedded
    private Phone phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    // 프로필 메모 추가 여부

    @Builder
    private Member(Email email, String hashedPassword, String name, String nickname, Phone phone, UserRole role,
            UserType type, UserStatus status) {
        Assert.notNull(email, "email은 필수값입니다.");
        Assert.hasText(hashedPassword, "비밀번호는 필수값입니다.");
        Assert.hasText(name, "이름은 필수값입니다.");
        Assert.notNull(phone, "연락처는 필수값입니다.");

        this.email = email;
        this.hashedPassword = hashedPassword;
        this.name = name;
        this.nickname = (nickname != null && !nickname.isBlank()) ? nickname : name;
        this.phone = phone;
        this.role = role != null ? role : UserRole.USER;
        this.type = type != null ? type : UserType.INDIVIDUAL;
        this.status = status != null ? status : UserStatus.ACTIVE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Member member)) {
            return false;
        }
        return this.getId() != null && Objects.equals(this.getId(), member.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getId());
    }

    public static Member create(String email, String hashedPassword, String name, String nickname, String phone) {
        return Member.builder()
                .email(new Email(email.trim()))
                .hashedPassword(hashedPassword)
                .name(name.trim())
                .nickname(hasText(nickname) ? nickname.trim() : null)
                .phone(new Phone(phone.trim()))
                .role(UserRole.USER)
                .type(UserType.INDIVIDUAL)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
