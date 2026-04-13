package com.generic4.itda.domain.resume;

import com.generic4.itda.domain.file.StoredFile;
import com.generic4.itda.domain.shared.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_resume_attachments_resume_id_and_display_order",
                        columnNames = {"resume_id", "display_order"}
                )
        }
)
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

    @Column(unique = true)
    private int displayOrder;

    private ResumeAttachment(Resume resume, StoredFile file, int displayOrder) {
        Assert.notNull(resume, "이력서는 필수값입니다.");
        Assert.notNull(file, "첨부 파일은 필수값입니다.");
        Assert.isTrue(displayOrder >= 0, "표시 순서는 음수일 수 없습니다.");

        this.resume = resume;
        this.file = file;
        this.displayOrder = displayOrder;
    }

    public static ResumeAttachment create(Resume resume, StoredFile file, int displayOrder) {
        return new ResumeAttachment(resume, file, displayOrder);
    }

    public void changeDisplayOrder(int displayOrder) {
        Assert.isTrue(displayOrder >= 0, "표시 순서는 음수일 수 없습니다.");
        this.displayOrder = displayOrder;
    }
}
