package com.generic4.itda.service.recommend.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.generic4.itda.domain.recommendation.ResumeEmbedding;
import com.generic4.itda.domain.recommendation.vo.EmbeddingVector;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.repository.ResumeEmbeddingRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeEmbeddingReaderImplTest {

    @Mock
    private ResumeEmbeddingRepository resumeEmbeddingRepository;

    private ResumeEmbeddingReaderImpl resumeEmbeddingReader;

    @BeforeEach
    void setUp() {
        resumeEmbeddingReader = new ResumeEmbeddingReaderImpl(resumeEmbeddingRepository);
    }

    @Test
    @DisplayName("이력서 ID 목록으로 임베딩을 조회하여 맵 형태로 반환한다")
    void 이력서_ID_목록으로_임베딩을_조회하여_맵_형태로_반환한다() {
        // given
        List<Long> resumeIds = List.of(1L, 2L);
        String model = "text-embedding-3-small";

        Resume resume1 = mock(Resume.class);
        given(resume1.getId()).willReturn(1L);
        Resume resume2 = mock(Resume.class);
        given(resume2.getId()).willReturn(2L);

        ResumeEmbedding embedding1 = mock(ResumeEmbedding.class);
        given(embedding1.getResume()).willReturn(resume1);
        given(embedding1.getEmbeddingVector()).willReturn(new EmbeddingVector(List.of(0.1, 0.2)));

        ResumeEmbedding embedding2 = mock(ResumeEmbedding.class);
        given(embedding2.getResume()).willReturn(resume2);
        given(embedding2.getEmbeddingVector()).willReturn(new EmbeddingVector(List.of(0.3, 0.4)));

        given(resumeEmbeddingRepository.findAllByResume_IdInAndEmbeddingModel(resumeIds, model))
                .willReturn(List.of(embedding1, embedding2));

        // when
        Map<Long, List<Double>> result = resumeEmbeddingReader.readEmbeddingsByResumeIds(resumeIds, model);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(1L)).containsExactly(0.1, 0.2);
        assertThat(result.get(2L)).containsExactly(0.3, 0.4);
    }

    @Test
    @DisplayName("이력서 ID 목록이 비어있으면 빈 맵을 반환한다")
    void 이력서_ID_목록이_비어있으면_빈_맵을_반환한다() {
        // when
        Map<Long, List<Double>> result = resumeEmbeddingReader.readEmbeddingsByResumeIds(Collections.emptyList(),
                "model");

        // then
        assertThat(result).isEmpty();
        then(resumeEmbeddingRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이력서 ID 목록이 null이면 빈 맵을 반환한다")
    void 이력서_ID_목록이_null이면_빈_맵을_반환한다() {
        // when
        Map<Long, List<Double>> result = resumeEmbeddingReader.readEmbeddingsByResumeIds(null, "model");

        // then
        assertThat(result).isEmpty();
        then(resumeEmbeddingRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("모델명이 없으면 예외가 발생한다")
    void 모델명이_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> resumeEmbeddingReader.readEmbeddingsByResumeIds(List.of(1L), null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> resumeEmbeddingReader.readEmbeddingsByResumeIds(List.of(1L), ""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> resumeEmbeddingReader.readEmbeddingsByResumeIds(List.of(1L), "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("조회되지 않은 이력서 ID는 결과 맵에 포함하지 않는다")
    void 조회되지_않은_이력서_ID는_결과_맵에_포함하지_않는다() {
        // given
        List<Long> resumeIds = List.of(1L, 2L);
        String model = "text-embedding-3-small";

        Resume resume1 = mock(Resume.class);
        given(resume1.getId()).willReturn(1L);

        ResumeEmbedding embedding1 = mock(ResumeEmbedding.class);
        given(embedding1.getResume()).willReturn(resume1);
        given(embedding1.getEmbeddingVector()).willReturn(new EmbeddingVector(List.of(0.1, 0.2)));

        given(resumeEmbeddingRepository.findAllByResume_IdInAndEmbeddingModel(resumeIds, model))
                .willReturn(List.of(embedding1));

        // when
        Map<Long, List<Double>> result = resumeEmbeddingReader.readEmbeddingsByResumeIds(resumeIds, model);

        // then
        assertThat(result).hasSize(1);
        assertThat(result).containsKey(1L);
        assertThat(result).doesNotContainKey(2L);
    }
}
