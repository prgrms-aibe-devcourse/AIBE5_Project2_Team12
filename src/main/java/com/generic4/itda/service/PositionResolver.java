package com.generic4.itda.service;

import com.generic4.itda.domain.position.Position;
import com.generic4.itda.repository.PositionRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PositionResolver {

    private static final List<String> REMOVABLE_SUFFIXES = List.of(
            "developer",
            "engineer",
            "designer",
            "manager",
            "lead",
            "개발자",
            "개발",
            "엔지니어",
            "디자이너",
            "기획자",
            "매니저",
            "리드"
    );

    private final PositionRepository positionRepository;

    public Optional<Position> resolve(String positionCategoryName) {
        if (!StringUtils.hasText(positionCategoryName)) {
            return Optional.empty();
        }

        List<Position> positions = positionRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        if (positions.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Position> exactIndex = new LinkedHashMap<>();
        Map<String, Position> aliasIndex = new LinkedHashMap<>();
        Set<String> ambiguousAliases = new LinkedHashSet<>();

        for (Position position : positions) {
            indexPosition(exactIndex, aliasIndex, ambiguousAliases, position);
        }

        String normalizedInput = normalize(positionCategoryName);
        Position exactMatch = exactIndex.get(normalizedInput);
        if (exactMatch != null) {
            return Optional.of(exactMatch);
        }

        for (String alias : buildAliases(positionCategoryName)) {
            if (ambiguousAliases.contains(alias)) {
                continue;
            }

            Position resolved = aliasIndex.get(alias);
            if (resolved != null) {
                return Optional.of(resolved);
            }
        }

        return Optional.empty();
    }

    public List<String> findAllowedCategoryNames() {
        return positionRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(Position::getName)
                .toList();
    }

    private void indexPosition(
            Map<String, Position> exactIndex,
            Map<String, Position> aliasIndex,
            Set<String> ambiguousAliases,
            Position position
    ) {
        String exactKey = normalize(position.getName());
        exactIndex.putIfAbsent(exactKey, position);

        for (String alias : buildAliases(position.getName())) {
            Position existing = aliasIndex.get(alias);
            if (existing == null) {
                aliasIndex.put(alias, position);
                continue;
            }

            if (!isSamePosition(existing, position)) {
                aliasIndex.remove(alias);
                ambiguousAliases.add(alias);
            }
        }
    }

    private boolean isSamePosition(Position source, Position target) {
        if (source == target) {
            return true;
        }
        if (source.getId() != null && target.getId() != null) {
            return source.getId().equals(target.getId());
        }
        return source.getName().equals(target.getName());
    }

    private List<String> buildAliases(String value) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }

        Set<String> aliases = new LinkedHashSet<>();
        aliases.add(normalized);

        String stripped = stripSuffix(normalized);
        if (StringUtils.hasText(stripped)) {
            aliases.add(stripped);
        }

        return new ArrayList<>(aliases);
    }

    private String stripSuffix(String normalized) {
        String current = normalized;
        boolean changed = true;

        while (changed) {
            changed = false;
            for (String suffix : REMOVABLE_SUFFIXES) {
                if (current.endsWith(suffix) && current.length() > suffix.length()) {
                    current = current.substring(0, current.length() - suffix.length());
                    changed = true;
                    break;
                }
            }
        }

        return current;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.toLowerCase()
                .replaceAll("[\\s/_-]+", "")
                .trim();
    }
}
