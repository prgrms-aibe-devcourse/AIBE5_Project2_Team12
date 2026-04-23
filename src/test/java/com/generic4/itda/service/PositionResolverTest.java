package com.generic4.itda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.generic4.itda.domain.position.Position;
import com.generic4.itda.repository.PositionRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class PositionResolverTest {

    @Mock
    private PositionRepository positionRepository;

    @InjectMocks
    private PositionResolver positionResolver;

    @Test
    @DisplayName("기존 Position 이름과 정확히 일치하면 그대로 매핑한다")
    void resolve_returnsExactMatchedPosition() {
        Position backend = Position.create("백엔드 개발자");
        given(positionRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))).willReturn(List.of(backend));

        assertThat(positionResolver.resolve("백엔드 개발자")).contains(backend);
    }

    @Test
    @DisplayName("직무 접미사만 다른 AI 카테고리도 기존 Position으로 매핑한다")
    void resolve_mapsAliasByNormalizedSuffix() {
        Position aiEngineer = Position.create("AI 엔지니어");
        given(positionRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))).willReturn(List.of(aiEngineer));

        assertThat(positionResolver.resolve("AI 개발자")).contains(aiEngineer);
    }

    @Test
    @DisplayName("동일 별칭으로 여러 Position이 겹치면 안전하게 매핑하지 않는다")
    void resolve_returnsEmptyWhenAliasIsAmbiguous() {
        Position aiEngineer = Position.create("AI 엔지니어");
        Position aiPlanner = Position.create("AI 기획자");
        given(positionRepository.findAll(Sort.by(Sort.Direction.ASC, "name")))
                .willReturn(List.of(aiEngineer, aiPlanner));

        assertThat(positionResolver.resolve("AI 개발자")).isEmpty();
    }
}
