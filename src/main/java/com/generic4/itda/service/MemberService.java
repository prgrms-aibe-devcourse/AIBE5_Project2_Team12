package com.generic4.itda.service;

import com.generic4.itda.domain.member.Member;
import com.generic4.itda.dto.member.MemberSignUpForm;
import com.generic4.itda.exception.DuplicateEmailException;
import com.generic4.itda.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public void signUp(MemberSignUpForm signUpForm) {
        if (memberRepository.existsByEmail_Value(signUpForm.getEmail())) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
        }

        Member member = Member.create(
                signUpForm.getEmail(),
                passwordEncoder.encode(signUpForm.getPassword()),
                signUpForm.getName(),
                signUpForm.getNickname(),
                null,
                signUpForm.getPhone()
        );

        memberRepository.save(member);
    }
}
