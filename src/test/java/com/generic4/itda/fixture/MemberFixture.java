package com.generic4.itda.fixture;

import com.generic4.itda.domain.member.Member;

public class MemberFixture {

    public static Member createMember(String email, String hashedPassword, String name, String nickname, String phone) {
        return Member.create(email, hashedPassword, name, nickname, null, phone);
    }

    public static Member createMember(String email, String hashedPassword, String name, String nickname, String memo,
            String phone) {
        return Member.create(email, hashedPassword, name, nickname, memo, phone);
    }

    public static Member createMember(String email, String hashedPassword, String name, String phone) {
        return Member.create(email, hashedPassword, name, null, null, phone);
    }

    public static Member createMember() {
        return Member.create(
                "test@example.com",
                "hashed-password",
                "dummy",
                null,
                null,
                "010-1234-5678"
        );
    }
}
