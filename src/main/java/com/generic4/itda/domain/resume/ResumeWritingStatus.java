package com.generic4.itda.domain.resume;

import lombok.Getter;

@Getter
public enum ResumeWritingStatus {

    WRITING("작성중"), DONE("작성 완료");

    private final String description;

    ResumeWritingStatus(String description) {
        this.description = description;
    }
}
