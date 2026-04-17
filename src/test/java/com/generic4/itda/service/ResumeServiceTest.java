package com.generic4.itda.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.resume.*;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.dto.resume.ResumeForm;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.repository.ResumeRepository;
import com.generic4.itda.repository.SkillRepository;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @InjectMocks
    private ResumeService resumeService;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SkillRepository skillRepository;

    private final String EMAIL = "user@test.com";
    private ResumeForm form;
    private Member member;

    @BeforeEach
    void setUp() {
        // ResumeForm.createDefault()는 존재하지 않음 → new ResumeForm() 사용
        form = new ResumeForm();
        form.setIntroduction("수정된 자기소개");
        form.setCareerYears((byte) 5);
        // Resume.create() 빌더 내부에서 Assert.notNull(writingStatus) 호출 → 필수 설정
        form.setWritingStatus(ResumeWritingStatus.WRITING);

        member = mock(Member.class);
    }

    @Test
    @DisplayName("이력서 생성 성공 - 존재 확인 후 회원 조회 순서 보장")
    void create_Success() {
        // given
        when(resumeRepository.existsByMemberEmail(EMAIL)).thenReturn(false);
        when(memberRepository.findByEmail_Value(EMAIL)).thenReturn(member);

        // when
        resumeService.create(EMAIL, form);

        // then
        verify(resumeRepository).save(any(Resume.class));
        verify(memberRepository).findByEmail_Value(EMAIL);
    }

    @Test
    @DisplayName("이력서 생성 실패 - 이미 존재하면 회원 조회를 하지 않음")
    void create_Fail_AlreadyExists() {
        // given
        when(resumeRepository.existsByMemberEmail(EMAIL)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> resumeService.create(EMAIL, form))
                .isInstanceOf(IllegalStateException.class);

        // 검증: 이미 존재하므로 멤버를 찾는 쿼리는 나가지 않아야 함 (리뷰어 포인트)
        verify(memberRepository, never()).findByEmail_Value(anyString());
    }

    @Test
    @DisplayName("이력서 수정 - 엔티티 업데이트 및 토글 로직 검증")
    void update_Success() {
        // given
        Resume resume = mock(Resume.class);
        when(memberRepository.findByEmail_Value(EMAIL)).thenReturn(member);
        when(resumeRepository.findByMemberId(any())).thenReturn(Optional.of(resume));

        // 초기 상태 설정
        when(resume.isPubliclyVisible()).thenReturn(true);
        form.setPubliclyVisible(false); // 변경 대상

        // when
        resumeService.update(EMAIL, form);

        // then
        verify(resume).update(anyString(), anyByte(), any(), any(), any(), any());
        verify(resume).togglePubliclyVisible(); // 값이 다르므로 호출되어야 함
    }

    @Test
    @DisplayName("이력서 조회 - Fetch Join 메서드를 사용하는지 확인")
    void findByEmail_WithFetchJoin() {
        // given
        Resume resume = mock(Resume.class);
        when(resumeRepository.findByMemberEmailWithDetails(EMAIL)).thenReturn(Optional.of(resume));

        // when
        Resume result = resumeService.findByEmail(EMAIL);

        // then
        assertThat(result).isNotNull();
        // 성능 최적화된 레포지토리 메서드 호출 확인
        verify(resumeRepository).findByMemberEmailWithDetails(EMAIL);
    }

    @Test
    @DisplayName("스킬 추가 - 이력서 조회 후 스킬 추가 메서드 호출")
    void addSkill_Success() {
        // given
        Resume resume = mock(Resume.class);
        Skill skill = mock(Skill.class);

        when(memberRepository.findByEmail_Value(EMAIL)).thenReturn(member);
        when(resumeRepository.findByMemberId(any())).thenReturn(Optional.of(resume));
        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill));

        // when
        resumeService.addSkill(EMAIL, 1L, Proficiency.INTERMEDIATE);

        // then
        verify(resume).addSkill(skill, Proficiency.INTERMEDIATE);
    }
}