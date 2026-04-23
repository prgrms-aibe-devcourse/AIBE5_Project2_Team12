package com.generic4.itda.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.generic4.itda.domain.position.Position;
import com.generic4.itda.service.PositionResolver;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PositionSearchControllerTest {

    @Mock
    private PositionResolver positionResolver;

    @InjectMocks
    private PositionSearchController positionSearchController;

    @Test
    @DisplayName("직무 검색 결과를 id와 name 응답으로 반환한다")
    void search_returnsPositionSearchResponses() {
        Position mobile = Position.create("모바일 앱 개발자");
        ReflectionTestUtils.setField(mobile, "id", 4L);
        given(positionResolver.search("react native")).willReturn(List.of(mobile));

        List<PositionSearchController.PositionSearchResponse> responses = positionSearchController.search("react native");

        assertThat(responses)
                .extracting(PositionSearchController.PositionSearchResponse::id, PositionSearchController.PositionSearchResponse::name)
                .containsExactly(tuple(4L, "모바일 앱 개발자"));
        then(positionResolver).should().search("react native");
    }

    @Test
    @DisplayName("검색어가 없어도 PositionResolver에 그대로 위임한다")
    void search_delegatesNullQueryToPositionResolver() {
        Position backend = Position.create("백엔드 개발자");
        ReflectionTestUtils.setField(backend, "id", 1L);
        given(positionResolver.search(null)).willReturn(List.of(backend));

        List<PositionSearchController.PositionSearchResponse> responses = positionSearchController.search(null);

        assertThat(responses)
                .extracting(PositionSearchController.PositionSearchResponse::id, PositionSearchController.PositionSearchResponse::name)
                .containsExactly(tuple(1L, "백엔드 개발자"));
        then(positionResolver).should().search(null);
    }
}
