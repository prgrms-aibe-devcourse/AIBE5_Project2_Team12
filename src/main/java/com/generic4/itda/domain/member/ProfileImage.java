package com.generic4.itda.domain.member;

import com.generic4.itda.domain.file.StoredFile;
import com.generic4.itda.domain.shared.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @OneToOne
    @JoinColumn(name = "file_id", nullable = false, unique = true)
    private StoredFile file;

    private ProfileImage(Member member, StoredFile file) {
        Assert.notNull(member, "회원은 필수 입력값입니다.");
        Assert.notNull(file, "파일은 필수 입력값입니다.");

        this.member = member;
        this.file = file;
    }

    public static ProfileImage create(Member member, StoredFile file) {
        return new ProfileImage(member, file);
    }

    public void changeFile(StoredFile file) {
        Assert.notNull(file, "파일은 필수 입력값입니다.");
        this.file = file;
    }
}
