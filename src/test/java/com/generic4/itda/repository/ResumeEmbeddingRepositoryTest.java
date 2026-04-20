package com.generic4.itda.repository;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@RepositoryTest
class ResumeEmbeddingRepositoryTest {

    @Autowired
    private ResumeEmbeddingRepository resumeEmbeddingRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private EntityManager em;

    private Resume resume;

    @BeforeEach
    void setUp() {
        Member member = memberRepository.save(createMember());
        resume = resumeRepository.save(
                Resume.create(member, "자기소개입니다.", (byte) 3, new CareerPayload(),
                        WorkType.REMOTE, ResumeWritingStatus.WRITING, null));
    }

    @Test
    @DisplayName("동일한 resume와 embeddingModel 조합은 중복 저장할 수 없다")
    void 동일한_resume와_embeddingModel_조합은_중복_저장할_수_없다() {
        // given - 같은 resume + 같은 model, 다른 hash/vector
        String model = "text-embedding-3-small";
        ResumeEmbedding first = ResumeEmbedding.create(
                resume, new SourceHash("hash001"), model, new EmbeddingVector(List.of(0.1, 0.2)));
        ResumeEmbedding second = ResumeEmbedding.create(
                resume, new SourceHash("hash002"), model, new EmbeddingVector(List.of(0.3, 0.4)));

        // when
        resumeEmbeddingRepository.saveAndFlush(first);

        // then
        assertThatThrownBy(() -> resumeEmbeddingRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("refresh() 후 SourceHash와 EmbeddingVector가 DB round-trip 된다")
    void refresh_후_SourceHash와_EmbeddingVector가_DB_round_trip된다() {
        // given
        ResumeEmbedding embedding = resumeEmbeddingRepository.saveAndFlush(
                ResumeEmbedding.create(resume, new SourceHash("old-hash"), "text-embedding-3-small",
                        new EmbeddingVector(List.of(0.1, 0.2))));

        SourceHash newHash = new SourceHash("new-hash");
        EmbeddingVector newVector = new EmbeddingVector(List.of(0.9, 0.8, 0.7));

        // when
        embedding.refresh(newHash, newVector);
        resumeEmbeddingRepository.saveAndFlush(embedding);
        em.clear();

        // then
        ResumeEmbedding found = resumeEmbeddingRepository.findById(embedding.getId()).orElseThrow();
        assertThat(found.getSourceHash()).isEqualTo(newHash);
        assertThat(found.getEmbeddingVector()).isEqualTo(newVector);
        assertThat(found.getEmbeddingModel()).isEqualTo("text-embedding-3-small");
    }

    @Test
    @DisplayName("이력서 ID 목록과 모델명으로 임베딩을 조회한다")
    void 이력서_ID_목록과_모델명으로_임베딩을_조회한다() {
        // given
        String model = "text-embedding-3-small";
        resumeEmbeddingRepository.saveAndFlush(
                ResumeEmbedding.create(resume, new SourceHash("hash1"), model, new EmbeddingVector(List.of(0.1, 0.2))));
        
        Member otherMember = memberRepository.save(createMember("other@test.com", "pass", "other", "010-1111-2222"));
        Resume otherResume = resumeRepository.save(
                Resume.create(otherMember, "자기소개입니다.", (byte) 5, new CareerPayload(),
                        WorkType.SITE, ResumeWritingStatus.WRITING, null));
        resumeEmbeddingRepository.saveAndFlush(
                ResumeEmbedding.create(otherResume, new SourceHash("hash2"), model, new EmbeddingVector(List.of(0.3, 0.4))));
        
        resumeEmbeddingRepository.saveAndFlush(
                ResumeEmbedding.create(resume, new SourceHash("hash3"), "other-model", new EmbeddingVector(List.of(0.5, 0.6))));

        // when
        List<ResumeEmbedding> results = resumeEmbeddingRepository.findAllByResume_IdInAndEmbeddingModel(
                List.of(resume.getId(), otherResume.getId()), model);

        // then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(ResumeEmbedding::getEmbeddingModel)
                .containsOnly(model);
        assertThat(results).extracting(re -> re.getResume().getId())
                .containsExactlyInAnyOrder(resume.getId(), otherResume.getId());
    }
}
