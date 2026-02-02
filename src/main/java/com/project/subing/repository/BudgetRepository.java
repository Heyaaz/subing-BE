package com.project.subing.repository;

import com.project.subing.domain.budget.entity.Budget;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByUser_IdAndYearAndMonth(Long userId, Integer year, Integer month);

    List<Budget> findByUser_IdOrderByYearDescMonthDesc(Long userId);

    /** 해당 년월 이전(포함)에 설정된 예산 중 가장 최근 것 1건 */
    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND (b.year < :year OR (b.year = :year AND b.month <= :month)) ORDER BY b.year DESC, b.month DESC")
    List<Budget> findLatestOnOrBefore(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month, Pageable pageable);

    boolean existsByUser_IdAndYearAndMonth(Long userId, Integer year, Integer month);
}
