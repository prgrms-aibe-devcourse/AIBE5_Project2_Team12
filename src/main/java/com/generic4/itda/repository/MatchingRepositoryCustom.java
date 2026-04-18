package com.generic4.itda.repository;

import com.generic4.itda.dto.freelancer.FreelancerDashboardItem;

import java.util.List;

public interface MatchingRepositoryCustom {
    List<FreelancerDashboardItem> getDashboardItems(String email, String status, String q);
}
