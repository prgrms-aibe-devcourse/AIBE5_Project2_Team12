package com.generic4.itda.controller;

import com.generic4.itda.domain.position.Position;
import com.generic4.itda.service.PositionResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PositionSearchController {

    private final PositionResolver positionResolver;

    @GetMapping("/api/positions")
    public List<PositionSearchResponse> search(@RequestParam(name = "query", required = false) String query) {
        return positionResolver.search(query).stream()
                .map(PositionSearchResponse::from)
                .toList();
    }

    public record PositionSearchResponse(Long id, String name) {

        private static PositionSearchResponse from(Position position) {
            return new PositionSearchResponse(position.getId(), position.getName());
        }
    }
}
