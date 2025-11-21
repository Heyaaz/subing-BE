package com.project.subing.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatisticsResponse {
    private Long totalUsers;
    private Long freeUsers;
    private Long proUsers;
    private Long activeSubscriptions;
    private Long totalServices;
    private Long totalPlans;
    private Integer totalMonthlyRevenue;  // 총 월 매출 (PRO 사용자 * 9900)
    private Map<String, Long> usersByMonth;  // 월별 가입자 수
    private Map<String, Long> subscriptionsByCategory;  // 카테고리별 구독 수
}