package com.project.subing.service;

import com.project.subing.domain.common.Currency;
import com.project.subing.domain.common.ServiceCategory;
import com.project.subing.domain.service.entity.ServiceEntity;
import com.project.subing.domain.subscription.entity.UserSubscription;
import com.project.subing.dto.statistics.CategoryExpenseResponse;
import com.project.subing.dto.statistics.ExpenseAnalysisResponse;
import com.project.subing.dto.statistics.MonthlyExpenseResponse;
import com.project.subing.repository.ServiceRepository;
import com.project.subing.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final ServiceRepository serviceRepository;

    // 환율 상수 (1 USD = 1300 KRW)
    private static final double USD_TO_KRW_RATE = 1300.0;
    
    public MonthlyExpenseResponse getMonthlyExpense(Long userId, Integer year, Integer month) {
        List<UserSubscription> subscriptions = userSubscriptionRepository.findByUserId(userId);

        // 특정 월(year, month)에 활성이었던 구독만 필터링
        YearMonth targetMonth = YearMonth.of(year, month);
        List<UserSubscription> activeInMonth = subscriptions.stream()
            .filter(sub -> sub.isActiveInMonth(targetMonth))
            .collect(Collectors.toList());

        int totalAmount = 0;
        Map<String, CategoryExpenseResponse> categoryMap = new HashMap<>();

        for (UserSubscription subscription : activeInMonth) {
            ServiceEntity service = subscription.getService();
            String category = service.getCategory().name();
            int amount = subscription.getMonthlyPrice();

            // 환율 변환 추가
            if (subscription.getCurrency() == Currency.USD) {
                amount = (int) (amount * USD_TO_KRW_RATE);
            }

            totalAmount += amount;

            if (categoryMap.containsKey(category)) {
                CategoryExpenseResponse existing = categoryMap.get(category);
                CategoryExpenseResponse updated = CategoryExpenseResponse.builder()
                        .category(category)
                        .amount(existing.getAmount() + amount)
                        .subscriptionCount(existing.getSubscriptionCount() + 1)
                        .percentage(0.0) // 나중에 계산
                        .serviceNames(addServiceName(existing.getServiceNames(), service.getServiceName()))
                        .build();
                categoryMap.put(category, updated);
            } else {
                List<String> serviceNames = new ArrayList<>();
                serviceNames.add(service.getServiceName());

                CategoryExpenseResponse newCategory = CategoryExpenseResponse.builder()
                        .category(category)
                        .amount(amount)
                        .subscriptionCount(1)
                        .percentage(0.0) // 나중에 계산
                        .serviceNames(serviceNames)
                        .build();
                categoryMap.put(category, newCategory);
            }
        }
        
        // 퍼센티지 계산
        List<CategoryExpenseResponse> categoryExpenses = new ArrayList<>();
        for (CategoryExpenseResponse category : categoryMap.values()) {
            double percentage = totalAmount > 0 ? (double) category.getAmount() / totalAmount * 100 : 0.0;
            CategoryExpenseResponse updatedCategory = CategoryExpenseResponse.builder()
                    .category(category.getCategory())
                    .amount(category.getAmount())
                    .subscriptionCount(category.getSubscriptionCount())
                    .percentage(Math.round(percentage * 100.0) / 100.0)
                    .serviceNames(category.getServiceNames())
                    .build();
            categoryExpenses.add(updatedCategory);
        }
        
        return MonthlyExpenseResponse.builder()
                .year(year)
                .month(month)
                .totalAmount(totalAmount)
                .activeSubscriptions(activeInMonth.size())
                .categoryExpenses(categoryExpenses)
                .generatedAt(LocalDateTime.now())
                .build();
    }
    
    public ExpenseAnalysisResponse getExpenseAnalysis(Long userId, Integer year, Integer month) {
        int currentYear = year;
        int currentMonthValue = month;

        // 현재 월 지출
        MonthlyExpenseResponse currentMonthData = getMonthlyExpense(userId, currentYear, currentMonthValue);

        // 이전 월 지출
        int previousMonth = currentMonthValue == 1 ? 12 : currentMonthValue - 1;
        int previousYear = currentMonthValue == 1 ? currentYear - 1 : currentYear;
        MonthlyExpenseResponse previousMonthData = getMonthlyExpense(userId, previousYear, previousMonth);
        
        // 월별 변화 계산
        int monthlyChange = currentMonthData.getTotalAmount() - previousMonthData.getTotalAmount();
        double monthlyChangePercentage = previousMonthData.getTotalAmount() > 0 
                ? (double) monthlyChange / previousMonthData.getTotalAmount() * 100 
                : 0.0;
        
        // 연간 총 지출 계산
        int yearlyTotal = 0;
        for (int m = 1; m <= 12; m++) {
            MonthlyExpenseResponse monthData = getMonthlyExpense(userId, year, m);
            yearlyTotal += monthData.getTotalAmount();
        }
        
        // 평균 월 지출
        int averageMonthlyExpense = yearlyTotal / 12;
        
        // 상위 지출 카테고리
        List<String> topExpenseCategories = new ArrayList<>();
        for (CategoryExpenseResponse category : currentMonthData.getCategoryExpenses()) {
            topExpenseCategories.add(category.getCategory());
        }
        
        // 추천사항 생성
        List<String> recommendations = generateRecommendations(currentMonthData, monthlyChange);
        
        return ExpenseAnalysisResponse.builder()
                .currentMonthTotal(currentMonthData.getTotalAmount())
                .previousMonthTotal(previousMonthData.getTotalAmount())
                .monthlyChange(monthlyChange)
                .monthlyChangePercentage(Math.round(monthlyChangePercentage * 100.0) / 100.0)
                .yearlyTotal(yearlyTotal)
                .averageMonthlyExpense(averageMonthlyExpense)
                .topExpenseCategories(topExpenseCategories)
                .recommendations(recommendations)
                .generatedAt(LocalDateTime.now())
                .build();
    }
    
    private List<String> addServiceName(List<String> serviceNames, String newServiceName) {
        List<String> updatedNames = new ArrayList<>(serviceNames);
        if (!updatedNames.contains(newServiceName)) {
            updatedNames.add(newServiceName);
        }
        return updatedNames;
    }
    
    private List<String> generateRecommendations(MonthlyExpenseResponse currentMonth, int monthlyChange) {
        List<String> recommendations = new ArrayList<>();
        
        if (monthlyChange > 0) {
            recommendations.add("이번 달 지출이 " + monthlyChange + "원 증가했습니다. 불필요한 구독을 검토해보세요.");
        } else if (monthlyChange < 0) {
            recommendations.add("이번 달 지출이 " + Math.abs(monthlyChange) + "원 절약되었습니다. 좋은 관리입니다!");
        }
        
        if (currentMonth.getTotalAmount() > 100000) {
            recommendations.add("월 지출이 10만원을 초과했습니다. 구독 서비스를 정리해보세요.");
        }
        
        if (currentMonth.getActiveSubscriptions() > 10) {
            recommendations.add("활성 구독이 10개를 초과했습니다. 사용하지 않는 서비스를 해지해보세요.");
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("현재 구독 관리가 잘 되고 있습니다. 계속 유지해보세요!");
        }
        
        return recommendations;
    }
}
