package com.generic4.itda.domain.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class StoredFileTest {

    @DisplayName("유효한 입력이 주어지면 저장 파일 메타데이터를 생성한다")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validStoredFileSource")
    void createWithValidInputs(
            String originalName,
            String storedName,
            String fileUrl,
            String contentType,
            Long size
    ) {
        StoredFile storedFile = createStoredFile(originalName, storedName, fileUrl, contentType, size);

        assertThat(storedFile.getOriginalName()).isEqualTo(originalName);
        assertThat(storedFile.getStoredName()).isEqualTo(storedName);
        assertThat(storedFile.getFileUrl()).isEqualTo(fileUrl);
        assertThat(storedFile.getContentType()).isEqualTo(contentType);
        assertThat(storedFile.getSize()).isEqualTo(size);
    }

    @DisplayName("원본 파일 이름이 누락되면 저장 파일 메타데이터 생성에 실패한다")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void failWhenOriginalNameIsMissing(String originalName) {
        assertThatThrownBy(() -> createStoredFile(originalName, "stored.png", "/files/profile/stored.png", "image/png", 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("원본 파일 이름은 필수값입니다.");
    }

    @DisplayName("저장 파일 이름이 누락되면 저장 파일 메타데이터 생성에 실패한다")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void failWhenStoredNameIsMissing(String storedName) {
        assertThatThrownBy(() -> createStoredFile("avatar.png", storedName, "/files/profile/stored.png", "image/png", 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("저장 파일 이름은 필수값입니다.");
    }

    @DisplayName("파일 요청 경로가 누락되면 저장 파일 메타데이터 생성에 실패한다")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void failWhenFileUrlIsMissing(String fileUrl) {
        assertThatThrownBy(() -> createStoredFile("avatar.png", "stored.png", fileUrl, "image/png", 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일 요청 경로는 필수값입니다.");
    }

    @DisplayName("파일 요청 경로가 슬래시로 시작하지 않으면 저장 파일 메타데이터 생성에 실패한다")
    @Test
    void failWhenFileUrlDoesNotStartWithSlash() {
        assertThatThrownBy(() -> createStoredFile("avatar.png", "stored.png", "files/profile/stored.png", "image/png", 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일 요청 경로는 '/'로 시작해야 합니다.");
    }

    @DisplayName("컨텐츠 타입이 누락되면 저장 파일 메타데이터 생성에 실패한다")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void failWhenContentTypeIsMissing(String contentType) {
        assertThatThrownBy(() -> createStoredFile("avatar.png", "stored.png", "/files/profile/stored.png", contentType, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("컨텐츠 타입은 필수값입니다.");
    }

    @DisplayName("파일 크기가 누락되면 저장 파일 메타데이터 생성에 실패한다")
    @Test
    void failWhenSizeIsNull() {
        assertThatThrownBy(() -> createStoredFile("avatar.png", "stored.png", "/files/profile/stored.png", "image/png", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일 크기는 필수값입니다.");
    }

    @DisplayName("파일 크기가 음수이면 저장 파일 메타데이터 생성에 실패한다")
    @Test
    void failWhenSizeIsNegative() {
        assertThatThrownBy(() -> createStoredFile("avatar.png", "stored.png", "/files/profile/stored.png", "image/png", -1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일 크기는 음수일 수 없습니다.");
    }

    private static StoredFile createStoredFile(
            String originalName,
            String storedName,
            String fileUrl,
            String contentType,
            Long size
    ) {
        return StoredFile.create(originalName, storedName, fileUrl, contentType, size);
    }

    private static Stream<Arguments> validStoredFileSource() {
        return Stream.of(
                Arguments.of(
                        Named.of("positive file size", "avatar.png"),
                        "stored-avatar.png",
                        "/files/profile/stored-avatar.png",
                        "image/png",
                        1024L
                ),
                Arguments.of(
                        Named.of("zero file size", "resume.pdf"),
                        "stored-resume.pdf",
                        "/files/resume/stored-resume.pdf",
                        "application/pdf",
                        0L
                )
        );
    }
}
