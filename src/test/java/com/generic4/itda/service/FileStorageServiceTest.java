package com.generic4.itda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.generic4.itda.config.file.FileUploadProperties;
import com.generic4.itda.domain.StoredFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        FileUploadProperties fileUploadProperties = new FileUploadProperties();
        fileUploadProperties.setBasePath(tempDir.toString());
        fileUploadProperties.setProfileImageDir("profile");
        fileUploadProperties.setProposalDir("proposal");
        fileUploadProperties.setResumeDir("resume");

        fileStorageService = new FileStorageService(fileUploadProperties);
    }

    @Test
    @DisplayName("프로필 이미지를 저장하면 파일과 공개 URL을 함께 반환한다")
    void storeProfileImageWithPublicFileUrl() throws IOException {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "profileImage",
                "C:\\fakepath\\avatar.PNG",
                "image/png",
                "profile-image".getBytes(StandardCharsets.UTF_8)
        );

        StoredFile storedFile = fileStorageService.storeProfileImage(multipartFile);
        Path savedFilePath = tempDir.resolve("profile").resolve(storedFile.getStoredName());

        assertThat(storedFile.getOriginalName()).isEqualTo("avatar.PNG");
        assertThat(storedFile.getStoredName()).endsWith(".png");
        assertThat(storedFile.getFileUrl()).isEqualTo("/files/profile/" + storedFile.getStoredName());
        assertThat(Files.exists(savedFilePath)).isTrue();
        assertThat(Files.readString(savedFilePath)).isEqualTo("profile-image");
    }

    @Test
    @DisplayName("제안서 파일을 저장하면 proposal 공개 URL을 반환한다")
    void storeProposalFileWithPublicFileUrl() {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "proposalFile",
                "proposal.pdf",
                "application/pdf",
                "proposal".getBytes(StandardCharsets.UTF_8)
        );

        StoredFile storedFile = fileStorageService.storeProposalFile(multipartFile);

        assertThat(storedFile.getFileUrl()).isEqualTo("/files/proposal/" + storedFile.getStoredName());
        assertThat(Files.exists(tempDir.resolve("proposal").resolve(storedFile.getStoredName()))).isTrue();
    }

    @Test
    @DisplayName("이력서 파일을 저장하면 resume 공개 URL을 반환한다")
    void storeResumeFileWithPublicFileUrl() {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "resumeFile",
                "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "resume".getBytes(StandardCharsets.UTF_8)
        );

        StoredFile storedFile = fileStorageService.storeResumeFile(multipartFile);

        assertThat(storedFile.getFileUrl()).isEqualTo("/files/resume/" + storedFile.getStoredName());
        assertThat(Files.exists(tempDir.resolve("resume").resolve(storedFile.getStoredName()))).isTrue();
    }

    @Test
    @DisplayName("빈 파일은 업로드할 수 없다")
    void failWhenFileIsEmpty() {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "profileImage",
                "avatar.png",
                "image/png",
                new byte[0]
        );

        assertThatThrownBy(() -> fileStorageService.storeProfileImage(multipartFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("빈 파일은 업로드할 수 없습니다.");

        assertNoSavedFiles("profile");
    }

    @Test
    @DisplayName("확장자가 없는 파일은 업로드할 수 없다")
    void failWhenOriginalFilenameHasNoExtension() {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "profileImage",
                "avatar",
                "image/png",
                "profile-image".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> fileStorageService.storeProfileImage(multipartFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("확장자가 없는 파일은 업로드할 수 없습니다.");

        assertNoSavedFiles("profile");
    }

    @Test
    @DisplayName("점으로 끝나는 파일명은 업로드할 수 없다")
    void failWhenOriginalFilenameEndsWithDot() {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "resumeFile",
                "resume.",
                "application/pdf",
                "resume".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> fileStorageService.storeResumeFile(multipartFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("확장자가 없는 파일은 업로드할 수 없습니다.");

        assertNoSavedFiles("resume");
    }

    @Test
    @DisplayName("경로가 섞인 파일명은 정제 후 검증한다")
    void failWhenOriginalFilenameContainsPathTraversal() {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "proposalFile",
                "a.txt/../../evil",
                "application/pdf",
                "proposal".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> fileStorageService.storeProposalFile(multipartFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("확장자가 없는 파일은 업로드할 수 없습니다.");

        assertNoSavedFiles("proposal");
    }

    @Test
    @DisplayName("컨텐츠 타입이 없으면 파일을 저장하지 않는다")
    void failWhenContentTypeIsMissingWithoutSavingFile() {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "profileImage",
                "avatar.png",
                null,
                "profile-image".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> fileStorageService.storeProfileImage(multipartFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("컨텐츠 타입은 필수값입니다.");

        assertNoSavedFiles("profile");
    }

    private void assertNoSavedFiles(String directoryName) {
        Path directory = tempDir.resolve(directoryName);

        if (!Files.exists(directory)) {
            return;
        }

        try (var paths = Files.list(directory)) {
            assertThat(paths).isEmpty();
        } catch (IOException e) {
            throw new IllegalStateException("저장 디렉토리 검증에 실패했습니다.", e);
        }
    }
}
