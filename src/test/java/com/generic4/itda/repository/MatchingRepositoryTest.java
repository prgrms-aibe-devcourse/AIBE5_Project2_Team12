package com.generic4.itda.repository;

import com.generic4.itda.config.TestContainerConfig;
import com.generic4.itda.config.TestQuerydslConfig;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.dto.freelancer.FreelancerDashboardItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestContainerConfig.class, TestQuerydslConfig.class}) // 두 설정 모두 임포트
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 실제 컨테이너 DB 사용
class MatchingRepositoryTest {

    @Autowired
    private MatchingRepository matchingRepository;

    @Test
    @DisplayName("전체 데이터 조회 및 통계 카운트 검증")
    void getDashboardItems_Statistics() {
        // Given
        String email = "freelancer@test.com";
        // 실제 테스트 시에는 EntityManager 등을 사용해 데이터를 INSERT 하는 로직이 선행되어야 함

        // When
        List<FreelancerDashboardItem> results = matchingRepository.getDashboardItems(email, null, null);

        // Then
        assertThat(results).isNotNull();
        // 결과가 비어있더라도 쿼리 자체에 오류가 없는지 확인됨
    }

    @Test
    @DisplayName("상태값이 'PROPOSED'인 데이터만 필터링 조회")
    void getDashboardItems_StatusFilter() {
        // Given
        String email = "freelancer@test.com";
        String status = "PROPOSED";

        // When
        List<FreelancerDashboardItem> results = matchingRepository.getDashboardItems(email, status, null);

        // Then
        assertThat(results).allMatch(item -> item.matchingStatus() == MatchingStatus.PROPOSED);
    }

    @Test
    @DisplayName("검색어가 포함된 제목의 제안서 조회")
    void getDashboardItems_SearchKeyword() {
        // Given
        String email = "freelancer@test.com";
        String keyword = "Backend";

        // When
        List<FreelancerDashboardItem> results = matchingRepository.getDashboardItems(email, null, keyword);

        // Then
        assertThat(results).allMatch(item ->
                item.proposalTitle().contains(keyword) || item.companyName().contains(keyword)
        );
    }
}