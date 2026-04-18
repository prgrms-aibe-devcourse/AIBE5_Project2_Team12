package com.generic4.itda.service;

import com.generic4.itda.dto.freelancer.FreelancerDashboardItem;
import com.generic4.itda.repository.MatchingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FreelancerDashboardServiceTest {

    @Mock
    private MatchingRepository matchingRepository;

    @InjectMocks
    private FreelancerDashboardService freelancerDashboardService;

    @Test
    @DisplayName("대시보드 아이템 조회 - 레포지토리 호출 및 데이터 반환 검증")
    void getDashboardItems_Success() {
        // Given (준비)
        String email = "test@test.com";
        String status = "PROPOSED";
        String keyword = "개발";

        // 가짜 데이터 생성 (내용은 중요하지 않음, 호출 여부 확인용)
        List<FreelancerDashboardItem> mockItems = List.of();

        given(matchingRepository.getDashboardItems(email, status, keyword))
                .willReturn(mockItems);

        // When (실행)
        List<FreelancerDashboardItem> result = freelancerDashboardService.getDashboardItems(email, status, keyword);

        // Then (검증)
        assertThat(result).isNotNull();
        // 레포지토리의 메서드가 우리가 넘긴 파라미터 그대로 호출되었는지 확인
        verify(matchingRepository).getDashboardItems(email, status, keyword);
    }
}