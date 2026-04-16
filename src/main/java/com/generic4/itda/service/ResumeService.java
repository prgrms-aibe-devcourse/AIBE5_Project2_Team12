package com.generic4.itda.service;

import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Proficiency;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import com.generic4.itda.domain.skill.Skill;
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
    public Resume create(
            String email,
            String introduction,
            Byte careerYears,
            CareerPayload career,
            WorkType preferredWorkType,
            ResumeWritingStatus writingStatus,
            String portfolioUrl
    ) {
        Member member = getMemberByEmail(email);

        if (resumeRepository.findByMemberId(member.getId()).isPresent()) {
            throw new IllegalStateException("이미 이력서가 존재합니다.");
        }

        Resume resume = Resume.create(member, introduction, careerYears, career,
                preferredWorkType, writingStatus, portfolioUrl);

        return resumeRepository.save(resume);
    }

    // 이력서 수정
    public void update(
            String email,
            String introduction,
            Byte careerYears,
            CareerPayload career,
            WorkType workType,
            ResumeWritingStatus writingStatus,
            String portfolioUrl
    ) {
        Resume resume = getResumeByEmail(email);
        resume.update(introduction, careerYears, career, workType, writingStatus, portfolioUrl);
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
        Member member = getMemberByEmail(email);
        return resumeRepository.findByMemberId(member.getId())
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