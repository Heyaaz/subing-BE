package com.project.subing.service;

import com.project.subing.domain.budget.entity.Budget;
import com.project.subing.domain.user.entity.User;
import com.project.subing.exception.auth.UnauthorizedAccessException;
import com.project.subing.exception.entity.BudgetNotFoundException;
import com.project.subing.exception.entity.UserNotFoundException;
import com.project.subing.repository.BudgetRepository;
import com.project.subing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;

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
                .orElseThrow(() -> new UserNotFoundException(userId));

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
        Optional<Budget> exact = budgetRepository.findByUser_IdAndYearAndMonth(userId, year, month);
        if (exact.isPresent()) return exact;
        return budgetRepository.findLatestOnOrBefore(userId, year, month, PageRequest.of(0, 1))
                .stream().findFirst();
    }

    @Transactional(readOnly = true)
    public List<Budget> getAllBudgets(Long userId) {
        return budgetRepository.findByUser_IdOrderByYearDescMonthDesc(userId);
    }

    @Transactional(readOnly = true)
    public Optional<Budget> getCurrentMonthBudget(Long userId) {
        LocalDate now = LocalDate.now();
        Optional<Budget> exact = budgetRepository.findByUser_IdAndYearAndMonth(userId, now.getYear(), now.getMonthValue());
        if (exact.isPresent()) return exact;
        return budgetRepository.findLatestOnOrBefore(userId, now.getYear(), now.getMonthValue(), PageRequest.of(0, 1))
                .stream().findFirst();
    }

    public void deleteBudget(Long budgetId, Long userId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new BudgetNotFoundException(budgetId));

        if (!budget.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("예산 삭제 권한이 없습니다.");
        }

        budgetRepository.delete(budget);
    }
}
