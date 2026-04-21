package com.generic4.itda.controller;

import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.service.SkillResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SkillSearchController {

    private final SkillResolver skillResolver;

    @GetMapping("/api/skills")
    public List<SkillSearchResponse> search(@RequestParam(name = "query", required = false) String query) {
        return skillResolver.search(query).stream()
                .map(SkillSearchResponse::from)
                .toList();
    }

    public record SkillSearchResponse(Long id, String name) {

        private static SkillSearchResponse from(Skill skill) {
            return new SkillSearchResponse(skill.getId(), skill.getName());
        }
    }
}