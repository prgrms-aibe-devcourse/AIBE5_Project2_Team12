package com.generic4.itda.domain.file;

import com.generic4.itda.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.util.Assert;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(callSuper = false)
public class StoredFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false)
    private String storedName;

    @Column(nullable = false)
    private String fileUrl;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @Builder(access = AccessLevel.PRIVATE)
    private StoredFile(String originalName, String storedName, String fileUrl, String contentType, Long size) {
        Assert.hasText(originalName, "원본 파일 이름은 필수값입니다.");
        Assert.hasText(storedName, "저장 파일 이름은 필수값입니다.");
        Assert.hasText(fileUrl, "파일 요청 경로는 필수값입니다.");
        Assert.isTrue(fileUrl.startsWith("/"), "파일 요청 경로는 '/'로 시작해야 합니다.");
        Assert.hasText(contentType, "컨텐츠 타입은 필수값입니다.");
        Assert.notNull(size, "파일 크기는 필수값입니다.");
        Assert.isTrue(size >= 0, "파일 크기는 음수일 수 없습니다.");

        this.originalName = originalName;
        this.storedName = storedName;
        this.fileUrl = fileUrl;
        this.contentType = contentType;
        this.size = size;
    }

    public static StoredFile create(String originalName, String storedName, String fileUrl, String contentType,
            Long size) {
        return new StoredFile(originalName, storedName, fileUrl, contentType, size);
    }

    public String getExtension() {
        int lastDotIndex = originalName.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == originalName.length() - 1) {
            return "";
        }
        return originalName.substring(lastDotIndex + 1);
    }

    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    public String getDisplayName() {
        return originalName;
    }

    public boolean hasSameStoredName(String storedName) {
        return this.storedName.equals(storedName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StoredFile that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
