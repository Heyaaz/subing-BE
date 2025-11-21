package com.project.subing.service;

import com.project.subing.domain.user.entity.User;
import com.project.subing.domain.user.entity.UserTier;
import com.project.subing.domain.user.entity.UserTierUsage;
import com.project.subing.repository.UserRepository;
import com.project.subing.repository.UserTierUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class TierLimitService {

    private final UserRepository userRepository;
    private final UserTierUsageRepository usageRepository;

    // 현재 월의 사용량 조회 (없으면 생성)
    public UserTierUsage getCurrentMonthUsage(Long userId) {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        return usageRepository.findByUserIdAndYearAndMonth(userId, year, month)
                .orElseGet(() -> usageRepository.save(
                        UserTierUsage.builder()
                                .userId(userId)
                                .year(year)
                                .month(month)
                                .build()
                ));
    }

    // GPT 추천 사용 가능 여부 체크
    public boolean canUseGptRecommendation(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // PRO 티어는 무제한
        if (user.getTier().isUnlimited("gpt")) {
            return true;
        }

        // FREE 티어는 제한 체크
        UserTierUsage usage = getCurrentMonthUsage(userId);
        int limit = user.getTier().getMaxGptRecommendations();
        return usage.getGptRecommendationCount() < limit;
    }

    // GPT 추천 사용량 증가
    public void incrementGptRecommendation(Long userId) {
        UserTierUsage usage = getCurrentMonthUsage(userId);
        usage.incrementGptRecommendation();
        usageRepository.save(usage);
    }

    // 최적화 체크 사용 가능 여부 체크
    public boolean canUseOptimizationCheck(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // PRO 티어는 무제한
        if (user.getTier().isUnlimited("optimization")) {
            return true;
        }

        // FREE 티어는 제한 체크
        UserTierUsage usage = getCurrentMonthUsage(userId);
        int limit = user.getTier().getMaxOptimizationChecks();
        return usage.getOptimizationCheckCount() < limit;
    }

    // 최적화 체크 사용량 증가
    public void incrementOptimizationCheck(Long userId) {
        UserTierUsage usage = getCurrentMonthUsage(userId);
        usage.incrementOptimizationCheck();
        usageRepository.save(usage);
    }

    // 남은 GPT 추천 횟수
    public int getRemainingGptRecommendations(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (user.getTier().isUnlimited("gpt")) {
            return -1; // 무제한
        }

        UserTierUsage usage = getCurrentMonthUsage(userId);
        int limit = user.getTier().getMaxGptRecommendations();
        int remaining = limit - usage.getGptRecommendationCount();
        return Math.max(0, remaining);
    }

    // 남은 최적화 체크 횟수
    public int getRemainingOptimizationChecks(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (user.getTier().isUnlimited("optimization")) {
            return -1; // 무제한
        }

        UserTierUsage usage = getCurrentMonthUsage(userId);
        int limit = user.getTier().getMaxOptimizationChecks();
        int remaining = limit - usage.getOptimizationCheckCount();
        return Math.max(0, remaining);
    }

    // 사용자 티어 업그레이드
    public void upgradeTier(Long userId, UserTier newTier) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        user.upgradeTier(newTier);
        userRepository.save(user);
    }
}