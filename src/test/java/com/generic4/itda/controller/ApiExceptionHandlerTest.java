package com.generic4.itda.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.generic4.itda.exception.UnresolvedSkillException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler apiExceptionHandler = new ApiExceptionHandler();

    @Test
    @DisplayName("해석할 수 없는 스킬 예외는 400 응답과 메시지로 변환한다")
    void handleUnresolvedSkill_returnsBadRequestWithMessage() {
        UnresolvedSkillException exception = new UnresolvedSkillException("등록되지 않은 스킬입니다: Unknown Skill");

        ResponseEntity<Map<String, String>> response = apiExceptionHandler.handleUnresolvedSkill(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("message", "등록되지 않은 스킬입니다: Unknown Skill");
    }
}