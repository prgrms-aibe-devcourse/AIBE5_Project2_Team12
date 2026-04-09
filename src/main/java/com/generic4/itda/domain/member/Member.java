package com.generic4.itda.domain.member;

import com.generic4.itda.domain.shared.BaseTimeEntity;
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
import org.springframework.util.StringUtils;

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

    @Builder(access = AccessLevel.PRIVATE)
    private Member(Email email, String hashedPassword, String name, String nickname, Phone phone, UserRole role,
            UserType type, UserStatus status) {
        Assert.notNull(email, "email은 필수값입니다.");
        Assert.hasText(hashedPassword, "비밀번호는 필수값입니다.");
        Assert.hasText(name, "이름은 필수값입니다.");
        Assert.notNull(phone, "연락처는 필수값입니다.");

        this.email = email;
        this.hashedPassword = hashedPassword;
        this.name = name.trim();
        this.nickname = StringUtils.hasText(nickname) ? nickname.trim() : name.trim();
        this.phone = phone;
        this.role = role != null ? role : UserRole.USER;
        this.type = type != null ? type : UserType.INDIVIDUAL;
        this.status = status != null ? status : UserStatus.ACTIVE;
    }

    public static Member create(String email, String hashedPassword, String name, String nickname, String phone) {
        return Member.builder()
                .email(new Email(email))
                .hashedPassword(hashedPassword)
                .name(name)
                .nickname(StringUtils.hasText(nickname) ? nickname : null)
                .phone(new Phone(phone))
                .role(UserRole.USER)
                .type(UserType.INDIVIDUAL)
                .status(UserStatus.ACTIVE)
                .build();
    }

    public void changeHashedPassword(String hashedPassword) {
        Assert.hasText(hashedPassword, "비밀번호는 필수값입니다.");
        this.hashedPassword = hashedPassword.trim();
    }

    public void delete() {
        this.status = UserStatus.INACTIVE;
    }

    public void restore() {
        this.status = UserStatus.ACTIVE;
    }

    public void update(String name, String nickname, String phone) {
        Assert.hasText(name, "이름은 필수값입니다.");
        this.name = name.trim();
        this.nickname = (nickname != null && !nickname.isBlank()) ? nickname.trim() : name.trim();
        this.phone = new Phone(phone);
    }

    // TODO - 사업자 등록 편의 메서드
}
