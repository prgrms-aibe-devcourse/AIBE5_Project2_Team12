package com.generic4.itda.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchingCancellationScheduler {

    private final MatchingService matchingService;

    @Scheduled(fixedDelay = 60_000)
    public void cancelOverdueAcceptedCancellationRequests() {
        matchingService.cancelOverdueAcceptedCancellationRequests(LocalDateTime.now());
    }
}
