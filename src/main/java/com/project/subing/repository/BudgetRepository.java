package com.project.subing.repository;

import com.project.subing.domain.budget.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByUser_IdAndYearAndMonth(Long userId, Integer year, Integer month);

    List<Budget> findByUser_IdOrderByYearDescMonthDesc(Long userId);

    boolean existsByUser_IdAndYearAndMonth(Long userId, Integer year, Integer month);
}
