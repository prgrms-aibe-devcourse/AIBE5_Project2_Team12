package com.generic4.itda.service.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.resume.CareerEmploymentType;
import com.generic4.itda.domain.resume.CareerItemPayload;
import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Proficiency;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeSkill;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.fixture.MemberFixture;
import com.generic4.itda.service.recommend.embedding.ResumeEmbeddingTextGenerator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("ResumeEmbeddingTextGenerator 단위 테스트")
class ResumeEmbeddingTextGeneratorTest {

    private final ResumeEmbeddingTextGenerator generator = new ResumeEmbeddingTextGenerator();

    @Nested
    @DisplayName("generate()")
    class GenerateTest {

        @Test
        @DisplayName("resume이 null이면 예외가 발생한다")
        void generate_nullResume_예외발생() {
            assertThatThrownBy(() -> generator.generate(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("resume은 필수입니다.");
        }

        @Test
        @DisplayName("모든 주요 필드가 결과 문자열에 정상적으로 반영된다")
        void generate_모든필드정상반영() {
            // given
            CareerItemPayload item = careerItem(
                    "Generic4", "Backend Engineer", CareerEmploymentType.FULL_TIME,
                    "2022-03", "2024-12", false, "Spring Boot API 개발", List.of("Java", "Spring Boot")
            );
            Resume resume = createResumeWithCareer(careerPayload(item));
            resume.addSkill(Skill.create("Java", null), Proficiency.ADVANCED);
            resume.addSkill(Skill.create("Python", null), Proficiency.BEGINNER);

            // when
            String result = generator.generate(resume);

            // then
            assertThat(result).contains("preferredWorkType: HYBRID");
            assertThat(result).contains("careerYears: 3 years");
            assertThat(result).contains("introduction: 백엔드 개발자입니다.");
            assertThat(result).contains("skills: Java (ADVANCED), Python (BEGINNER)");
            assertThat(result).contains("company: Generic4");
            assertThat(result).contains("position: Backend Engineer");
            assertThat(result).contains("employmentType: 정규직");
            assertThat(result).contains("period: 2022-03~2024-12");
            assertThat(result).contains("summary: Spring Boot API 개발");
            assertThat(result).contains("techStack: Java, Spring Boot");
        }

        @Test
        @DisplayName("결과 문자열이 정해진 섹션 순서를 유지한다")
        void generate_전체포맷_섹션순서검증() {
            // given
            Resume resume = createMinimalResume();

            // when
            String result = generator.generate(resume);

            // then
            assertThat(result).matches(
                    "(?s)preferredWorkType:.*careerYears:.*introduction:.*skills:.*careerDetails:.*");
        }

        @Nested
        @DisplayName("공백 정규화 및 Trim")
        class NormalizationTest {

            @Test
            @DisplayName("introduction의 연속 공백과 줄바꿈은 한 칸 공백으로 정규화된다")
            void generate_introduction_공백정규화() {
                Resume resume = createMinimalResume();
                ReflectionTestUtils.setField(resume, "introduction", "  Hello  \n\n  World  ");

                String result = generator.generate(resume);

                assertThat(result).contains("introduction: Hello World");
            }

            @Test
            @DisplayName("career summary의 공백과 줄바꿈은 한 칸 공백으로 정규화된다")
            void generate_careerSummary_공백정규화() {
                CareerItemPayload item = careerItem(
                        "회사A", "개발자", CareerEmploymentType.FULL_TIME,
                        "2023-01", null, true, "  개발  \n  업무  ", null
                );
                Resume resume = createResumeWithCareer(careerPayload(item));

                String result = generator.generate(resume);

                assertThat(result).contains("summary: 개발 업무");
            }

            @Test
            @DisplayName("companyName과 position에 trim이 적용된다")
            void generate_companyAndPosition_trimApplied() {
                CareerItemPayload item = careerItem(
                        "  Generic4  ", "  Backend  ", CareerEmploymentType.FULL_TIME,
                        "2023-01", null, true, "summary", null
                );
                Resume resume = createResumeWithCareer(careerPayload(item));

                String result = generator.generate(resume);

                assertThat(result).contains("company: Generic4");
                assertThat(result).contains("position: Backend");
            }
        }

        @Nested
        @DisplayName("nullable 필드 처리")
        class NullableFieldsTest {

            @Test
            @DisplayName("주요 필드가 null이거나 비어 있으면 빈 값으로 처리한다")
            void generate_nullable필드_빈값처리() {
                Resume resume = createMinimalResume();
                ReflectionTestUtils.setField(resume, "preferredWorkType", null);
                ReflectionTestUtils.setField(resume, "careerYears", null);
                ReflectionTestUtils.setField(resume, "introduction", null);

                String result = generator.generate(resume);

                assertThat(result).contains("preferredWorkType: \n");
                assertThat(result).contains("careerYears: \n");
                assertThat(result).contains("introduction: \n");
                assertThat(result).contains("skills: \n");
            }

            @Test
            @DisplayName("career가 null이거나 항목이 없으면 careerDetails는 빈 섹션으로 남는다")
            void generate_careerNullOrEmpty_빈섹션처리() {
                Resume resume = createMinimalResume();
                ReflectionTestUtils.setField(resume, "career", null);

                String result = generator.generate(resume);

                assertThat(result).endsWith("careerDetails:");
            }
        }

        @Nested
        @DisplayName("스킬 포맷")
        class SkillFormatTest {

            @Test
            @DisplayName("proficiency가 null이면 스킬 이름만 출력된다")
            void generate_skillProficiency가없으면스킬명만반영() {
                Resume resume = createMinimalResume();
                resume.addSkill(Skill.create("Java", null), Proficiency.ADVANCED);
                ResumeSkill resumeSkill = resume.getSkills().first();
                ReflectionTestUtils.setField(resumeSkill, "proficiency", null);

                String result = generator.generate(resume);

                assertThat(result).contains("skills: Java\n");
            }

            @Test
            @DisplayName("skill 객체 자체가 null인 ResumeSkill은 안전하게 무시한다")
            void generate_nullSkillObject_무시() {
                Resume resume = createMinimalResume();
                resume.addSkill(Skill.create("Java", null), Proficiency.ADVANCED);
                ResumeSkill resumeSkill = resume.getSkills().first();
                ReflectionTestUtils.setField(resumeSkill, "skill", null);

                String result = generator.generate(resume);

                assertThat(result).contains("skills: ");
                assertThat(result).doesNotContain("null");
            }

            @Test
            @DisplayName("여러 스킬이 ', '로 join된다")
            void generate_multipleSkills_joinedWithComma() {
                Resume resume = createMinimalResume();
                resume.addSkill(Skill.create("Docker", null), Proficiency.INTERMEDIATE);
                resume.addSkill(Skill.create("Kotlin", null), Proficiency.BEGINNER);

                String result = generator.generate(resume);

                assertThat(result).contains("skills: Docker (INTERMEDIATE), Kotlin (BEGINNER)");
            }
        }

        @Nested
        @DisplayName("경력 항목 포맷")
        class CareerItemFormatTest {

            @Test
            @DisplayName("여러 경력 항목은 줄바꿈으로 연결되며 입력 순서가 유지된다")
            void generate_multipleCareerItems_joinedWithNewlineAndOrdered() {
                CareerItemPayload item1 = careerItem("회사A", "역할A", null, "2021", "2022", false, null, null);
                CareerItemPayload item2 = careerItem("회사B", "역할B", null, "2022", null, true, null, null);
                Resume resume = createResumeWithCareer(careerPayload(item1, item2));

                String result = generator.generate(resume);

                String details = result.substring(result.indexOf("careerDetails:"));
                assertThat(details).contains("- company: 회사A");
                assertThat(details).contains("- company: 회사B");
                assertThat(details.indexOf("회사A")).isLessThan(details.indexOf("회사B"));
            }

            @Test
            @DisplayName("null인 경력 항목은 결과에서 제외된다")
            void generate_nullCareerItem_제외() {
                List<CareerItemPayload> items = new ArrayList<>();
                items.add(null);
                items.add(careerItem("회사A", "개발자", null, null, null, false, null, null));

                CareerPayload payload = new CareerPayload();
                payload.setItems(items);
                Resume resume = createResumeWithCareer(payload);

                String result = generator.generate(resume);

                assertThat(result).contains("careerDetails:");
                assertThat(result).contains("- company: 회사A");
                assertThat(result).doesNotContain("\n- \n");
            }

            @Test
            @DisplayName("currentlyWorking=true이면 종료일 대신 present가 출력된다")
            void generate_currentlyWorking이면_present_출력() {
                CareerItemPayload item = careerItem("회사A", "개발자", null, "2023-01", null, true, null, null);
                Resume resume = createResumeWithCareer(careerPayload(item));

                String result = generator.generate(resume);

                assertThat(result).contains("period: 2023-01~present");
            }

            @Test
            @DisplayName("startYearMonth와 endYearMonth 조합 로직을 검증한다")
            void generate_period_조합로직() {
                // start만 있는 경우
                CareerItemPayload startOnly = careerItem("A", "P", null, "2023", null, false, null, null);
                assertThat(generator.generate(createResumeWithCareer(careerPayload(startOnly))))
                        .contains("period: 2023 |");

                // end만 있는 경우
                CareerItemPayload endOnly = careerItem("B", "P", null, null, "2024", false, null, null);
                assertThat(generator.generate(createResumeWithCareer(careerPayload(endOnly))))
                        .contains("period: 2024 |");

                // 둘 다 없는 경우
                CareerItemPayload none = careerItem("C", "P", null, null, null, false, null, null);
                assertThat(generator.generate(createResumeWithCareer(careerPayload(none))))
                        .contains("period:  |");
            }
        }

        @Nested
        @DisplayName("techStack 처리")
        class TechStackTest {

            @Test
            @DisplayName("blank 값은 제외하고 나머지를 trim 후 ', '로 join한다")
            void generate_techStack_정규화및join() {
                CareerItemPayload item = careerItem(
                        "회사A", "개발자", null, null, null, false, null,
                        List.of(" Java ", "  ", " Spring Boot ", "")
                );
                Resume resume = createResumeWithCareer(careerPayload(item));

                String result = generator.generate(resume);

                assertThat(result).contains("techStack: Java, Spring Boot");
            }
        }
    }

    // ---- fixture helpers ----

    private Resume createMinimalResume() {
        CareerPayload emptyCareer = new CareerPayload();
        Member member = MemberFixture.createMember();
        return Resume.create(
                member,
                "최소 자기소개",
                (byte) 0,
                emptyCareer,
                WorkType.SITE,
                ResumeWritingStatus.DONE,
                null
        );
    }

    private Resume createResumeWithCareer(CareerPayload career) {
        Member member = MemberFixture.createMember();
        return Resume.create(
                member,
                "백엔드 개발자입니다.",
                (byte) 3,
                career,
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                null
        );
    }

    private CareerPayload careerPayload(CareerItemPayload... items) {
        CareerPayload payload = new CareerPayload();
        payload.setItems(new ArrayList<>(List.of(items)));
        return payload;
    }

    private CareerItemPayload careerItem(
            String companyName,
            String position,
            CareerEmploymentType employmentType,
            String startYearMonth,
            String endYearMonth,
            boolean currentlyWorking,
            String summary,
            List<String> techStack
    ) {
        CareerItemPayload item = new CareerItemPayload();
        item.setCompanyName(companyName);
        item.setPosition(position);
        item.setEmploymentType(employmentType);
        item.setStartYearMonth(startYearMonth);
        item.setEndYearMonth(endYearMonth);
        item.setCurrentlyWorking(currentlyWorking);
        item.setSummary(summary);
        if (techStack != null) {
            item.setTechStack(new ArrayList<>(techStack));
        }
        return item;
    }
}
