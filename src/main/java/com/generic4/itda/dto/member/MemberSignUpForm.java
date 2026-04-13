package com.generic4.itda.dto.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MemberSignUpForm {

    private static final String PHONE_REGEX =
            "^(010-\\d{4}-\\d{4}|010\\d{8}|01[1-9]-\\d{3,4}-\\d{4}|01[1-9]\\d{7,8}|"
                    + "02-\\d{3,4}-\\d{4}|02\\d{7,8}|0[3-9][0-9]-\\d{3,4}-\\d{4}|0[3-9]\\d{8,9}|"
                    + "1\\d{3}-\\d{4}|1\\d{7})$";

    @NotBlank(message = "이메일은 필수값입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 254, message = "이메일은 254자를 초과할 수 없습니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수값입니다.")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다.")
    private String password;

    @NotBlank(message = "이름은 필수값입니다.")
    @Size(max = 100, message = "이름은 100자를 초과할 수 없습니다.")
    private String name;

    @Size(max = 100, message = "닉네임은 100자를 초과할 수 없습니다.")
    private String nickname;

    @NotBlank(message = "연락처는 필수값입니다.")
    @Pattern(regexp = PHONE_REGEX, message = "연락처 형식이 올바르지 않습니다.")
    private String phone;
}
