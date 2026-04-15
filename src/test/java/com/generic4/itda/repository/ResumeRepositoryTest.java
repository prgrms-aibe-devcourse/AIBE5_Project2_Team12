package com.generic4.itda.repository;

import org.springframework.dao.DataIntegrityViolationException;
import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.generic4.itda.annotation.H2RepositoryTest;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.resume.CareerEmploymentType;
import com.generic4.itda.domain.resume.CareerItemPayload;
import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@H2RepositoryTest
class ResumeRepositoryTest {

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("경력이 없는 CareerPayload도 JSON으로 정상 저장/조회된다")
    @Test
    void saveAndLoad_emptyCareer() {
        Member member = memberRepository.save(createMember());
        CareerPayload career = new CareerPayload();

        Resume resume = Resume.create(member, "자기소개입니다.", (byte) 0, career, WorkType.REMOTE,
                ResumeWritingStatus.WRITING, null);
        resumeRepository.saveAndFlush(resume);
        entityManager.clear();

        Resume found = resumeRepository.findById(resume.getId()).orElseThrow();
        assertThat(found.getCareer()).isNotNull();
        assertThat(found.getCareer().getVersion()).isEqualTo(1);
        assertThat(found.getCareer().getItems()).isEmpty();
    }

    @DisplayName("재직 중인 CareerItemPayload는 endYearMonth 없이 저장/조회된다")
    @Test
    void saveAndLoad_currentlyWorkingCareerItem() {
        Member member = memberRepository.save(createMember());

        CareerItemPayload item = new CareerItemPayload();
        item.setCompanyName("카카오");
        item.setPosition("백엔드 개발자");
        item.setEmploymentType(CareerEmploymentType.FULL_TIME);
        item.setStartYearMonth("2022-03");
        item.setCurrentlyWorking(true);
        item.setSummary("Spring Boot 기반 서비스 개발");
        item.setTechStack(List.of("Java", "Spring Boot", "MySQL"));

        CareerPayload career = new CareerPayload();
        career.getItems().add(item);

        Resume resume = Resume.create(member, "자기소개", (byte) 3, career, WorkType.REMOTE,
                ResumeWritingStatus.WRITING, null);
        resumeRepository.saveAndFlush(resume);
        entityManager.clear();

        Resume found = resumeRepository.findById(resume.getId()).orElseThrow();
        CareerPayload loadedCareer = found.getCareer();

        assertThat(loadedCareer.getVersion()).isEqualTo(1);
        assertThat(loadedCareer.getItems()).hasSize(1);

        CareerItemPayload loadedItem = loadedCareer.getItems().get(0);
        assertThat(loadedItem.getCompanyName()).isEqualTo("카카오");
        assertThat(loadedItem.getPosition()).isEqualTo("백엔드 개발자");
        assertThat(loadedItem.getEmploymentType()).isEqualTo(CareerEmploymentType.FULL_TIME);
        assertThat(loadedItem.getStartYearMonth()).isEqualTo("2022-03");
        assertThat(loadedItem.getEndYearMonth()).isNull();
        assertThat(loadedItem.getCurrentlyWorking()).isTrue();
        assertThat(loadedItem.getSummary()).isEqualTo("Spring Boot 기반 서비스 개발");
        assertThat(loadedItem.getTechStack()).containsExactly("Java", "Spring Boot", "MySQL");
    }

    @DisplayName("퇴직한 CareerItemPayload는 endYearMonth와 함께 저장/조회된다")
    @Test
    void saveAndLoad_formerCareerItem() {
        Member member = memberRepository.save(createMember());

        CareerItemPayload item = new CareerItemPayload();
        item.setCompanyName("네이버");
        item.setPosition("시스템 엔지니어");
        item.setEmploymentType(CareerEmploymentType.CONTRACT);
        item.setStartYearMonth("2020-01");
        item.setEndYearMonth("2021-12");
        item.setCurrentlyWorking(false);
        item.setSummary(null);
        item.setTechStack(List.of());

        CareerPayload career = new CareerPayload();
        career.getItems().add(item);

        Resume resume = Resume.create(member, "자기소개", (byte) 2, career, WorkType.SITE,
                ResumeWritingStatus.DONE, null);
        resumeRepository.saveAndFlush(resume);
        entityManager.clear();

        Resume found = resumeRepository.findById(resume.getId()).orElseThrow();
        CareerItemPayload loadedItem = found.getCareer().getItems().get(0);

        assertThat(loadedItem.getStartYearMonth()).isEqualTo("2020-01");
        assertThat(loadedItem.getEndYearMonth()).isEqualTo("2021-12");
        assertThat(loadedItem.getCurrentlyWorking()).isFalse();
        assertThat(loadedItem.getSummary()).isNull();
        assertThat(loadedItem.getTechStack()).isEmpty();
    }

    @DisplayName("복수의 CareerItemPayload가 순서대로 저장/조회된다")
    @Test
    void saveAndLoad_multipleCareerItems_orderPreserved() {
        Member member = memberRepository.save(createMember());

        CareerItemPayload item1 = new CareerItemPayload();
        item1.setCompanyName("A사");
        item1.setPosition("주니어 개발자");
        item1.setEmploymentType(CareerEmploymentType.FULL_TIME);
        item1.setStartYearMonth("2019-03");
        item1.setEndYearMonth("2021-02");
        item1.setCurrentlyWorking(false);
        item1.setTechStack(List.of("Python", "Django"));

        CareerItemPayload item2 = new CareerItemPayload();
        item2.setCompanyName("B사");
        item2.setPosition("시니어 개발자");
        item2.setEmploymentType(CareerEmploymentType.FULL_TIME);
        item2.setStartYearMonth("2021-03");
        item2.setCurrentlyWorking(true);
        item2.setSummary("마이크로서비스 설계 및 개발");
        item2.setTechStack(List.of("Java", "Kotlin", "Kubernetes"));

        CareerItemPayload item3 = new CareerItemPayload();
        item3.setCompanyName("C사");
        item3.setPosition("프리랜서");
        item3.setEmploymentType(CareerEmploymentType.FREELANCE);
        item3.setStartYearMonth("2018-06");
        item3.setEndYearMonth("2019-01");
        item3.setCurrentlyWorking(false);
        item3.setTechStack(List.of());

        CareerPayload career = new CareerPayload();
        career.getItems().addAll(List.of(item1, item2, item3));

        Resume resume = Resume.create(member, "자기소개", (byte) 7, career, WorkType.HYBRID,
                ResumeWritingStatus.WRITING, null);
        resumeRepository.saveAndFlush(resume);
        entityManager.clear();

        Resume found = resumeRepository.findById(resume.getId()).orElseThrow();
        List<CareerItemPayload> loadedItems = found.getCareer().getItems();

        assertThat(loadedItems).hasSize(3);
        assertThat(loadedItems.get(0).getCompanyName()).isEqualTo("A사");
        assertThat(loadedItems.get(0).getTechStack()).containsExactly("Python", "Django");
        assertThat(loadedItems.get(1).getCompanyName()).isEqualTo("B사");
        assertThat(loadedItems.get(1).getTechStack()).containsExactly("Java", "Kotlin", "Kubernetes");
        assertThat(loadedItems.get(2).getCompanyName()).isEqualTo("C사");
        assertThat(loadedItems.get(2).getTechStack()).isEmpty();
    }

    @DisplayName("Resume 업데이트 시 변경된 CareerPayload가 올바르게 반영된다")
    @Test
    void update_career_reflectsChanges() {
        Member member = memberRepository.save(createMember());

        CareerItemPayload original = new CareerItemPayload();
        original.setCompanyName("구버전 회사");
        original.setPosition("개발자");
        original.setEmploymentType(CareerEmploymentType.INTERN);
        original.setStartYearMonth("2023-01");
        original.setCurrentlyWorking(true);
        original.setTechStack(List.of("PHP"));

        CareerPayload originalCareer = new CareerPayload();
        originalCareer.getItems().add(original);

        Resume resume = Resume.create(member, "자기소개", (byte) 1, originalCareer, WorkType.REMOTE,
                ResumeWritingStatus.WRITING, null);
        resumeRepository.saveAndFlush(resume);
        entityManager.clear();

        Resume persisted = resumeRepository.findById(resume.getId()).orElseThrow();

        CareerItemPayload updated = new CareerItemPayload();
        updated.setCompanyName("신버전 회사");
        updated.setPosition("시니어 개발자");
        updated.setEmploymentType(CareerEmploymentType.FULL_TIME);
        updated.setStartYearMonth("2023-01");
        updated.setEndYearMonth("2024-12");
        updated.setCurrentlyWorking(false);
        updated.setTechStack(List.of("Java", "Spring"));

        CareerPayload updatedCareer = new CareerPayload();
        updatedCareer.getItems().add(updated);

        persisted.update("수정된 자기소개", (byte) 2, updatedCareer, WorkType.SITE,
                ResumeWritingStatus.DONE, null);
        resumeRepository.saveAndFlush(persisted);
        entityManager.clear();

        Resume result = resumeRepository.findById(resume.getId()).orElseThrow();
        CareerItemPayload loadedItem = result.getCareer().getItems().get(0);

        assertThat(loadedItem.getCompanyName()).isEqualTo("신버전 회사");
        assertThat(loadedItem.getEmploymentType()).isEqualTo(CareerEmploymentType.FULL_TIME);
        assertThat(loadedItem.getEndYearMonth()).isEqualTo("2024-12");
        assertThat(loadedItem.getCurrentlyWorking()).isFalse();
        assertThat(loadedItem.getTechStack()).containsExactly("Java", "Spring");
    }

    @DisplayName("같은 회원은 이력서를 하나만 가질 수 있다")
    @Test
    void failWhenSavingTwoResumesWithSameMember() {
        // given
        Member member = memberRepository.save(createMember());

        Resume firstResume = Resume.create(
                member,
                "첫 번째 이력서입니다.",
                (byte) 3,
                createCareerPayload(),
                WorkType.HYBRID,
                ResumeWritingStatus.WRITING,
                "https://github.com/user1"
        );

        Resume secondResume = Resume.create(
                member,
                "두 번째 이력서입니다.",
                (byte) 5,
                createCareerPayload(),
                WorkType.REMOTE,
                ResumeWritingStatus.DONE,
                "https://github.com/user2"
        );

        // when
        resumeRepository.saveAndFlush(firstResume);

        // then
        assertThatThrownBy(() -> resumeRepository.saveAndFlush(secondResume))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @DisplayName("회원 ID로 이력서를 조회할 수 있다")
    @Test
    void findByMemberId_success() {
        // given
        Member member = memberRepository.save(createMember());

        Resume resume = Resume.create(
                member,
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload(),
                WorkType.HYBRID,
                ResumeWritingStatus.WRITING,
                "https://github.com/user1"
        );

        Resume saved = resumeRepository.saveAndFlush(resume);
        entityManager.clear();

        // when
        Optional<Resume> result = resumeRepository.findByMemberId(member.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
        assertThat(result.get().getMember().getId()).isEqualTo(member.getId());
    }

    @DisplayName("존재하지 않는 회원 ID로 조회하면 결과가 없다")
    @Test
    void findByMemberId_returnsEmptyWhenNotFound() {
        // given
        Long nonExistingMemberId = 999999L;

        // when
        Optional<Resume> result = resumeRepository.findByMemberId(nonExistingMemberId);

        // then
        assertThat(result).isEmpty();
    }


    private static CareerPayload createCareerPayload() {
        CareerItemPayload item = new CareerItemPayload();
        item.setCompanyName("Generic4");
        item.setPosition("Backend Engineer");
        item.setEmploymentType(CareerEmploymentType.FULL_TIME);
        item.setStartYearMonth("2024-01");
        item.setEndYearMonth(null);
        item.setCurrentlyWorking(true);
        item.setSummary("Spring Boot 기반 API를 개발하고 운영했습니다.");
        item.setTechStack(List.of("Java", "Spring Boot", "PostgreSQL"));

        CareerPayload payload = new CareerPayload();
        payload.getItems().add(item);
        return payload;
    }
}
