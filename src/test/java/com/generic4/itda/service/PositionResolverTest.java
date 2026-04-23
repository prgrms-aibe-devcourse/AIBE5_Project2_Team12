package com.generic4.itda.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.repository.PositionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PositionResolverTest {

    private static final java.util.List<String> CANONICAL_POSITIONS = java.util.List.of(
            "백엔드 개발자",
            "프론트엔드 개발자",
            "풀스택 개발자",
            "모바일 앱 개발자",
            "AI 엔지니어",
            "데이터 엔지니어",
            "DevOps 엔지니어",
            "UI/UX 디자이너",
            "서비스 기획자",
            "QA 엔지니어"
    );

    @Mock
    private PositionRepository positionRepository;

    @InjectMocks
    private PositionResolver positionResolver;

    @Test
    @DisplayName("기존 Position 이름과 정확히 일치하면 그대로 매핑한다")
    void resolve_returnsExactMatchedPosition() {
        Position backend = Position.create("백엔드 개발자");
        org.mockito.BDDMockito.given(positionRepository.findByName("백엔드 개발자"))
                .willReturn(java.util.Optional.of(backend));

        assertThat(positionResolver.resolve("백엔드 개발자")).contains(backend);
    }

    @Test
    @DisplayName("직무 접미사만 다른 AI 카테고리도 기존 Position으로 매핑한다")
    void resolve_mapsAliasByNormalizedSuffix() {
        Position aiEngineer = Position.create("AI 엔지니어");
        org.mockito.BDDMockito.given(positionRepository.findByName("AI 엔지니어"))
                .willReturn(java.util.Optional.of(aiEngineer));

        assertThat(positionResolver.resolve("AI 개발자")).contains(aiEngineer);
    }

    @Test
    @DisplayName("영문과 대소문자가 섞여도 정규 Position 카테고리로 매핑한다")
    void resolve_mapsEnglishAliasIgnoringCase() {
        Position mobileDeveloper = Position.create("모바일 앱 개발자");
        org.mockito.BDDMockito.given(positionRepository.findByName("모바일 앱 개발자"))
                .willReturn(java.util.Optional.of(mobileDeveloper));

        assertThat(positionResolver.resolve("ReAcT NaTiVe Developer")).contains(mobileDeveloper);
        assertThat(positionResolver.resolveCanonicalName("ReAcT NaTiVe Developer"))
                .contains("모바일 앱 개발자");
    }

    @Test
    @DisplayName("기존 PM 계열 표현은 서비스 기획자 카테고리로 정규화한다")
    void resolve_mapsPmAliasToServicePlanner() {
        assertThat(positionResolver.resolveCanonicalName("Product Manager"))
                .contains("서비스 기획자");
        assertThat(positionResolver.resolveCanonicalName("pm"))
                .contains("서비스 기획자");
    }

    @Test
    @DisplayName("허용 Position 목록에 없는 카테고리는 매핑하지 않는다")
    void resolve_returnsEmptyWhenCategoryIsNotAllowed() {
        assertThat(positionResolver.resolve("데이터 분석가")).isEmpty();
        assertThat(positionResolver.resolveCanonicalName("데이터 분석가")).isEmpty();
    }

    @Test
    @DisplayName("허용 Position 목록은 공식 열 개 카테고리만 반환한다")
    void findAllowedCategoryNames_returnsCanonicalCategories() {
        assertThat(positionResolver.findAllowedCategoryNames())
                .containsExactlyElementsOf(CANONICAL_POSITIONS);
    }
}
