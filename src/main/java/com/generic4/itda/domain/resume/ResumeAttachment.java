package com.generic4.itda.domain.resume;

import com.generic4.itda.domain.file.StoredFile;
import com.generic4.itda.domain.shared.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResumeAttachment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private StoredFile file;

    private ResumeAttachment(Resume resume, StoredFile file) {
        Assert.notNull(resume, "이력서는 필수값입니다.");
        Assert.notNull(file, "첨부 파일은 필수값입니다.");

        this.resume = resume;
        this.file = file;
    }

    public static ResumeAttachment create(Resume resume, StoredFile file) {
        return new ResumeAttachment(resume, file);
    }
}
