package com.project.subing.repository;

import com.project.subing.domain.recommendation.entity.RecommendationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationResultRepository extends JpaRepository<RecommendationResult, Long> {

    List<RecommendationResult> findByUser_IdOrderByCreatedAtDesc(Long userId);

    List<RecommendationResult> findTop5ByUser_IdOrderByCreatedAtDesc(Long userId);
}
