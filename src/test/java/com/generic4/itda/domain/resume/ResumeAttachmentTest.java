package com.generic4.itda.domain.resume;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.generic4.itda.domain.file.StoredFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResumeAttachmentTest {

    @DisplayName("유효한 입력이 주어지면 이력서 첨부파일을 생성한다")
    @Test
    void createWithValidInputs() {
        Resume resume = createResume();
        StoredFile file = createStoredFile("resume.pdf");

        ResumeAttachment attachment = ResumeAttachment.create(resume, file);

        assertThat(attachment.getResume()).isEqualTo(resume);
        assertThat(attachment.getFile()).isEqualTo(file);
    }

    @DisplayName("이력서가 누락되면 이력서 첨부파일 생성에 실패한다")
    @Test
    void failWhenResumeIsNull() {
        assertThatThrownBy(() -> ResumeAttachment.create(null, createStoredFile("resume.pdf")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이력서는 필수값입니다.");
    }

    @DisplayName("첨부 파일이 누락되면 이력서 첨부파일 생성에 실패한다")
    @Test
    void failWhenFileIsNull() {
        assertThatThrownBy(() -> ResumeAttachment.create(createResume(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("첨부 파일은 필수값입니다.");
    }

    private static Resume createResume() {
        CareerItemPayload item = new CareerItemPayload();
        item.setCompanyName("Generic4");
        item.setPosition("Backend Engineer");
        item.setEmploymentType(CareerEmploymentType.FULL_TIME);
        item.setStartYearMonth("2024-01");
        item.setCurrentlyWorking(true);
        item.setSummary("Spring Boot 기반 API를 개발했습니다.");
        item.setTechStack(java.util.List.of("Java", "Spring Boot"));

        CareerPayload career = new CareerPayload();
        career.setItems(java.util.List.of(item));

        return Resume.create(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                career,
                WorkType.HYBRID,
                ResumeWritingStatus.WRITING,
                null
        );
    }

    private static StoredFile createStoredFile(String originalName) {
        return StoredFile.create(originalName, "stored-" + originalName, "/files/" + originalName, "application/pdf",
                1024L);
    }
}
