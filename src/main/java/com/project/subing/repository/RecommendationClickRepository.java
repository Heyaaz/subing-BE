package com.project.subing.repository;

import com.project.subing.domain.recommendation.entity.RecommendationClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationClickRepository extends JpaRepository<RecommendationClick, Long> {

    /**
     * 특정 추천 결과의 클릭 목록 조회
     */
    List<RecommendationClick> findByRecommendationResult_Id(Long recommendationResultId);

    /**
     * 사용자의 클릭 목록 조회
     */
    List<RecommendationClick> findByUser_Id(Long userId);

    /**
     * 추천 결과별 클릭 수 집계 (A/B 테스트 분석용)
     */
    @Query("SELECT rc.recommendationResult.promptVersion as promptVersion, COUNT(rc) as clickCount " +
           "FROM RecommendationClick rc " +
           "GROUP BY rc.recommendationResult.promptVersion")
    List<Object[]> countClicksByPromptVersion();
}