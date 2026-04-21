package com.generic4.itda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.exception.UnresolvedSkillException;
import com.generic4.itda.repository.SkillRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SkillResolverTest {

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private SkillResolver skillResolver;

    @DisplayName("스킬 문자열을 정규 Skill 이름으로 매핑한다")
    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @MethodSource("aliasSource")
    void resolve_mapsAliasToCanonicalSkillName(String input, String canonicalName) {
        Skill skill = Skill.create(canonicalName, null);
        given(skillRepository.findByName(canonicalName)).willReturn(Optional.of(skill));

        Optional<Skill> resolvedSkill = skillResolver.resolve(input);

        assertThat(resolvedSkill).contains(skill);
        then(skillRepository).should().findByName(canonicalName);
    }

    @Test
    @DisplayName("앞뒤 공백이 있는 스킬 문자열도 정규 Skill 이름으로 매핑한다")
    void resolve_trimsInputBeforeMapping() {
        Skill skill = Skill.create("React", null);
        given(skillRepository.findByName("React")).willReturn(Optional.of(skill));

        Optional<Skill> resolvedSkill = skillResolver.resolve("  React.js  ");

        assertThat(resolvedSkill).contains(skill);
        then(skillRepository).should().findByName("React");
    }

    @Test
    @DisplayName("매핑된 정규 Skill이 저장소에 없으면 Optional.empty를 반환한다")
    void resolve_returnsEmptyWhenCanonicalSkillDoesNotExist() {
        given(skillRepository.findByName("React")).willReturn(Optional.empty());

        Optional<Skill> resolvedSkill = skillResolver.resolve("react");

        assertThat(resolvedSkill).isEmpty();
        then(skillRepository).should().findByName("React");
    }

    @Test
    @DisplayName("별칭 목록에 없는 입력은 trim한 원문으로 조회한다")
    void resolve_fallsBackToTrimmedInputWhenAliasDoesNotExist() {
        Skill skill = Skill.create("Unknown Skill", null);
        given(skillRepository.findByName("Unknown Skill")).willReturn(Optional.of(skill));

        Optional<Skill> resolvedSkill = skillResolver.resolve("  Unknown Skill  ");

        assertThat(resolvedSkill).contains(skill);
        then(skillRepository).should().findByName("Unknown Skill");
    }

    @DisplayName("입력값이 비어 있으면 Optional.empty를 반환하고 저장소를 조회하지 않는다")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("blankInputSource")
    void resolve_returnsEmptyWhenInputIsBlank(String input) {
        Optional<Skill> resolvedSkill = skillResolver.resolve(input);

        assertThat(resolvedSkill).isEmpty();
        then(skillRepository).should(never()).findByName(anyString());
    }

    @Test
    @DisplayName("resolveRequired는 매핑에 성공한 Skill을 반환한다")
    void resolveRequired_returnsResolvedSkill() {
        Skill skill = Skill.create("Spring Boot", null);
        given(skillRepository.findByName("Spring Boot")).willReturn(Optional.of(skill));

        Skill resolvedSkill = skillResolver.resolveRequired("스프링부트");

        assertThat(resolvedSkill).isSameAs(skill);
        then(skillRepository).should().findByName("Spring Boot");
    }

    @Test
    @DisplayName("resolveRequired는 매핑에 실패하면 예외를 던진다")
    void resolveRequired_throwsWhenSkillCannotBeResolved() {
        given(skillRepository.findByName("Unknown Skill")).willReturn(Optional.empty());

        assertThatThrownBy(() -> skillResolver.resolveRequired("Unknown Skill"))
                .isInstanceOf(UnresolvedSkillException.class)
                .hasMessage("등록되지 않은 스킬입니다: Unknown Skill");
    }

    @Test
    @DisplayName("검색어가 비어 있으면 전체 Skill을 이름순으로 반환한다")
    void search_returnsAllSkillsWhenQueryIsBlank() {
        Skill spring = Skill.create("Spring", null);
        Skill react = Skill.create("React", null);
        given(skillRepository.findAll()).willReturn(List.of(spring, react));

        List<Skill> skills = skillResolver.search(" ");

        assertThat(skills)
                .extracting(Skill::getName)
                .containsExactly("React", "Spring");
        then(skillRepository).should().findAll();
    }

    @Test
    @DisplayName("검색어가 정규 Skill 이름과 일치하면 해당 Skill을 반환한다")
    void search_returnsSkillWhenCanonicalNameMatches() {
        Skill react = Skill.create("React", null);
        Skill vue = Skill.create("Vue", null);
        given(skillRepository.findAll()).willReturn(List.of(react, vue));

        List<Skill> skills = skillResolver.search("react");

        assertThat(skills)
                .extracting(Skill::getName)
                .containsExactly("React");
        then(skillRepository).should().findAll();
    }

    @Test
    @DisplayName("검색어가 별칭과 일치하면 정규 Skill을 반환한다")
    void search_returnsSkillWhenAliasMatches() {
        Skill react = Skill.create("React", null);
        Skill vue = Skill.create("Vue", null);
        given(skillRepository.findAll()).willReturn(List.of(react, vue));

        List<Skill> skills = skillResolver.search("리액트");

        assertThat(skills)
                .extracting(Skill::getName)
                .containsExactly("React");
        then(skillRepository).should().findAll();
    }

    @Test
    @DisplayName("검색어의 대소문자와 구분자를 정규화해 검색한다")
    void search_normalizesCaseAndSeparators() {
        Skill springBoot = Skill.create("Spring Boot", null);
        Skill spring = Skill.create("Spring", null);
        given(skillRepository.findAll()).willReturn(List.of(spring, springBoot));

        List<Skill> skills = skillResolver.search("spring-boot");

        assertThat(skills)
                .extracting(Skill::getName)
                .containsExactly("Spring Boot");
        then(skillRepository).should().findAll();
    }

    @Test
    @DisplayName("검색어가 매칭되지 않으면 빈 목록을 반환한다")
    void search_returnsEmptyWhenNoSkillMatches() {
        Skill react = Skill.create("React", null);
        Skill vue = Skill.create("Vue", null);
        given(skillRepository.findAll()).willReturn(List.of(react, vue));

        List<Skill> skills = skillResolver.search("Unknown Skill");

        assertThat(skills).isEmpty();
        then(skillRepository).should().findAll();
    }

    private static Stream<Arguments> blankInputSource() {
        return Stream.of(
                Arguments.of((String) null),
                Arguments.of(""),
                Arguments.of(" ")
        );
    }

    private static Stream<Arguments> aliasSource() {
        return Stream.of(
                Arguments.of(Named.of("React lower case", "react"), "React"),
                Arguments.of(Named.of("React.js", "React.js"), "React"),
                Arguments.of(Named.of("reactjs", "reactjs"), "React"),
                Arguments.of(Named.of("리액트", "리액트"), "React"),
                Arguments.of(Named.of("Vue.js", "Vue.js"), "Vue"),
                Arguments.of(Named.of("AngularJS", "AngularJS"), "Angular"),
                Arguments.of(Named.of("NextJS", "NextJS"), "Next.js"),
                Arguments.of(Named.of("ts", "ts"), "TypeScript"),
                Arguments.of(Named.of("js", "js"), "JavaScript"),
                Arguments.of(Named.of("tailwindcss", "tailwindcss"), "Tailwind CSS"),
                Arguments.of(Named.of("spring", "spring"), "Spring"),
                Arguments.of(Named.of("spring boot", "spring boot"), "Spring Boot"),
                Arguments.of(Named.of("spring-boot", "spring-boot"), "Spring Boot"),
                Arguments.of(Named.of("스프링부트", "스프링부트"), "Spring Boot"),
                Arguments.of(Named.of("nodejs", "nodejs"), "Node.js"),
                Arguments.of(Named.of("nest.js", "nest.js"), "NestJS"),
                Arguments.of(Named.of("py", "py"), "Python"),
                Arguments.of(Named.of("fast api", "fast api"), "FastAPI"),
                Arguments.of(Named.of("query dsl", "query dsl"), "Querydsl"),
                Arguments.of(Named.of("restful api", "restful api"), "REST API"),
                Arguments.of(Named.of("postgres", "postgres"), "PostgreSQL"),
                Arguments.of(Named.of("mongo", "mongo"), "MongoDB"),
                Arguments.of(Named.of("sql server", "sql server"), "MsSQL"),
                Arguments.of(Named.of("elastic search", "elastic search"), "Elasticsearch"),
                Arguments.of(Named.of("amazon web services", "amazon web services"), "AWS"),
                Arguments.of(Named.of("k8s", "k8s"), "Kubernetes"),
                Arguments.of(Named.of("github action", "github action"), "GitHub Actions"),
                Arguments.of(Named.of("cicd", "cicd"), "CI/CD"),
                Arguments.of(Named.of("google cloud platform", "google cloud platform"), "GCP"),
                Arguments.of(Named.of("microsoft azure", "microsoft azure"), "Azure"),
                Arguments.of(Named.of("reactnative", "reactnative"), "React Native"),
                Arguments.of(Named.of("파이토치", "파이토치"), "PyTorch"),
                Arguments.of(Named.of("tensor flow", "tensor flow"), "TensorFlow"),
                Arguments.of(Named.of("lang chain", "lang chain"), "LangChain"),
                Arguments.of(Named.of("large language model", "large language model"), "LLM"),
                Arguments.of(Named.of("피그마", "피그마"), "Figma")
        );
    }
}