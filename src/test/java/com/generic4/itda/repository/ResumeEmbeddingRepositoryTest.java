package com.generic4.itda.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.generic4.itda.annotation.RepositoryTest;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.recommendation.ResumeEmbedding;
import com.generic4.itda.domain.recommendation.vo.EmbeddingVector;
import com.generic4.itda.domain.recommendation.vo.SourceHash;
import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RepositoryTest
@DisplayName("ResumeEmbeddingRepository 테스트")
class ResumeEmbeddingRepositoryTest {

    @Autowired
    private ResumeEmbeddingRepository resumeEmbeddingRepository;

    @Autowired
    private EntityManager em;

    @Nested
    @DisplayName("findByResume_IdAndEmbeddingModel 파생 쿼리 검증")
    class FindByResumeIdAndEmbeddingModelTest {

        @Test
        @DisplayName("resumeId와 embeddingModel이 모두 일치하면 엔티티를 반환해야 한다")
        void should_return_entity_when_resumeId_and_model_match() {
            // given
            Resume resume = createResume("test@example.com");
            String model = "text-embedding-3-small";
            createResumeEmbedding(resume, model);

            em.flush();
            em.clear();

            // when
            Optional<ResumeEmbedding> found = resumeEmbeddingRepository.findByResume_IdAndEmbeddingModel(
                    resume.getId(), model);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getResume().getId()).isEqualTo(resume.getId());
            assertThat(found.get().getEmbeddingModel()).isEqualTo(model);
        }

        @Test
        @DisplayName("resumeId는 같지만 embeddingModel이 다르면 조회되지 않아야 한다")
        void should_return_empty_when_model_mismatch() {
            // given
            Resume resume = createResume("test@example.com");
            createResumeEmbedding(resume, "model-A");

            em.flush();
            em.clear();

            // when
            Optional<ResumeEmbedding> found = resumeEmbeddingRepository.findByResume_IdAndEmbeddingModel(
                    resume.getId(), "model-B");

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("embeddingModel은 같지만 resumeId가 다르면 조회되지 않아야 한다")
        void should_return_empty_when_resumeId_mismatch() {
            // given
            Resume resume1 = createResume("user1@example.com");
            Resume resume2 = createResume("user2@example.com");
            String model = "target-model";
            createResumeEmbedding(resume1, model);

            em.flush();
            em.clear();

            // when
            Optional<ResumeEmbedding> found = resumeEmbeddingRepository.findByResume_IdAndEmbeddingModel(
                    resume2.getId(), model);

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("조건에 맞는 데이터가 없으면 Optional.empty()를 반환해야 한다")
        void should_return_empty_when_no_matching_data() {
            // given
            Long nonExistentResumeId = 999L;
            String model = "any-model";

            // when
            Optional<ResumeEmbedding> found = resumeEmbeddingRepository.findByResume_IdAndEmbeddingModel(
                    nonExistentResumeId, model);

            // then
            assertThat(found).isEmpty();
        }
    }

    /**
     * 테스트용 Resume 생성 헬퍼 메서드
     */
    private Resume createResume(String email) {
        Member member = Member.create(
                email,
                "password123!",
                "테스트유저",
                "닉네임",
                "메모",
                "010-1234-5678"
        );
        em.persist(member);

        Resume resume = Resume.create(
                member,
                "안녕하세요, 테스트 자기소개입니다.",
                (byte) 3,
                new CareerPayload(),
                WorkType.REMOTE,
                ResumeWritingStatus.DONE,
                "https://portfolio.it-da.com"
        );
        em.persist(resume);
        return resume;
    }

    /**
     * 테스트용 ResumeEmbedding 생성 및 저장 헬퍼 메서드
     */
    private ResumeEmbedding createResumeEmbedding(Resume resume, String model) {
        ResumeEmbedding embedding = ResumeEmbedding.create(
                resume,
                new SourceHash("dummy-source-hash-value"),
                model,
                new EmbeddingVector(List.of(0.1, 0.2, 0.3, 0.4, 0.5))
        );
        return resumeEmbeddingRepository.save(embedding);
    }
}
