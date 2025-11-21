package com.project.subing.repository;

import com.project.subing.domain.user.entity.UserTierUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserTierUsageRepository extends JpaRepository<UserTierUsage, Long> {

    // 특정 사용자의 특정 년월 사용량 조회
    Optional<UserTierUsage> findByUserIdAndYearAndMonth(Long userId, int year, int month);

    // 특정 사용자의 특정 년월 사용량 존재 여부
    boolean existsByUserIdAndYearAndMonth(Long userId, int year, int month);
}