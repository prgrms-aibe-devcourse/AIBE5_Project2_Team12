package com.generic4.itda.repository;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;

import com.generic4.itda.annotation.H2RepositoryTest;
import com.generic4.itda.domain.file.StoredFile;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Proficiency;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeSkill;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import com.generic4.itda.domain.skill.Skill;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@H2RepositoryTest
class ResumeAssociationTest {

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StoredFileRepository storedFileRepository;

    @Autowired
    private EntityManager em;

    private Resume savedResume;

    @BeforeEach
    void setUp() {
        Member member = memberRepository.save(createMember());
        Resume resume = Resume.create(member, "자기소개", (byte) 3, new CareerPayload(),
                WorkType.REMOTE, ResumeWritingStatus.WRITING, null);
        savedResume = resumeRepository.saveAndFlush(resume);
        em.clear();
    }

    // ═══════════════════════════════════════════════════════════
    // 스킬 정렬 — getSortedSkills()
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("스킬 정렬 — getSortedSkills()")
    class SkillSortingTest {

        /**
         * skills 컬렉션에 @OrderBy가 없으므로 DB 조회 순서에 의존하지 않습니다.
         * getSortedSkills()는 Proficiency.priority 기준 내림차순으로 정렬합니다:
         * ADVANCED(3) > INTERMEDIATE(2) > BEGINNER(1)
         */
        @DisplayName("BEGINNER·INTERMEDIATE·ADVANCED 순으로 삽입해도 getSortedSkills()는 priority 내림차순으로 반환한다")
        @Test
        void getSortedSkills_orderedByPriorityDesc() {
            Resume resume = resumeRepository.findById(savedResume.getId()).orElseThrow();

            Skill java = Skill.create("Java", null);
            Skill python = Skill.create("Python", null);
            Skill go = Skill.create("Go", null);
            em.persist(java);
            em.persist(python);
            em.persist(go);

            // 의도적으로 낮은 숙련도부터 삽입
            resume.addSkill(java, Proficiency.BEGINNER);
            resume.addSkill(python, Proficiency.INTERMEDIATE);
            resume.addSkill(go, Proficiency.ADVANCED);
            resumeRepository.saveAndFlush(resume);
            em.clear();

            Resume found = resumeRepository.findById(resume.getId()).orElseThrow();
            List<ResumeSkill> sorted = found.getSortedSkills();

            assertThat(sorted)
                    .extracting(ResumeSkill::getProficiency)
                    .containsExactly(Proficiency.ADVANCED, Proficiency.INTERMEDIATE, Proficiency.BEGINNER);
        }

        @DisplayName("동일 숙련도 스킬이 포함되어도 getSortedSkills()는 올바른 priority 내림차순을 유지한다")
        @Test
        void getSortedSkills_withDuplicatePriority() {
            Resume resume = resumeRepository.findById(savedResume.getId()).orElseThrow();

            Skill java = Skill.create("Java", null);
            Skill kotlin = Skill.create("Kotlin", null);
            Skill python = Skill.create("Python", null);
            em.persist(java);
            em.persist(kotlin);
            em.persist(python);

            resume.addSkill(java, Proficiency.BEGINNER);
            resume.addSkill(kotlin, Proficiency.ADVANCED);
            resume.addSkill(python, Proficiency.ADVANCED);
            resumeRepository.saveAndFlush(resume);
            em.clear();

            Resume found = resumeRepository.findById(resume.getId()).orElseThrow();
            List<ResumeSkill> sorted = found.getSortedSkills();

            assertThat(sorted).hasSize(3);
            assertThat(sorted.subList(0, 2))
                    .extracting(ResumeSkill::getProficiency)
                    .containsOnly(Proficiency.ADVANCED);
            assertThat(sorted.get(2).getProficiency()).isEqualTo(Proficiency.BEGINNER);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 첨부파일 정렬 — @OrderBy("createdAt asc")
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("첨부파일 정렬 — @OrderBy(\"createdAt asc\")")
    class AttachmentSortingTest {

        /**
         * attachments는 @OrderBy("createdAt asc")로 정렬됩니다.
         * 파일별로 saveAndFlush 후 Thread.sleep을 사용해 createdAt 차이를 보장합니다.
         */
        @DisplayName("가장 먼저 생성된 파일이 attachments 리스트의 첫 번째로 조회된다")
        @Test
        void orderedByCreatedAtAsc() throws InterruptedException {
            Resume resume = resumeRepository.findById(savedResume.getId()).orElseThrow();

            StoredFile file0 = storedFileRepository.save(toStoredFile("report.pdf", "s-0.pdf"));
            resume.addFile(file0);
            resumeRepository.saveAndFlush(resume);

            Thread.sleep(20);

            StoredFile file1 = storedFileRepository.save(toStoredFile("portfolio.pdf", "s-1.pdf"));
            resume.addFile(file1);
            resumeRepository.saveAndFlush(resume);

            Thread.sleep(20);

            StoredFile file2 = storedFileRepository.save(toStoredFile("cover.pdf", "s-2.pdf"));
            resume.addFile(file2);
            resumeRepository.saveAndFlush(resume);

            em.clear();

            Resume found = resumeRepository.findById(resume.getId()).orElseThrow();

            assertThat(found.getAttachments()).hasSize(3);
            // 생성 시간 오름차순: file0(earliest) → file1 → file2(latest)
            assertThat(found.getAttachments())
                    .extracting(a -> a.getFile().getId())
                    .containsExactly(file0.getId(), file1.getId(), file2.getId());
        }

        @DisplayName("나중에 생성된 파일은 attachments 리스트의 뒤에 위치한다")
        @Test
        void lateAddedFile_appearsLast() throws InterruptedException {
            Resume resume = resumeRepository.findById(savedResume.getId()).orElseThrow();

            StoredFile first = storedFileRepository.save(toStoredFile("first.pdf", "sf.pdf"));
            resume.addFile(first);
            resumeRepository.saveAndFlush(resume);

            Thread.sleep(20);

            StoredFile last = storedFileRepository.save(toStoredFile("last.pdf", "sl.pdf"));
            resume.addFile(last);
            resumeRepository.saveAndFlush(resume);

            em.clear();

            Resume found = resumeRepository.findById(resume.getId()).orElseThrow();

            assertThat(found.getAttachments()).hasSize(2);
            assertThat(found.getAttachments().get(0).getFile().getId()).isEqualTo(first.getId());
            assertThat(found.getAttachments().get(1).getFile().getId()).isEqualTo(last.getId());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // orphanRemoval — 스킬
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("orphanRemoval - 스킬")
    class SkillOrphanRemovalTest {

        @DisplayName("removeSkill 후 flush하면 DB에서 해당 ResumeSkill 레코드가 삭제된다")
        @Test
        void removeSkill_deletesOrphanRow() {
            Resume resume = resumeRepository.findById(savedResume.getId()).orElseThrow();

            Skill java = Skill.create("Java", null);
            Skill python = Skill.create("Python", null);
            em.persist(java);
            em.persist(python);

            resume.addSkill(java, Proficiency.ADVANCED);
            resume.addSkill(python, Proficiency.INTERMEDIATE);
            resumeRepository.saveAndFlush(resume);
            em.clear();

            Resume persisted = resumeRepository.findById(resume.getId()).orElseThrow();
            Skill managedJava = em.find(Skill.class, java.getId());
            persisted.removeSkill(managedJava);
            resumeRepository.saveAndFlush(persisted);
            em.clear();

            Resume afterRemoval = resumeRepository.findById(resume.getId()).orElseThrow();
            assertThat(afterRemoval.getSkills()).hasSize(1);
            assertThat(afterRemoval.getSkills().get(0).getSkill().getName()).isEqualTo("Python");

            Long count = em.createQuery(
                            "SELECT COUNT(rs) FROM ResumeSkill rs WHERE rs.resume.id = :id", Long.class)
                    .setParameter("id", resume.getId())
                    .getSingleResult();
            assertThat(count).isEqualTo(1L);
        }

        @DisplayName("모든 스킬 제거 후 DB에 ResumeSkill 레코드가 하나도 없다")
        @Test
        void removeAllSkills_noOrphanRowRemains() {
            Resume resume = resumeRepository.findById(savedResume.getId()).orElseThrow();

            Skill java = Skill.create("Java", null);
            em.persist(java);
            resume.addSkill(java, Proficiency.BEGINNER);
            resumeRepository.saveAndFlush(resume);
            em.clear();

            Resume persisted = resumeRepository.findById(resume.getId()).orElseThrow();
            Skill managedJava = em.find(Skill.class, java.getId());
            persisted.removeSkill(managedJava);
            resumeRepository.saveAndFlush(persisted);
            em.clear();

            Long count = em.createQuery(
                            "SELECT COUNT(rs) FROM ResumeSkill rs WHERE rs.resume.id = :id", Long.class)
                    .setParameter("id", resume.getId())
                    .getSingleResult();
            assertThat(count).isZero();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // orphanRemoval — 첨부파일
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("orphanRemoval - 첨부파일")
    class AttachmentOrphanRemovalTest {

        @DisplayName("중간 파일 제거 후 flush하면 DB에서 해당 ResumeAttachment 레코드가 삭제된다")
        @Test
        void removeMiddleFile_deletesOrphanRow() throws InterruptedException {
            Resume resume = resumeRepository.findById(savedResume.getId()).orElseThrow();

            StoredFile file0 = storedFileRepository.save(toStoredFile("a.pdf", "sa.pdf"));
            resume.addFile(file0);
            resumeRepository.saveAndFlush(resume);

            Thread.sleep(20);

            StoredFile file1 = storedFileRepository.save(toStoredFile("b.pdf", "sb.pdf"));
            resume.addFile(file1);
            resumeRepository.saveAndFlush(resume);

            Thread.sleep(20);

            StoredFile file2 = storedFileRepository.save(toStoredFile("c.pdf", "sc.pdf"));
            resume.addFile(file2);
            resumeRepository.saveAndFlush(resume);

            em.clear();

            // 중간 파일(file1) 제거
            Resume persisted = resumeRepository.findById(resume.getId()).orElseThrow();
            StoredFile managedFile1 = em.find(StoredFile.class, file1.getId());
            persisted.removeFile(managedFile1);
            resumeRepository.saveAndFlush(persisted);
            em.clear();

            Resume afterRemoval = resumeRepository.findById(resume.getId()).orElseThrow();
            assertThat(afterRemoval.getAttachments()).hasSize(2);

            Long count = em.createQuery(
                            "SELECT COUNT(ra) FROM ResumeAttachment ra WHERE ra.resume.id = :id", Long.class)
                    .setParameter("id", resume.getId())
                    .getSingleResult();
            assertThat(count).isEqualTo(2L);

            // @OrderBy("createdAt asc"): file0(earliest) → file2
            assertThat(afterRemoval.getAttachments())
                    .extracting(a -> a.getFile().getId())
                    .containsExactly(file0.getId(), file2.getId());
        }

        @DisplayName("첫 번째 파일 제거 후 flush하면 DB에서 해당 ResumeAttachment 레코드가 삭제된다")
        @Test
        void removeFirstFile_deletesOrphanRow() throws InterruptedException {
            Resume resume = resumeRepository.findById(savedResume.getId()).orElseThrow();

            StoredFile file0 = storedFileRepository.save(toStoredFile("x.pdf", "sx.pdf"));
            resume.addFile(file0);
            resumeRepository.saveAndFlush(resume);

            Thread.sleep(20);

            StoredFile file1 = storedFileRepository.save(toStoredFile("y.pdf", "sy.pdf"));
            resume.addFile(file1);
            resumeRepository.saveAndFlush(resume);

            Thread.sleep(20);

            StoredFile file2 = storedFileRepository.save(toStoredFile("z.pdf", "sz.pdf"));
            resume.addFile(file2);
            resumeRepository.saveAndFlush(resume);

            em.clear();

            // 첫 번째 파일(file0) 제거
            Resume persisted = resumeRepository.findById(resume.getId()).orElseThrow();
            StoredFile managedFile0 = em.find(StoredFile.class, file0.getId());
            persisted.removeFile(managedFile0);
            resumeRepository.saveAndFlush(persisted);
            em.clear();

            Resume afterRemoval = resumeRepository.findById(resume.getId()).orElseThrow();
            assertThat(afterRemoval.getAttachments()).hasSize(2);

            Long count = em.createQuery(
                            "SELECT COUNT(ra) FROM ResumeAttachment ra WHERE ra.resume.id = :id", Long.class)
                    .setParameter("id", resume.getId())
                    .getSingleResult();
            assertThat(count).isEqualTo(2L);

            // @OrderBy("createdAt asc"): file1 → file2
            assertThat(afterRemoval.getAttachments())
                    .extracting(a -> a.getFile().getId())
                    .containsExactly(file1.getId(), file2.getId());
        }

        @DisplayName("모든 파일 제거 후 DB에 ResumeAttachment 레코드가 하나도 없다")
        @Test
        void removeAllFiles_noOrphanRowRemains() throws InterruptedException {
            Resume resume = resumeRepository.findById(savedResume.getId()).orElseThrow();

            StoredFile file0 = storedFileRepository.save(toStoredFile("p.pdf", "sp.pdf"));
            resume.addFile(file0);
            resumeRepository.saveAndFlush(resume);

            Thread.sleep(20);

            StoredFile file1 = storedFileRepository.save(toStoredFile("q.pdf", "sq.pdf"));
            resume.addFile(file1);
            resumeRepository.saveAndFlush(resume);

            em.clear();

            Resume persisted = resumeRepository.findById(resume.getId()).orElseThrow();
            StoredFile mf0 = em.find(StoredFile.class, file0.getId());
            StoredFile mf1 = em.find(StoredFile.class, file1.getId());
            persisted.removeFile(mf0);
            persisted.removeFile(mf1);
            resumeRepository.saveAndFlush(persisted);
            em.clear();

            Long count = em.createQuery(
                            "SELECT COUNT(ra) FROM ResumeAttachment ra WHERE ra.resume.id = :id", Long.class)
                    .setParameter("id", resume.getId())
                    .getSingleResult();
            assertThat(count).isZero();
        }
    }

    private StoredFile toStoredFile(String originalName, String storedName) {
        return StoredFile.create(originalName, storedName, "/files/" + storedName, "application/pdf", 1024L);
    }
}
