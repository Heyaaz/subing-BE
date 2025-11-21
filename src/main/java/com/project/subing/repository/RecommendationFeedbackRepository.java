package com.project.subing.repository;

import com.project.subing.domain.recommendation.entity.RecommendationFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecommendationFeedbackRepository extends JpaRepository<RecommendationFeedback, Long> {

    Optional<RecommendationFeedback> findByRecommendationResult_IdAndUser_Id(Long recommendationId, Long userId);

    boolean existsByRecommendationResult_IdAndUser_Id(Long recommendationId, Long userId);
}
