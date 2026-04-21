package com.generic4.itda.controller;

import com.generic4.itda.exception.UnresolvedSkillException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ProposalAiInterviewController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(UnresolvedSkillException.class)
    public ResponseEntity<Map<String, String>> handleUnresolvedSkill(UnresolvedSkillException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", exception.getMessage()));
    }
}