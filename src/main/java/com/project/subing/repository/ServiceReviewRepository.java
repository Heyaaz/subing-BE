package com.project.subing.repository;

import com.project.subing.domain.review.entity.ServiceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceReviewRepository extends JpaRepository<ServiceReview, Long> {

    // 서비스별 리뷰 목록 조회
    List<ServiceReview> findByServiceIdOrderByCreatedAtDesc(Long serviceId);

    // 사용자별 리뷰 목록 조회
    List<ServiceReview> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 특정 사용자가 특정 서비스에 작성한 리뷰 조회
    Optional<ServiceReview> findByUserIdAndServiceId(Long userId, Long serviceId);

    // 서비스별 평균 평점 조회
    @Query("SELECT AVG(r.rating) FROM ServiceReview r WHERE r.service.id = :serviceId")
    Double findAverageRatingByServiceId(@Param("serviceId") Long serviceId);

    // 서비스별 리뷰 개수 조회
    Long countByServiceId(Long serviceId);

    // 사용자가 해당 서비스에 리뷰를 작성했는지 확인
    boolean existsByUserIdAndServiceId(Long userId, Long serviceId);
}