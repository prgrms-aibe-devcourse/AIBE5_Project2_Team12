package com.generic4.itda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.generic4.itda.config.file.FileUploadProperties;
import com.generic4.itda.domain.StoredFile;
import com.generic4.itda.repository.StoredFileRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private StoredFileRepository storedFileRepository;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        FileUploadProperties fileUploadProperties = new FileUploadProperties();
        fileUploadProperties.setBasePath(tempDir.toString());
        fileUploadProperties.setProfileImageDir("profile");
        fileUploadProperties.setProposalDir("proposal");
        fileUploadProperties.setResumeDir("resume");

        fileStorageService = new FileStorageService(fileUploadProperties, storedFileRepository);
    }

    @Test
    @DisplayName("프로필 이미지를 저장하면 파일과 공개 URL을 함께 반환한다")
    void storeProfileImageWithPublicFileUrl() throws IOException {
        stubSaveReturnsArgument();

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
        then(storedFileRepository).should().save(any(StoredFile.class));
    }

    @Test
    @DisplayName("제안서 파일을 저장하면 proposal 공개 URL을 반환한다")
    void storeProposalFileWithPublicFileUrl() {
        stubSaveReturnsArgument();

        MockMultipartFile multipartFile = new MockMultipartFile(
                "proposalFile",
                "proposal.pdf",
                "application/pdf",
                "proposal".getBytes(StandardCharsets.UTF_8)
        );

        StoredFile storedFile = fileStorageService.storeProposalFile(multipartFile);

        assertThat(storedFile.getFileUrl()).isEqualTo("/files/proposal/" + storedFile.getStoredName());
        assertThat(Files.exists(tempDir.resolve("proposal").resolve(storedFile.getStoredName()))).isTrue();
        then(storedFileRepository).should().save(any(StoredFile.class));
    }

    @Test
    @DisplayName("이력서 파일을 저장하면 resume 공개 URL을 반환한다")
    void storeResumeFileWithPublicFileUrl() {
        stubSaveReturnsArgument();

        MockMultipartFile multipartFile = new MockMultipartFile(
                "resumeFile",
                "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "resume".getBytes(StandardCharsets.UTF_8)
        );

        StoredFile storedFile = fileStorageService.storeResumeFile(multipartFile);

        assertThat(storedFile.getFileUrl()).isEqualTo("/files/resume/" + storedFile.getStoredName());
        assertThat(Files.exists(tempDir.resolve("resume").resolve(storedFile.getStoredName()))).isTrue();
        then(storedFileRepository).should().save(any(StoredFile.class));
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
        then(storedFileRepository).should(never()).save(any(StoredFile.class));
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
        then(storedFileRepository).should(never()).save(any(StoredFile.class));
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
        then(storedFileRepository).should(never()).save(any(StoredFile.class));
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
        then(storedFileRepository).should(never()).save(any(StoredFile.class));
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
        then(storedFileRepository).should(never()).save(any(StoredFile.class));
    }

    @Test
    @DisplayName("메타정보 저장에 실패하면 저장된 파일을 정리한다")
    void cleanupSavedFileWhenMetadataSaveFails() {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "profileImage",
                "avatar.png",
                "image/png",
                "profile-image".getBytes(StandardCharsets.UTF_8)
        );
        RuntimeException saveException = new RuntimeException("db save failure");

        given(storedFileRepository.save(any(StoredFile.class))).willThrow(saveException);

        assertThatThrownBy(() -> fileStorageService.storeProfileImage(multipartFile))
                .isSameAs(saveException);

        assertNoSavedFiles("profile");
        then(storedFileRepository).should().save(any(StoredFile.class));
    }

    @Test
    @DisplayName("저장된 프로필 이미지 삭제 시 메타정보와 실제 파일을 함께 제거한다")
    void deleteStoredProfileImage() throws IOException {
        StoredFile storedFile = createStoredFile("avatar.png", "stored-avatar.png", "/files/profile/stored-avatar.png",
                "image/png", 10L);
        Path savedFilePath = createSavedFile("profile", storedFile.getStoredName(), "profile-image");

        fileStorageService.delete(storedFile);

        assertThat(Files.exists(savedFilePath)).isFalse();
        then(storedFileRepository).should().delete(storedFile);
    }

    @Test
    @DisplayName("실제 파일이 이미 없어도 메타정보 삭제는 수행한다")
    void deleteMetadataWhenPhysicalFileIsMissing() {
        StoredFile storedFile = createStoredFile("proposal.pdf", "stored-proposal.pdf",
                "/files/proposal/stored-proposal.pdf", "application/pdf", 10L);

        fileStorageService.delete(storedFile);

        assertThat(Files.exists(tempDir.resolve("proposal").resolve(storedFile.getStoredName()))).isFalse();
        then(storedFileRepository).should().delete(storedFile);
    }

    @Test
    @DisplayName("지원하지 않는 파일 요청 경로는 삭제할 수 없다")
    void failDeleteWhenFileUrlIsUnsupported() {
        StoredFile storedFile = createStoredFile("avatar.png", "stored-avatar.png", "/files/unknown/stored-avatar.png",
                "image/png", 10L);

        assertThatThrownBy(() -> fileStorageService.delete(storedFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("지원하지 않는 파일 요청 경로입니다.");

        then(storedFileRepository).should(never()).delete(any(StoredFile.class));
    }

    @Test
    @DisplayName("파일 요청 경로와 저장 파일 이름이 다르면 삭제할 수 없다")
    void failDeleteWhenFileUrlAndStoredNameDoNotMatch() {
        StoredFile storedFile = createStoredFile("avatar.png", "stored-avatar.png", "/files/profile/another.png",
                "image/png", 10L);

        assertThatThrownBy(() -> fileStorageService.delete(storedFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일 요청 경로와 저장 파일 이름이 일치하지 않습니다.");

        then(storedFileRepository).should(never()).delete(any(StoredFile.class));
    }

    @Test
    @DisplayName("메타정보 삭제가 실패하면 실제 파일은 유지한다")
    void keepPhysicalFileWhenMetadataDeleteFails() throws IOException {
        StoredFile storedFile = createStoredFile("resume.pdf", "stored-resume.pdf", "/files/resume/stored-resume.pdf",
                "application/pdf", 10L);
        Path savedFilePath = createSavedFile("resume", storedFile.getStoredName(), "resume");
        RuntimeException deleteException = new RuntimeException("db delete failure");
        willThrow(deleteException).given(storedFileRepository).delete(storedFile);

        assertThatThrownBy(() -> fileStorageService.delete(storedFile))
                .isSameAs(deleteException);

        assertThat(Files.exists(savedFilePath)).isTrue();
        then(storedFileRepository).should().delete(storedFile);
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

    private void stubSaveReturnsArgument() {
        given(storedFileRepository.save(any(StoredFile.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    private Path createSavedFile(String directoryName, String storedName, String content) throws IOException {
        Path directory = tempDir.resolve(directoryName);
        Files.createDirectories(directory);
        Path savedFilePath = directory.resolve(storedName);
        Files.writeString(savedFilePath, content);
        return savedFilePath;
    }

    private StoredFile createStoredFile(String originalName, String storedName, String fileUrl, String contentType,
            Long size) {
        return StoredFile.create(originalName, storedName, fileUrl, contentType, size);
    }
}
