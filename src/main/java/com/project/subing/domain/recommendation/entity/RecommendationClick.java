package com.project.subing.domain.recommendation.entity;

import com.project.subing.domain.service.entity.ServiceEntity;
import com.project.subing.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 추천 결과 클릭 추적
 * 사용자가 추천받은 서비스를 클릭했을 때 기록
 */
@Entity
@Table(name = "recommendation_clicks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RecommendationClick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_result_id", nullable = false)
    private RecommendationResult recommendationResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    @CreationTimestamp
    @Column(name = "clicked_at", nullable = false, updatable = false)
    private LocalDateTime clickedAt;
}