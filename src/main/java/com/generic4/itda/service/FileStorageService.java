package com.generic4.itda.service;

import com.generic4.itda.config.file.FileUploadProperties;
import com.generic4.itda.domain.StoredFile;
import com.generic4.itda.repository.StoredFileRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final Pattern SAFE_EXTENSION_PATTERN = Pattern.compile("\\.[A-Za-z0-9]{1,10}");

    private final FileUploadProperties fileUploadProperties;
    private final StoredFileRepository storedFileRepository;

    public StoredFile storeProfileImage(MultipartFile multipartFile) {
        return store(multipartFile, fileUploadProperties.getProfileImageDirectory(), "/files/profile");
    }

    public StoredFile storeProposalFile(MultipartFile multipartFile) {
        return store(multipartFile, fileUploadProperties.getProposalDirectory(), "/files/proposal");
    }

    public StoredFile storeResumeFile(MultipartFile multipartFile) {
        return store(multipartFile, fileUploadProperties.getResumeDirectory(), "/files/resume");
    }

    private StoredFile store(MultipartFile multipartFile, Path directory, String fileUrlBasePath) {
        Assert.notNull(multipartFile, "파일은 필수값입니다.");
        Assert.isTrue(!multipartFile.isEmpty(), "빈 파일은 업로드할 수 없습니다.");

        String originalName = extractOriginalName(multipartFile.getOriginalFilename());
        String storedName = generateStoredFilename(originalName);
        String contentType = multipartFile.getContentType();
        Assert.hasText(contentType, "컨텐츠 타입은 필수값입니다.");

        Path baseDirectory = directory.toAbsolutePath().normalize();
        Path targetPath = baseDirectory.resolve(storedName).normalize();
        Assert.isTrue(targetPath.startsWith(baseDirectory), "잘못된 파일명입니다.");

        createDirectoryIfNotExist(baseDirectory);

        try (InputStream inputStream = multipartFile.getInputStream()) {
            Files.copy(inputStream, targetPath);
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장에 실패했습니다.", e);
        }

        StoredFile storedFile = StoredFile.create(
                originalName,
                storedName,
                buildFileUrl(fileUrlBasePath, storedName),
                contentType,
                multipartFile.getSize()
        );

        try {
            return storedFileRepository.save(storedFile);
        } catch (RuntimeException e) {
            deleteSavedFileQuietly(targetPath, e);
            throw e;
        }
    }

    private void createDirectoryIfNotExist(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IllegalStateException("업로드 디렉토리 생성에 실패했습니다.", e);
        }
    }

    private void deleteSavedFileQuietly(Path targetPath, RuntimeException cause) {
        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException cleanupException) {
            cause.addSuppressed(cleanupException);
        }
    }

    private String generateStoredFilename(String originalFilename) {
        String extension = extractExtension(originalFilename);
        return UUID.randomUUID().toString() + extension;
    }

    private String extractOriginalName(String originalFilename) {
        Assert.hasText(originalFilename, "원본 파일명은 필수값입니다.");

        int lastSeparatorIndex = Math.max(originalFilename.lastIndexOf('/'), originalFilename.lastIndexOf('\\'));
        String sanitizedFilename = originalFilename.substring(lastSeparatorIndex + 1);
        Assert.hasText(sanitizedFilename, "원본 파일명은 필수값입니다.");

        return sanitizedFilename;
    }

    private String extractExtension(String originalFilename) {
        int lastDotIndex = originalFilename.lastIndexOf('.');
        Assert.isTrue(lastDotIndex > 0 && lastDotIndex < originalFilename.length() - 1,
                "확장자가 없는 파일은 업로드할 수 없습니다.");

        String extension = originalFilename.substring(lastDotIndex).toLowerCase(Locale.ROOT);
        Assert.isTrue(SAFE_EXTENSION_PATTERN.matcher(extension).matches(), "허용되지 않는 확장자 형식입니다.");

        return extension;
    }

    private String buildFileUrl(String fileUrlBasePath, String storedName) {
        return fileUrlBasePath + "/" + storedName;
    }
}
