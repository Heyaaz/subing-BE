package com.project.subing.service;

import com.project.subing.domain.budget.entity.Budget;
import com.project.subing.domain.user.entity.User;
import com.project.subing.repository.BudgetRepository;
import com.project.subing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    public Budget setBudget(Long userId, Integer year, Integer month, Long monthlyLimit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 기존 예산이 있으면 업데이트, 없으면 생성
        Optional<Budget> existingBudget = budgetRepository.findByUser_IdAndYearAndMonth(userId, year, month);

        if (existingBudget.isPresent()) {
            Budget budget = existingBudget.get();
            budget.updateMonthlyLimit(monthlyLimit);
            return budget;
        } else {
            Budget newBudget = Budget.builder()
                    .user(user)
                    .year(year)
                    .month(month)
                    .monthlyLimit(monthlyLimit)
                    .build();
            return budgetRepository.save(newBudget);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Budget> getBudget(Long userId, Integer year, Integer month) {
        return budgetRepository.findByUser_IdAndYearAndMonth(userId, year, month);
    }

    @Transactional(readOnly = true)
    public List<Budget> getAllBudgets(Long userId) {
        return budgetRepository.findByUser_IdOrderByYearDescMonthDesc(userId);
    }

    @Transactional(readOnly = true)
    public Optional<Budget> getCurrentMonthBudget(Long userId) {
        LocalDate now = LocalDate.now();
        return budgetRepository.findByUser_IdAndYearAndMonth(userId, now.getYear(), now.getMonthValue());
    }

    public void deleteBudget(Long budgetId, Long userId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("예산을 찾을 수 없습니다."));

        if (!budget.getUser().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        budgetRepository.delete(budget);
    }
}
