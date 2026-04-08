package com.generic4.itda.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.generic4.itda.annotation.IntegrationTest;
import com.generic4.itda.domain.StoredFile;
import com.generic4.itda.repository.StoredFileRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@IntegrationTest
class FileStorageServiceIntegrationTest {

    private static final Path UPLOAD_BASE_PATH = createUploadBasePath();

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private StoredFileRepository storedFileRepository;

    @DynamicPropertySource
    static void overrideUploadBasePath(DynamicPropertyRegistry registry) {
        registry.add("file.upload.base-path", () -> UPLOAD_BASE_PATH.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        storedFileRepository.deleteAll();
        cleanUploadDirectory();
    }

    @AfterAll
    static void tearDownAll() throws IOException {
        deleteRecursively(UPLOAD_BASE_PATH);
    }

    @Test
    @DisplayName("제안서 파일 저장 시 메타정보와 실제 파일을 함께 저장한다")
    void storeProposalFile() throws IOException {
        byte[] proposalContent = "proposal-content".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "proposalFile",
                "proposal.pdf",
                "application/pdf",
                proposalContent
        );

        StoredFile storedFile = fileStorageService.storeProposalFile(multipartFile);
        StoredFile persistedFile = storedFileRepository.findById(storedFile.getId()).orElseThrow();
        Path savedFilePath = UPLOAD_BASE_PATH.resolve("proposal").resolve(storedFile.getStoredName());

        assertThat(storedFile.getId()).isNotNull();
        assertThat(storedFileRepository.count()).isEqualTo(1);
        assertThat(persistedFile.getOriginalName()).isEqualTo("proposal.pdf");
        assertThat(persistedFile.getStoredName()).isEqualTo(storedFile.getStoredName());
        assertThat(persistedFile.getFileUrl()).isEqualTo("/files/proposal/" + storedFile.getStoredName());
        assertThat(persistedFile.getContentType()).isEqualTo("application/pdf");
        assertThat(persistedFile.getSize()).isEqualTo((long) proposalContent.length);
        assertThat(Files.exists(savedFilePath)).isTrue();
        assertThat(Files.readString(savedFilePath)).isEqualTo("proposal-content");
    }

    @Test
    @DisplayName("프로필 이미지 삭제 시 메타정보와 실제 파일을 함께 제거한다")
    void deleteProfileImage() throws IOException {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "profileImage",
                "avatar.png",
                "image/png",
                "profile-image".getBytes(StandardCharsets.UTF_8)
        );

        StoredFile storedFile = fileStorageService.storeProfileImage(multipartFile);
        Path savedFilePath = UPLOAD_BASE_PATH.resolve("profile").resolve(storedFile.getStoredName());

        fileStorageService.delete(storedFile);

        assertThat(storedFileRepository.findById(storedFile.getId())).isEmpty();
        assertThat(storedFileRepository.count()).isZero();
        assertThat(Files.exists(savedFilePath)).isFalse();
    }

    private static Path createUploadBasePath() {
        try {
            return Files.createTempDirectory("file-storage-integration-test-");
        } catch (IOException e) {
            throw new UncheckedIOException("통합 테스트 업로드 경로 생성에 실패했습니다.", e);
        }
    }

    private static void cleanUploadDirectory() throws IOException {
        if (Files.notExists(UPLOAD_BASE_PATH)) {
            Files.createDirectories(UPLOAD_BASE_PATH);
            return;
        }
        try (var paths = Files.list(UPLOAD_BASE_PATH)) {
            for (Path child : paths.toList()) {
                deleteRecursively(child);
            }
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (path == null || Files.notExists(path)) {
            return;
        }
        try (var paths = Files.walk(path)) {
            for (Path current : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(current);
            }
        }
    }
}
