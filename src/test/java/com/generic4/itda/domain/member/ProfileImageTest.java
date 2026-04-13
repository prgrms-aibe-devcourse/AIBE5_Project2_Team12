package com.generic4.itda.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.generic4.itda.domain.file.StoredFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProfileImageTest {

    @DisplayName("저장 파일이 주어지면 프로필 이미지를 생성한다")
    @Test
    void createProfileImage() {
        StoredFile storedFile = createStoredFile("avatar.png", "stored-avatar.png", "/files/profile/stored-avatar.png");

        ProfileImage profileImage = ProfileImage.create(storedFile);

        assertThat(profileImage.getFile()).isSameAs(storedFile);
    }

    @DisplayName("파일이 없으면 프로필 이미지 생성에 실패한다")
    @Test
    void failWhenFileIsNull() {
        assertThatThrownBy(() -> ProfileImage.create(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일은 필수 입력값입니다.");
    }

    private StoredFile createStoredFile(String originalName, String storedName, String fileUrl) {
        return StoredFile.create(originalName, storedName, fileUrl, "image/png", 1024L);
    }
}
