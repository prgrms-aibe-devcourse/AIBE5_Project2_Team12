package com.generic4.itda.domain.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
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

    @Nested
    @DisplayName("getExtension")
    class GetExtension {

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("com.generic4.itda.domain.file.StoredFileTest#extensionSource")
        @DisplayName("원본 파일 이름에서 확장자를 반환한다")
        void returnsExtension(String originalName, String expectedExtension) {
            StoredFile storedFile = createStoredFile(originalName, "stored.bin", "/files/stored.bin", "application/octet-stream", 0L);

            assertThat(storedFile.getExtension()).isEqualTo(expectedExtension);
        }
    }

    @Nested
    @DisplayName("isImage")
    class IsImage {

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("com.generic4.itda.domain.file.StoredFileTest#imageContentTypeSource")
        @DisplayName("컨텐츠 타입이 image/로 시작하면 true를 반환한다")
        void returnsTrueForImageContentType(String contentType) {
            StoredFile storedFile = createStoredFile("photo.png", "stored.png", "/files/stored.png", contentType, 0L);

            assertThat(storedFile.isImage()).isTrue();
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("com.generic4.itda.domain.file.StoredFileTest#nonImageContentTypeSource")
        @DisplayName("컨텐츠 타입이 image/로 시작하지 않으면 false를 반환한다")
        void returnsFalseForNonImageContentType(String contentType) {
            StoredFile storedFile = createStoredFile("file.pdf", "stored.pdf", "/files/stored.pdf", contentType, 0L);

            assertThat(storedFile.isImage()).isFalse();
        }
    }

    @Nested
    @DisplayName("getDisplayName")
    class GetDisplayName {

        @Test
        @DisplayName("원본 파일 이름을 반환한다")
        void returnsOriginalName() {
            StoredFile storedFile = createStoredFile("avatar.png", "stored.png", "/files/stored.png", "image/png", 0L);

            assertThat(storedFile.getDisplayName()).isEqualTo("avatar.png");
        }
    }

    @Nested
    @DisplayName("hasSameStoredName")
    class HasSameStoredName {

        @Test
        @DisplayName("저장 파일 이름이 같으면 true를 반환한다")
        void returnsTrueWhenSameStoredName() {
            StoredFile storedFile = createStoredFile("avatar.png", "stored-uuid.png", "/files/stored-uuid.png", "image/png", 0L);

            assertThat(storedFile.hasSameStoredName("stored-uuid.png")).isTrue();
        }

        @Test
        @DisplayName("저장 파일 이름이 다르면 false를 반환한다")
        void returnsFalseWhenDifferentStoredName() {
            StoredFile storedFile = createStoredFile("avatar.png", "stored-uuid.png", "/files/stored-uuid.png", "image/png", 0L);

            assertThat(storedFile.hasSameStoredName("other-uuid.png")).isFalse();
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("같은 참조이면 동등하다")
        void sameReference_isEqual() {
            StoredFile file = createStoredFile("a.png", "s.png", "/files/s.png", "image/png", 0L);

            assertThat(file).isEqualTo(file);
        }

        @Test
        @DisplayName("id가 null인 두 인스턴스는 동등하지 않다")
        void nullIdInstances_areNotEqualToEachOther() {
            StoredFile file1 = createStoredFile("a.png", "s1.png", "/files/s1.png", "image/png", 0L);
            StoredFile file2 = createStoredFile("a.png", "s2.png", "/files/s2.png", "image/png", 0L);

            assertThat(file1).isNotEqualTo(file2);
        }

        @Test
        @DisplayName("같은 id를 가지면 동등하다")
        void sameId_areEqual() {
            StoredFile file1 = createStoredFile("a.png", "s1.png", "/files/s1.png", "image/png", 0L);
            StoredFile file2 = createStoredFile("b.pdf", "s2.pdf", "/files/s2.pdf", "application/pdf", 100L);
            ReflectionTestUtils.setField(file1, "id", 1L);
            ReflectionTestUtils.setField(file2, "id", 1L);

            assertThat(file1).isEqualTo(file2);
        }

        @Test
        @DisplayName("다른 id를 가지면 동등하지 않다")
        void differentId_areNotEqual() {
            StoredFile file1 = createStoredFile("a.png", "s1.png", "/files/s1.png", "image/png", 0L);
            StoredFile file2 = createStoredFile("a.png", "s2.png", "/files/s2.png", "image/png", 0L);
            ReflectionTestUtils.setField(file1, "id", 1L);
            ReflectionTestUtils.setField(file2, "id", 2L);

            assertThat(file1).isNotEqualTo(file2);
        }

        @Test
        @DisplayName("null과 비교하면 동등하지 않다")
        void notEqualToNull() {
            StoredFile file = createStoredFile("a.png", "s.png", "/files/s.png", "image/png", 0L);

            assertThat(file).isNotEqualTo(null);
        }

        @Test
        @DisplayName("다른 타입과 비교하면 동등하지 않다")
        void notEqualToDifferentType() {
            StoredFile file = createStoredFile("a.png", "s.png", "/files/s.png", "image/png", 0L);

            assertThat(file).isNotEqualTo("a.png");
        }

        @Test
        @DisplayName("id가 null이면 hashCode는 0이다")
        void nullIdHashCode_isZero() {
            StoredFile file = createStoredFile("a.png", "s.png", "/files/s.png", "image/png", 0L);

            assertThat(file.hashCode()).isZero();
        }

        @Test
        @DisplayName("같은 id를 가지면 hashCode가 동일하다")
        void sameId_haveSameHashCode() {
            StoredFile file1 = createStoredFile("a.png", "s1.png", "/files/s1.png", "image/png", 0L);
            StoredFile file2 = createStoredFile("b.pdf", "s2.pdf", "/files/s2.pdf", "application/pdf", 100L);
            ReflectionTestUtils.setField(file1, "id", 42L);
            ReflectionTestUtils.setField(file2, "id", 42L);

            assertThat(file1.hashCode()).isEqualTo(file2.hashCode());
        }
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

    static Stream<Arguments> extensionSource() {
        return Stream.of(
                Arguments.of(Named.of("단일 확장자", "avatar.png"), "png"),
                Arguments.of(Named.of("복합 확장자 (마지막만 반환)", "archive.tar.gz"), "gz"),
                Arguments.of(Named.of("점이 없는 파일명", "README"), ""),
                Arguments.of(Named.of("점으로 끝나는 파일명", "file."), "")
        );
    }

    static Stream<String> imageContentTypeSource() {
        return Stream.of("image/png", "image/jpeg", "image/gif", "image/webp");
    }

    static Stream<String> nonImageContentTypeSource() {
        return Stream.of("application/pdf", "text/plain", "application/octet-stream", "video/mp4");
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
