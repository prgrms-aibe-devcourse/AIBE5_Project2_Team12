package com.generic4.itda.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.service.SkillResolver;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SkillSearchControllerTest {

    @Mock
    private SkillResolver skillResolver;

    @InjectMocks
    private SkillSearchController skillSearchController;

    @Test
    @DisplayName("스킬 검색 결과를 id와 name 응답으로 반환한다")
    void search_returnsSkillSearchResponses() {
        Skill react = Skill.create("React", null);
        ReflectionTestUtils.setField(react, "id", 1L);
        given(skillResolver.search("리액트")).willReturn(List.of(react));

        List<SkillSearchController.SkillSearchResponse> responses = skillSearchController.search("리액트");

        assertThat(responses)
                .extracting(SkillSearchController.SkillSearchResponse::id, SkillSearchController.SkillSearchResponse::name)
                .containsExactly(tuple(1L, "React"));
        then(skillResolver).should().search("리액트");
    }

    @Test
    @DisplayName("검색어가 없어도 SkillResolver에 그대로 위임한다")
    void search_delegatesNullQueryToSkillResolver() {
        Skill react = Skill.create("React", null);
        ReflectionTestUtils.setField(react, "id", 1L);
        given(skillResolver.search(null)).willReturn(List.of(react));

        List<SkillSearchController.SkillSearchResponse> responses = skillSearchController.search(null);

        assertThat(responses)
                .extracting(SkillSearchController.SkillSearchResponse::id, SkillSearchController.SkillSearchResponse::name)
                .containsExactly(tuple(1L, "React"));
        then(skillResolver).should().search(null);
    }
}