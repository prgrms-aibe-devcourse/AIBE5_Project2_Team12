package com.generic4.itda.service;

import com.generic4.itda.domain.position.Position;
import com.generic4.itda.repository.PositionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PositionResolver {

    private static final List<String> ALLOWED_CATEGORY_NAMES = List.of(
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

    private static final Map<String, String> CANONICAL_ALIAS_TO_NAME = createAliasMap();

    private final PositionRepository positionRepository;

    public Optional<Position> resolve(String positionCategoryName) {
        Optional<String> canonicalName = resolveCanonicalName(positionCategoryName);
        if (canonicalName.isEmpty()) {
            return Optional.empty();
        }

        return positionRepository.findByName(canonicalName.get());
    }

    public List<String> findAllowedCategoryNames() {
        return ALLOWED_CATEGORY_NAMES;
    }

    public List<Position> findAllowedPositions() {
        return ALLOWED_CATEGORY_NAMES.stream()
                .map(positionRepository::findByName)
                .flatMap(Optional::stream)
                .toList();
    }

    public List<Position> search(String query) {
        String normalizedQuery = normalizeQuery(query);

        return findAllowedPositions().stream()
                .filter(position -> matchesSearch(position, normalizedQuery))
                .sorted(Comparator
                        .comparingInt((Position position) -> searchScore(position, normalizedQuery))
                        .thenComparing(Position::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Optional<String> resolveCanonicalName(String positionCategoryName) {
        if (!StringUtils.hasText(positionCategoryName)) {
            return Optional.empty();
        }

        String canonicalName = CANONICAL_ALIAS_TO_NAME.get(normalize(positionCategoryName));
        return Optional.ofNullable(canonicalName);
    }

    private static String normalizeStatic(String value) {
        if (value == null) {
            return "";
        }

        return value.toLowerCase()
                .replaceAll("[\\s/_-]+", "")
                .trim();
    }

    private String normalize(String value) {
        return normalizeStatic(value);
    }

    private boolean matchesSearch(Position position, String normalizedQuery) {
        return searchScore(position, normalizedQuery) < Integer.MAX_VALUE;
    }

    private int searchScore(Position position, String normalizedQuery) {
        String positionName = position.getName();
        if (!ALLOWED_CATEGORY_NAMES.contains(positionName)) {
            return Integer.MAX_VALUE;
        }

        String normalizedPositionName = normalize(positionName);
        if (!StringUtils.hasText(normalizedQuery)) {
            return 0;
        }

        String resolvedCanonicalName = CANONICAL_ALIAS_TO_NAME.get(normalizedQuery);
        if (positionName.equals(resolvedCanonicalName) || normalizedPositionName.equals(normalizedQuery)) {
            return 0;
        }

        for (Map.Entry<String, String> entry : CANONICAL_ALIAS_TO_NAME.entrySet()) {
            if (!entry.getValue().equals(positionName)) {
                continue;
            }

            String alias = entry.getKey();
            if (alias.equals(normalizedQuery)) {
                return 0;
            }
            if (alias.startsWith(normalizedQuery) || normalizedPositionName.startsWith(normalizedQuery)) {
                return 1;
            }
            if (alias.contains(normalizedQuery) || normalizedPositionName.contains(normalizedQuery)) {
                return 2;
            }
        }

        return Integer.MAX_VALUE;
    }

    private String normalizeQuery(String value) {
        return StringUtils.hasText(value) ? normalize(value) : "";
    }

    private static Map<String, String> createAliasMap() {
        Map<String, String> aliases = new LinkedHashMap<>();

        registerAliases(aliases, "백엔드 개발자",
                "백엔드 개발자", "백엔드", "backend", "backenddeveloper", "backendengineer",
                "서버 개발자", "서버 엔지니어", "api 개발자", "apideveloper", "java 백엔드",
                "spring 백엔드", "플랫폼 백엔드", "platformbackend");

        registerAliases(aliases, "프론트엔드 개발자",
                "프론트엔드 개발자", "프론트엔드", "frontend", "frontenddeveloper",
                "frontendengineer", "웹 프론트엔드", "webfrontend", "react 개발자",
                "reactdeveloper", "vue 개발자", "angular 개발자");

        registerAliases(aliases, "풀스택 개발자",
                "풀스택 개발자", "풀스택", "fullstack", "fullstackdeveloper",
                "fullstackengineer", "웹 개발자", "webdeveloper");

        registerAliases(aliases, "모바일 앱 개발자",
                "모바일 앱 개발자", "모바일 개발자", "앱 개발자", "앱개발자", "mobile",
                "mobiledeveloper", "mobileappdeveloper", "appdeveloper", "ios 개발자",
                "iosdeveloper", "android 개발자", "androiddeveloper", "flutter 개발자",
                "flutterdeveloper", "react native 개발자", "reactnativedeveloper");

        registerAliases(aliases, "AI 엔지니어",
                "AI 엔지니어", "AI", "AI 개발자", "aiengineer", "aideveloper",
                "ML 엔지니어", "mlengineer", "머신러닝 엔지니어", "llm 엔지니어",
                "llmengineer", "prompt engineer", "프롬프트 엔지니어", "mlops 엔지니어");

        registerAliases(aliases, "데이터 엔지니어",
                "데이터 엔지니어", "데이터", "dataengineer", "data engineer",
                "데이터 플랫폼 엔지니어", "dataplatformengineer", "etl 개발자",
                "etldeveloper", "analytics engineer");

        registerAliases(aliases, "DevOps 엔지니어",
                "DevOps 엔지니어", "devops", "devopsengineer", "SRE", "site reliability engineer",
                "platform engineer", "플랫폼 엔지니어", "인프라 엔지니어", "infra engineer",
                "cloud engineer", "클라우드 엔지니어");

        registerAliases(aliases, "UI/UX 디자이너",
                "UI/UX 디자이너", "uiuxdesigner", "ui designer", "ux designer",
                "product designer", "프로덕트 디자이너", "웹 디자이너", "앱 디자이너",
                "디자이너");

        registerAliases(aliases, "서비스 기획자",
                "서비스 기획자", "서비스기획자", "기획자", "planner", "serviceplanner",
                "product manager", "productmanager", "PM", "PO", "product owner");

        registerAliases(aliases, "QA 엔지니어",
                "QA 엔지니어", "QA", "qaengineer", "test engineer", "testengineer",
                "test automation engineer", "quality assurance", "품질보증",
                "테스트 엔지니어", "테스트 자동화 엔지니어");

        return Map.copyOf(aliases);
    }

    private static void registerAliases(Map<String, String> aliases, String canonicalName, String... names) {
        for (String name : names) {
            aliases.put(normalizeStatic(name), canonicalName);
        }
    }
}
