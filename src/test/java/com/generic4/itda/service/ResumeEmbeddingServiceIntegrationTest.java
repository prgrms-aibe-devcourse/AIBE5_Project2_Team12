package com.generic4.itda.service;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.generic4.itda.annotation.IntegrationTest;
import com.generic4.itda.config.ai.AiEmbeddingProperties;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.recommendation.ResumeEmbedding;
import com.generic4.itda.domain.recommendation.vo.SourceHash;
import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Proficiency;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.repository.ResumeEmbeddingRepository;
import com.generic4.itda.service.recommend.embedding.ResumeEmbeddingSourceHashGenerator;
import com.generic4.itda.service.recommend.embedding.ResumeEmbeddingTextGenerator;
import com.generic4.itda.service.recommend.scoring.QueryEmbeddingGenerator;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ResumeEmbeddingServiceIntegrationTest {

    @Autowired
    private ResumeEmbeddingService resumeEmbeddingService;

    @Autowired
    private ResumeEmbeddingRepository resumeEmbeddingRepository;

    @Autowired
    private ResumeEmbeddingTextGenerator resumeEmbeddingTextGenerator;

    @Autowired
    private ResumeEmbeddingSourceHashGenerator resumeEmbeddingSourceHashGenerator;

    @Autowired
    private AiEmbeddingProperties aiEmbeddingProperties;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private QueryEmbeddingGenerator queryEmbeddingGenerator;

    @Test
    @DisplayName("createOrRefresh를 호출하면 실제 텍스트/해시 생성기를 거쳐 ResumeEmbedding이 저장된다")
    void createOrRefresh_persists_resume_embedding_with_real_generators_and_mocked_embedding_api() {
        // given
        Member member = persist(createMember("resume-embedding-owner@test.com", "pw", "이력서소유자", "010-1234-5678"));
        Skill java = persist(Skill.create("Java", null));

        Resume resume = Resume.create(
                member,
                "  백엔드   개발 경험이 있습니다.  ",
                (byte) 5,
                new CareerPayload(),
                WorkType.REMOTE,
                ResumeWritingStatus.DONE,
                "https://portfolio.example.com"
        );
        resume.addSkill(java, Proficiency.ADVANCED);
        persist(resume);

        String expectedEmbeddingText = resumeEmbeddingTextGenerator.generate(resume);
        SourceHash expectedSourceHash = resumeEmbeddingSourceHashGenerator.generate(expectedEmbeddingText);
        String expectedModel = aiEmbeddingProperties.resolveEmbeddingModel();
        List<Double> embeddingVector = List.of(0.12, 0.34, 0.56);

        given(queryEmbeddingGenerator.generate(expectedEmbeddingText)).willReturn(embeddingVector);

        entityManager.flush();
        entityManager.clear();

        Resume storedResume = entityManager.find(Resume.class, resume.getId());

        // when
        ResumeEmbedding result = resumeEmbeddingService.createOrRefresh(storedResume);

        entityManager.flush();
        entityManager.clear();

        // then
        ResumeEmbedding persisted = resumeEmbeddingRepository
                .findByResume_IdAndEmbeddingModel(resume.getId(), expectedModel)
                .orElseThrow();

        assertThat(result.getId()).isNotNull();
        assertThat(persisted.getId()).isEqualTo(result.getId());
        assertThat(persisted.getResume().getId()).isEqualTo(resume.getId());
        assertThat(persisted.getEmbeddingModel()).isEqualTo(expectedModel);
        assertThat(persisted.getSourceHash()).isEqualTo(expectedSourceHash);
        assertThat(persisted.getEmbeddingVector().values()).isEqualTo(embeddingVector);
        then(queryEmbeddingGenerator).should().generate(expectedEmbeddingText);
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }
}
