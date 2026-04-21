package com.generic4.itda.service;

import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.repository.SkillRepository;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class SkillResolver {

    private static final Map<String, String> CANONICAL_NAMES = createCanonicalNames();

    private final SkillRepository skillRepository;

    public Optional<Skill> resolve(String input) {
        if (!StringUtils.hasText(input)) {
            return Optional.empty();
        }

        String normalizedInput = normalize(input);
        String canonicalName = CANONICAL_NAMES.getOrDefault(normalizedInput, input.trim());

        return skillRepository.findByName(canonicalName);
    }

    public Skill resolveRequired(String input) {
        return resolve(input)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 스킬입니다: " + input));
    }

    private static Map<String, String> createCanonicalNames() {
        Map<String, String> names = new LinkedHashMap<>();

        register(names, "React", "react", "react.js", "reactjs", "리액트");
        register(names, "Vue", "vue", "vue.js", "vuejs", "뷰");
        register(names, "Angular", "angular", "angular.js", "angularjs", "앵귤러");
        register(names, "Next.js", "next", "next.js", "nextjs", "넥스트", "넥스트js");
        register(names, "TypeScript", "typescript", "type script", "ts", "타입스크립트");
        register(names, "JavaScript", "javascript", "java script", "js", "자바스크립트");
        register(names, "HTML", "html");
        register(names, "CSS", "css");
        register(names, "Tailwind CSS", "tailwind", "tailwind css", "tailwindcss", "테일윈드");
        register(names, "Java", "java", "자바");
        register(names, "Spring", "spring", "스프링");
        register(names, "Spring Boot", "spring boot", "springboot", "spring-boot", "스프링부트");
        register(names, "Node.js", "node", "node.js", "nodejs", "노드", "노드js");
        register(names, "Express", "express", "express.js", "expressjs");
        register(names, "NestJS", "nest", "nestjs", "nest.js");
        register(names, "Python", "python", "py", "파이썬");
        register(names, "Django", "django", "장고");
        register(names, "FastAPI", "fastapi", "fast api");
        register(names, "JPA", "jpa");
        register(names, "Querydsl", "querydsl", "query dsl");
        register(names, "REST API", "rest", "rest api", "restful api", "restful");
        register(names, "GraphQL", "graphql", "graph ql");
        register(names, "MySQL", "mysql", "my sql");
        register(names, "PostgreSQL", "postgresql", "postgres", "postgre", "포스트그레스");
        register(names, "MongoDB", "mongodb", "mongo", "mongo db");
        register(names, "Redis", "redis", "레디스");
        register(names, "Oracle", "oracle", "oracle db", "oracle database");
        register(names, "MsSQL", "mssql", "ms sql", "sql server", "microsoft sql server");
        register(names, "Elasticsearch", "elasticsearch", "elastic search", "elastic");
        register(names, "AWS", "aws", "amazon web services");
        register(names, "Docker", "docker", "도커");
        register(names, "Kubernetes", "kubernetes", "k8s", "쿠버네티스");
        register(names, "GitHub Actions", "github actions", "github action", "gha");
        register(names, "Nginx", "nginx");
        register(names, "Git", "git");
        register(names, "CI/CD", "ci/cd", "cicd", "ci cd");
        register(names, "Kafka", "kafka", "apache kafka");
        register(names, "Jenkins", "jenkins");
        register(names, "GCP", "gcp", "google cloud", "google cloud platform");
        register(names, "Azure", "azure", "microsoft azure");
        register(names, "Linux", "linux", "리눅스");
        register(names, "Flutter", "flutter", "플러터");
        register(names, "React Native", "react native", "reactnative", "rn", "리액트네이티브");
        register(names, "Swift", "swift");
        register(names, "Kotlin", "kotlin", "코틀린");
        register(names, "PyTorch", "pytorch", "파이토치");
        register(names, "TensorFlow", "tensorflow", "tensor flow", "텐서플로우");
        register(names, "LangChain", "langchain", "lang chain");
        register(names, "LLM", "llm", "large language model", "large language models");
        register(names, "Figma", "figma", "피그마");

        return Map.copyOf(names);
    }

    private static void register(Map<String, String> names, String canonicalName, String... aliases) {
        names.put(normalize(canonicalName), canonicalName);
        for (String alias : aliases) {
            names.put(normalize(alias), canonicalName);
        }
    }

    private static String normalize(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[._\\-]+", " ")
                .replaceAll("\\s+", " ");
    }
}