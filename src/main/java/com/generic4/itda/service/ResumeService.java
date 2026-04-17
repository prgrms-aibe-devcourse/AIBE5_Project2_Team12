package com.generic4.itda.service;

import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Proficiency;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.dto.resume.ResumeForm;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.repository.ResumeRepository;
import com.generic4.itda.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final MemberRepository memberRepository;
    private final SkillRepository skillRepository;

    // 이력서 생성
    @Transactional
    public Resume create(String email, ResumeForm form) {
        // 1. 존재 여부 확인 (리뷰어 제안 적용)
        if (resumeRepository.existsByMemberEmail(email)) {
            throw new IllegalStateException("이미 이력서가 존재합니다.");
        }

        // 2. 회원 정보 조회
        Member member = getMemberByEmail(email);

        // 3. 엔티티 생성 시 form에서 데이터를 하나씩 꺼내서 전달
        // ※ 여기서 에러가 났을 겁니다. Resume.create가 요구하는 인자 개수를 맞춰주세요.
        Resume resume = Resume.create(
                member,
                form.getIntroduction(),
                form.getCareerYears(),
                form.getCareer(),
                form.getPreferredWorkType(),
                form.getWritingStatus(),
                form.getPortfolioUrl()
        );

        return resumeRepository.save(resume);
    }

    // 이력서 수정
    public void update(String email, ResumeForm form) {
        // 1. 이메일로 이력서 조회
        Resume resume = getResumeByEmail(email);

        // 2. form에서 데이터를 꺼내서 엔티티 업데이트
        resume.update(
                form.getIntroduction(),
                form.getCareerYears(),
                form.getCareer(),
                form.getPreferredWorkType(), // 서비스에서는 preferredWorkType, 엔티티는 workType일 수 있으니 이름 확인!
                form.getWritingStatus(),
                form.getPortfolioUrl()
        );

        // 3. 토글 로직도 form의 값으로 처리
        if (resume.isPubliclyVisible() != form.isPubliclyVisible()) {
            resume.togglePubliclyVisible();
        }
        if (resume.isAiMatchingEnabled() != form.isAiMatchingEnabled()) {
            resume.toggleAiMatchingEnabled();
        }
    }

    // 스킬 추가
    public void addSkill(String email, Long skillId, Proficiency proficiency) {
        Resume resume = getResumeByEmail(email);
        Skill skill = getSkillById(skillId);
        resume.addSkill(skill, proficiency);
    }

    // 스킬 수정
    public void updateSkill(String email, Long skillId, Proficiency proficiency) {
        Resume resume = getResumeByEmail(email);
        Skill skill = getSkillById(skillId);
        resume.updateSkill(skill, proficiency);
    }

    // 스킬 삭제
    public void removeSkill(String email, Long skillId) {
        Resume resume = getResumeByEmail(email);
        Skill skill = getSkillById(skillId);
        resume.removeSkill(skill);
    }

    // 이력서 조회
    @Transactional(readOnly = true)
    public Resume findByEmail(String email) {
        // 이제 여기서 Fetch Join으로 한 번에 다 가져옵니다.
        // .size() 같은 강제 초기화 코드가 더 이상 필요 없습니다!
        return resumeRepository.findByMemberEmailWithDetails(email)
                .orElseThrow(() -> new IllegalStateException("이력서가 존재하지 않습니다."));
    }

    // 전체 스킬 목록 조회
    @Transactional(readOnly = true)
    public List<Skill> findAllSkills() {
        return skillRepository.findAll();
    }

    // 내부 헬퍼
    private Member getMemberByEmail(String email) {
        Member member = memberRepository.findByEmail_Value(email);
        if (member == null) {
            throw new IllegalArgumentException("존재하지 않는 회원입니다.");
        }
        return member;
    }

    private Resume getResumeByEmail(String email) {
        Member member = getMemberByEmail(email);
        return resumeRepository.findByMemberId(member.getId())
                .orElseThrow(() -> new IllegalStateException("이력서가 존재하지 않습니다."));
    }

    private Skill getSkillById(Long skillId) {
        return skillRepository.findById(skillId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스킬입니다."));
    }
}