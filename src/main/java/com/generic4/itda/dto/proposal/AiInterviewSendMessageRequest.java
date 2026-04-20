package com.generic4.itda.dto.proposal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AiInterviewSendMessageRequest {

    @NotBlank(message = "AI 인터뷰 메시지는 필수값입니다.")
    @Size(max = 5000, message = "AI 인터뷰 메시지는 5000자를 초과할 수 없습니다.")
    private String message;
}