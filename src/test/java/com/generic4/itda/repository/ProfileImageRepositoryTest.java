package com.generic4.itda.repository;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;

import com.generic4.itda.annotation.H2RepositoryTest;
import com.generic4.itda.domain.file.StoredFile;
import com.generic4.itda.domain.member.Member;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@H2RepositoryTest
class ProfileImageRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StoredFileRepository storedFileRepository;

    @Autowired
    private ProfileImageRepository profileImageRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("회원 프로필 수정 시 프로필 이미지와 메모를 함께 저장하고 기존 파일을 교체할 수 있다")
    @Test
    void saveProfileImageLifecycle() {
        Member member = memberRepository.save(createMember());
        StoredFile firstFile = storedFileRepository.save(
                createStoredFile("avatar.png", "stored-avatar.png", "/files/profile/stored-avatar.png"));

        member.updateProfile(firstFile, "  첫 메모  ");
        memberRepository.saveAndFlush(member);
        entityManager.clear();

        Member persistedMember = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(persistedMember.getMemo()).isEqualTo("첫 메모");
        assertThat(persistedMember.getProfileImage()).isNotNull();
        assertThat(persistedMember.getProfileImage().getFile().getId()).isEqualTo(firstFile.getId());
        assertThat(profileImageRepository.count()).isEqualTo(1L);

        StoredFile secondFile = storedFileRepository.save(
                createStoredFile("avatar-2.png", "stored-avatar-2.png", "/files/profile/stored-avatar-2.png"));

        persistedMember.updateProfile(secondFile, "  두 번째 메모  ");
        memberRepository.saveAndFlush(persistedMember);
        entityManager.clear();

        Member updatedMember = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(updatedMember.getMemo()).isEqualTo("두 번째 메모");
        assertThat(updatedMember.getProfileImage()).isNotNull();
        assertThat(updatedMember.getProfileImage().getFile().getId()).isEqualTo(secondFile.getId());
        assertThat(profileImageRepository.count()).isEqualTo(1L);
    }

    @DisplayName("프로필 이미지를 삭제하면 orphan row 없이 다시 등록할 수 있다")
    @Test
    void removeProfileImageAndRegisterAgain() {
        Member member = memberRepository.save(createMember());
        StoredFile firstFile = storedFileRepository.save(
                createStoredFile("avatar.png", "stored-avatar.png", "/files/profile/stored-avatar.png"));

        member.updateProfile(firstFile, "메모");
        memberRepository.saveAndFlush(member);
        entityManager.clear();

        Member persistedMember = memberRepository.findById(member.getId()).orElseThrow();
        persistedMember.removeProfileImage();
        memberRepository.saveAndFlush(persistedMember);
        entityManager.clear();

        Member memberWithoutImage = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(memberWithoutImage.getProfileImage()).isNull();
        assertThat(profileImageRepository.count()).isZero();

        StoredFile secondFile = storedFileRepository.save(
                createStoredFile("avatar-2.png", "stored-avatar-2.png", "/files/profile/stored-avatar-2.png"));

        memberWithoutImage.updateProfile(secondFile, null);
        memberRepository.saveAndFlush(memberWithoutImage);
        entityManager.clear();

        Member memberWithNewImage = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(memberWithNewImage.getProfileImage()).isNotNull();
        assertThat(memberWithNewImage.getProfileImage().getFile().getId()).isEqualTo(secondFile.getId());
        assertThat(profileImageRepository.count()).isEqualTo(1L);
    }

    private StoredFile createStoredFile(String originalName, String storedName, String fileUrl) {
        return StoredFile.create(originalName, storedName, fileUrl, "image/png", 1024L);
    }
}
