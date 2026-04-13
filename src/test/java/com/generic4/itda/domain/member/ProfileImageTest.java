package com.generic4.itda.domain.member;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.generic4.itda.domain.file.StoredFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProfileImageTest {

    @DisplayName("회원과 저장 파일이 주어지면 프로필 이미지를 생성한다")
    @Test
    void createProfileImage() {
        Member member = createMember();
        StoredFile storedFile = createStoredFile("avatar.png", "stored-avatar.png", "/files/profile/stored-avatar.png");

        ProfileImage profileImage = ProfileImage.create(member, storedFile);

        assertThat(profileImage.getMember()).isSameAs(member);
        assertThat(profileImage.getFile()).isSameAs(storedFile);
    }

    @DisplayName("회원이 없으면 프로필 이미지 생성에 실패한다")
    @Test
    void failWhenMemberIsNull() {
        StoredFile storedFile = createStoredFile("avatar.png", "stored-avatar.png", "/files/profile/stored-avatar.png");

        assertThatThrownBy(() -> ProfileImage.create(null, storedFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("회원은 필수 입력값입니다.");
    }

    @DisplayName("파일이 없으면 프로필 이미지 생성에 실패한다")
    @Test
    void failWhenFileIsNull() {
        Member member = createMember();

        assertThatThrownBy(() -> ProfileImage.create(member, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일은 필수 입력값입니다.");
    }

    @DisplayName("프로필 이미지 파일을 새 파일로 교체한다")
    @Test
    void changeFile() {
        Member member = createMember();
        StoredFile firstFile = createStoredFile("avatar.png", "stored-avatar.png", "/files/profile/stored-avatar.png");
        StoredFile secondFile = createStoredFile("avatar-2.png", "stored-avatar-2.png",
                "/files/profile/stored-avatar-2.png");
        ProfileImage profileImage = ProfileImage.create(member, firstFile);

        profileImage.changeFile(secondFile);

        assertThat(profileImage.getFile()).isSameAs(secondFile);
    }

    @DisplayName("교체할 파일이 없으면 프로필 이미지 수정에 실패한다")
    @Test
    void failWhenChangingFileWithNull() {
        Member member = createMember();
        StoredFile storedFile = createStoredFile("avatar.png", "stored-avatar.png", "/files/profile/stored-avatar.png");
        ProfileImage profileImage = ProfileImage.create(member, storedFile);

        assertThatThrownBy(() -> profileImage.changeFile(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일은 필수 입력값입니다.");
    }

    private StoredFile createStoredFile(String originalName, String storedName, String fileUrl) {
        return StoredFile.create(originalName, storedName, fileUrl, "image/png", 1024L);
    }
}
