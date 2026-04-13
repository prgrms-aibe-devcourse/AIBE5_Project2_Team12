package com.generic4.itda.domain.member;

import com.generic4.itda.domain.file.StoredFile;
import com.generic4.itda.domain.shared.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false, unique = true)
    private StoredFile file;

    private ProfileImage(StoredFile file) {
        Assert.notNull(file, "파일은 필수 입력값입니다.");

        this.file = file;
    }

    public static ProfileImage create(StoredFile file) {
        return new ProfileImage(file);
    }

    public void changeFile(StoredFile file) {
        Assert.notNull(file, "파일은 필수 입력값입니다.");
        this.file = file;
    }
}
