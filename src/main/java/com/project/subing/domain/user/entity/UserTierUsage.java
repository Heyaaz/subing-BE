package com.project.subing.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_tier_usage",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "year", "month"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserTierUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    @Column(nullable = false)
    @Builder.Default
    private int gptRecommendationCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int optimizationCheckCount = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 비즈니스 로직
    public void incrementGptRecommendation() {
        this.gptRecommendationCount++;
    }

    public void incrementOptimizationCheck() {
        this.optimizationCheckCount++;
    }

    public void resetGptRecommendation() {
        this.gptRecommendationCount = 0;
    }

    public void resetOptimizationCheck() {
        this.optimizationCheckCount = 0;
    }

    public void resetAll() {
        this.gptRecommendationCount = 0;
        this.optimizationCheckCount = 0;
    }
}