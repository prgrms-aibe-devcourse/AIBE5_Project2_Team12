package com.generic4.itda.domain.member;

import com.generic4.itda.domain.file.StoredFile;
import static com.generic4.itda.fixture.MemberFixture.createMember;
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
import org.springframework.test.util.ReflectionTestUtils;

class MemberTest {

    @DisplayName("유효한 입력이 주어지면 회원을 생성한다")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validMemberSource")
    void createWithValidInputs(
            String email,
            String hashedPassword,
            String name,
            String nickname,
            String phone,
            String expectedName,
            String expectedNickname,
            String expectedPhone
    ) {
        Member member = createMember(email, hashedPassword, name, nickname, phone);

        assertThat(member).isNotNull();
        assertThat(member.getEmail().getValue()).isEqualTo(email);
        assertThat(member.getHashedPassword()).isEqualTo(hashedPassword);
        assertThat(member.getName()).isEqualTo(expectedName);
        assertThat(member.getNickname()).isEqualTo(expectedNickname);
        assertThat(member.getPhone().getValue()).isEqualTo(expectedPhone);
        assertThat(member.getRole()).isEqualTo(UserRole.USER);
        assertThat(member.getType()).isEqualTo(UserType.INDIVIDUAL);
        assertThat(member.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @DisplayName("메모는 trim 후 저장하고 비어 있으면 null로 정규화한다")
    @ParameterizedTest
    @MethodSource("memoNormalizationSource")
    void normalizeMemo(String memo, String expectedMemo) {
        Member member = createMember(
                "test@example.com",
                "hashed-password",
                "홍길동",
                "길동",
                memo,
                "010-1234-5678"
        );

        assertThat(member.getMemo()).isEqualTo(expectedMemo);
    }

    @DisplayName("유효하지 않은 데이터가 주어지면 회원 생성에 실패한다")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidMemberSource")
    void failWhenInvalidInputs(String email, String hashedPassword, String name, String nickname, String phone) {
        assertThatThrownBy(() -> createMember(email, hashedPassword, name, nickname, phone))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("암호화된 비밀번호를 변경한다")
    @ParameterizedTest
    @ValueSource(strings = {
            "new-hashed-password",
            "  new-hashed-password  "
    })
    void changeHashedPasswordWithValidInput(String hashedPassword) {
        Member member = createMember();

        member.changeHashedPassword(hashedPassword);

        assertThat(member.getHashedPassword()).isEqualTo(hashedPassword.trim());
    }

    @DisplayName("암호화된 비밀번호가 누락되면 비밀번호 변경에 실패한다.")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void failChangeHashedPasswordWhenInvalidPassword(String hashedPassword) {
        Member member = createMember();

        assertThatThrownBy(() -> member.changeHashedPassword(hashedPassword))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("회원 상태에 관계없이 삭제를 진행할 수 있다")
    @Test
    void deleteWhateverStatusIsActive() {
        Member member = createMember();
        member.delete();
        assertThat(member.getStatus()).isEqualTo(UserStatus.INACTIVE);

        ReflectionTestUtils.setField(member, "status", UserStatus.INACTIVE);
        member.delete();
        assertThat(member.getStatus()).isEqualTo(UserStatus.INACTIVE);
    }

    @DisplayName("회원 상태에 관계없이 복구를 진행할 수 있다")
    @Test
    void restoreWhateverStatusIsInactive() {
        Member member = createMember();
        member.restore();
        assertThat(member.getStatus()).isEqualTo(UserStatus.ACTIVE);

        ReflectionTestUtils.setField(member, "status", UserStatus.INACTIVE);
        member.restore();
        assertThat(member.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @DisplayName("유효한 입력이 주어진 경우 회원 정보를 수정한다")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validMemberSource")
    void updateWithValidInputs(
            String email,
            String hashedPassword,
            String name,
            String nickname,
            String phone,
            String expectedName,
            String expectedNickname,
            String expectedPhone
    ) {
        Member member = createMember();
        Email expectedEmail = member.getEmail();
        String expectedHashedPassword = member.getHashedPassword();

        member.update(name, nickname, phone);

        assertThat(member.getName()).isEqualTo(expectedName);
        assertThat(member.getNickname()).isEqualTo(expectedNickname);
        assertThat(member.getPhone().getValue()).isEqualTo(expectedPhone);

        assertThat(member.getEmail()).isEqualTo(expectedEmail);
        assertThat(member.getHashedPassword()).isEqualTo(expectedHashedPassword);
        assertThat(member.getRole()).isEqualTo(UserRole.USER);
        assertThat(member.getType()).isEqualTo(UserType.INDIVIDUAL);
        assertThat(member.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @DisplayName("유효하지 않은 데이터가 주어지면 회원 생성에 실패한다")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidUpdateSource")
    void failUpdateWhenInvalidInputs(String name, String nickname, String phone) {
        Member member = createMember();

        assertThatThrownBy(() -> member.update(name, nickname, phone))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("프로필을 수정하면 메모를 정규화하고 프로필 이미지를 생성한다")
    @Test
    void updateProfileCreatesProfileImage() {
        Member member = createMember();
        StoredFile storedFile = createStoredFile("avatar.png", "stored-avatar.png", "/files/profile/stored-avatar.png");

        member.updateProfile(storedFile, "  프로필 메모  ");

        assertThat(member.getMemo()).isEqualTo("프로필 메모");
        assertThat(member.getProfileImage()).isNotNull();
        assertThat(member.getProfileImage().getMember()).isSameAs(member);
        assertThat(member.getProfileImage().getFile()).isSameAs(storedFile);
    }

    @DisplayName("기존 프로필 이미지가 있으면 새 파일로 교체한다")
    @Test
    void updateProfileReplacesExistingImageFile() {
        Member member = createMember();
        StoredFile firstFile = createStoredFile("avatar.png", "stored-avatar.png", "/files/profile/stored-avatar.png");
        StoredFile secondFile = createStoredFile("avatar-2.png", "stored-avatar-2.png",
                "/files/profile/stored-avatar-2.png");

        member.updateProfile(firstFile, "첫 메모");
        ProfileImage profileImage = member.getProfileImage();

        member.updateProfile(secondFile, "  두 번째 메모  ");

        assertThat(member.getMemo()).isEqualTo("두 번째 메모");
        assertThat(member.getProfileImage()).isSameAs(profileImage);
        assertThat(member.getProfileImage().getFile()).isSameAs(secondFile);
    }

    @DisplayName("프로필 이미지 파일이 없으면 메모만 수정한다")
    @Test
    void updateProfileOnlyNormalizesMemoWhenFileIsMissing() {
        Member member = createMember();

        member.updateProfile(null, "  소개 메모  ");

        assertThat(member.getMemo()).isEqualTo("소개 메모");
        assertThat(member.getProfileImage()).isNull();
    }

    @DisplayName("프로필 이미지 삭제 시 연관관계를 제거한다")
    @Test
    void removeProfileImage() {
        Member member = createMember();
        StoredFile storedFile = createStoredFile("avatar.png", "stored-avatar.png", "/files/profile/stored-avatar.png");
        member.updateProfile(storedFile, "메모");

        member.removeProfileImage();

        assertThat(member.getProfileImage()).isNull();
    }

    private StoredFile createStoredFile(String originalName, String storedName, String fileUrl) {
        return StoredFile.create(originalName, storedName, fileUrl, "image/png", 1024L);
    }

    static Stream<Arguments> validMemberSource() {
        return Stream.of(
                Arguments.of(
                        Named.of("all values provided", "test@example.com"),
                        "hashed-password",
                        "dummy",
                        "nickname",
                        "010-1111-1111",
                        "dummy",
                        "nickname",
                        "01011111111"
                ),
                Arguments.of(
                        Named.of("null nickname", "test@example.com"),
                        "hashed-password",
                        "dummy",
                        null,
                        "010-1111-1111",
                        "dummy",
                        "dummy",
                        "01011111111"
                ),
                Arguments.of(
                        Named.of("empty nickname", "test@example.com"),
                        "hashed-password",
                        "dummy",
                        " ",
                        "010-1111-1111",
                        "dummy",
                        "dummy",
                        "01011111111"
                ),
                Arguments.of(
                        Named.of("trim name and nickname", "test@example.com"),
                        "hashed-password",
                        "  dummy  ",
                        "  nickname  ",
                        "010-1111-1111",
                        "dummy",
                        "nickname",
                        "01011111111"
                )
        );
    }

    static Stream<Arguments> invalidMemberSource() {
        return Stream.of(
                Arguments.of(
                        Named.of("null email", null),
                        "hashed-password",
                        "홍길동",
                        "길동",
                        "010-1234-5678"
                ),
                Arguments.of(
                        Named.of("null hashed password", "test@example.com"),
                        null,
                        "홍길동",
                        "길동",
                        "010-1234-5678"
                ),
                Arguments.of(
                        Named.of("blank hashed password", "test@example.com"),
                        " ",
                        "홍길동",
                        "길동",
                        "010-1234-5678"
                ),
                Arguments.of(
                        Named.of("null name", "test@example.com"),
                        "hashed-password",
                        null,
                        "길동",
                        "010-1234-5678"
                ),
                Arguments.of(
                        Named.of("blank name", "test@example.com"),
                        "hashed-password",
                        " ",
                        "길동",
                        "010-1234-5678"
                ),
                Arguments.of(
                        Named.of("null phone", "test@example.com"),
                        "hashed-password",
                        "홍길동",
                        "길동",
                        null
                )
        );
    }

    static Stream<Arguments> invalidUpdateSource() {
        return Stream.of(
                Arguments.of(
                        Named.of("null name", null),
                        "길동",
                        "010-1234-5678"
                ),
                Arguments.of(
                        Named.of("blank name", " "),
                        "길동",
                        "010-1234-5678"
                ),
                Arguments.of(
                        Named.of("null phone", "홍길동"),
                        "길동",
                        null
                )
        );
    }

    static Stream<Arguments> memoNormalizationSource() {
        return Stream.of(
                Arguments.of("  내부 메모  ", "내부 메모"),
                Arguments.of(" ", null),
                Arguments.of(null, null)
        );
    }
}
