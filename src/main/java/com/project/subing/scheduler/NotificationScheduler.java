package com.project.subing.scheduler;

import com.project.subing.domain.budget.entity.Budget;
import com.project.subing.domain.notification.entity.NotificationType;
import com.project.subing.domain.subscription.entity.UserSubscription;
import com.project.subing.repository.BudgetRepository;
import com.project.subing.repository.UserSubscriptionRepository;
import com.project.subing.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final BudgetRepository budgetRepository;
    private final NotificationService notificationService;

    /**
     * 매일 자정: 모든 알림 체크를 단일 쿼리로 통합 (메모리 최적화)
     * 기존: findAllActiveWithServiceAndUser() 4번 호출 → 변경: 1번 호출
     */
    @Scheduled(cron = "0 0 0 * * *")
    @PreAuthorize("permitAll()")
    public void checkDailyNotifications() {
        log.info("일일 알림 체크 시작");

        List<UserSubscription> activeSubscriptions = userSubscriptionRepository.findAllActiveWithServiceAndUser();
        LocalDate today = LocalDate.now();

        checkPaymentDueNotifications(activeSubscriptions, today);
        checkBudgetExceededNotifications(activeSubscriptions, today);
        checkSubscriptionRenewalNotifications(activeSubscriptions, today);

        log.info("일일 알림 체크 완료");
    }

    /**
     * 매주 월요일 자정: 미사용 구독 감지
     */
    @Scheduled(cron = "0 0 0 * * MON")
    @PreAuthorize("permitAll()")
    public void checkWeeklyNotifications() {
        log.info("주간 알림 체크 시작");

        List<UserSubscription> activeSubscriptions = userSubscriptionRepository.findAllActiveWithServiceAndUser();
        LocalDate today = LocalDate.now();

        checkUnusedSubscriptionNotifications(activeSubscriptions, today);

        log.info("주간 알림 체크 완료");
    }

    private void checkPaymentDueNotifications(List<UserSubscription> activeSubscriptions, LocalDate today) {
        for (UserSubscription subscription : activeSubscriptions) {
            try {
                LocalDate nextBillingDate = subscription.getNextBillingDate();
                long daysUntilBilling = ChronoUnit.DAYS.between(today, nextBillingDate);

                if (daysUntilBilling == 3) {
                    notificationService.createNotification(
                            subscription.getUser().getId(),
                            NotificationType.PAYMENT_DUE_3DAYS,
                            "결제일 3일 전 알림",
                            String.format("%s 구독이 3일 후(%s)에 결제됩니다. 금액: %,d원",
                                    subscription.getService().getServiceName(),
                                    nextBillingDate,
                                    subscription.getMonthlyPrice()),
                            subscription.getId()
                    );
                }

                if (daysUntilBilling == 1) {
                    notificationService.createNotification(
                            subscription.getUser().getId(),
                            NotificationType.PAYMENT_DUE_1DAY,
                            "결제일 1일 전 알림",
                            String.format("%s 구독이 내일(%s) 결제됩니다. 금액: %,d원",
                                    subscription.getService().getServiceName(),
                                    nextBillingDate,
                                    subscription.getMonthlyPrice()),
                            subscription.getId()
                    );
                }
            } catch (Exception e) {
                log.error("결제일 알림 생성 실패 - subscriptionId: {}", subscription.getId(), e);
            }
        }
    }

    private void checkBudgetExceededNotifications(List<UserSubscription> activeSubscriptions, LocalDate today) {
        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();

        List<Budget> budgets = budgetRepository.findByYearAndMonthWithUser(currentYear, currentMonth);

        Map<Long, Long> userExpenseMap = activeSubscriptions.stream()
                .collect(Collectors.groupingBy(
                        subscription -> subscription.getUser().getId(),
                        Collectors.summingLong(subscription -> subscription.getMonthlyPrice().longValue())
                ));

        for (Budget budget : budgets) {
            try {
                Long userId = budget.getUser().getId();
                Long totalExpense = userExpenseMap.getOrDefault(userId, 0L);
                Long budgetLimit = budget.getMonthlyLimit();

                if (totalExpense > budgetLimit) {
                    notificationService.createNotification(
                            userId,
                            NotificationType.BUDGET_EXCEEDED,
                            "예산 초과 알림",
                            String.format("이번 달 구독 지출(%,d원)이 설정한 예산(%,d원)을 %,d원 초과했습니다.",
                                    totalExpense, budgetLimit, totalExpense - budgetLimit),
                            null
                    );
                }
            } catch (Exception e) {
                log.error("예산 초과 알림 생성 실패 - budgetId: {}", budget.getId(), e);
            }
        }
    }

    private void checkUnusedSubscriptionNotifications(List<UserSubscription> activeSubscriptions, LocalDate today) {
        LocalDate thresholdDate = today.minusDays(90);

        List<UserSubscription> longTermSubscriptions = activeSubscriptions.stream()
                .filter(subscription -> subscription.getCreatedAt().toLocalDate().isBefore(thresholdDate))
                .toList();

        for (UserSubscription subscription : longTermSubscriptions) {
            try {
                long daysActive = ChronoUnit.DAYS.between(subscription.getCreatedAt().toLocalDate(), today);

                notificationService.createNotification(
                        subscription.getUser().getId(),
                        NotificationType.UNUSED_SUBSCRIPTION,
                        "장기 구독 확인 필요",
                        String.format("%s 구독을 %d일째 사용 중입니다. 계속 사용하시나요? 불필요한 구독은 해지하여 비용을 절약하세요.",
                                subscription.getService().getServiceName(), daysActive),
                        subscription.getId()
                );
            } catch (Exception e) {
                log.error("미사용 구독 감지 알림 생성 실패 - subscriptionId: {}", subscription.getId(), e);
            }
        }
    }

    private void checkSubscriptionRenewalNotifications(List<UserSubscription> activeSubscriptions, LocalDate today) {
        int todayDay = today.getDayOfMonth();

        for (UserSubscription subscription : activeSubscriptions) {
            try {
                if (subscription.getBillingDate().equals(todayDay)) {
                    notificationService.createNotification(
                            subscription.getUser().getId(),
                            NotificationType.SUBSCRIPTION_RENEWAL,
                            "구독 갱신 완료",
                            String.format("%s 구독이 오늘 갱신되었습니다. 결제 금액: %,d원",
                                    subscription.getService().getServiceName(),
                                    subscription.getMonthlyPrice()),
                            subscription.getId()
                    );
                }
            } catch (Exception e) {
                log.error("구독 갱신 알림 생성 실패 - subscriptionId: {}", subscription.getId(), e);
            }
        }
    }
}
