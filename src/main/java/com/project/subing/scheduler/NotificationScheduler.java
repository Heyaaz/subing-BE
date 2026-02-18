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

    // 매일 자정에 실행 (결제일 알림 체크)
    @Scheduled(cron = "0 0 0 * * *")
    @PreAuthorize("permitAll()")
    public void checkPaymentDueNotifications() {
        log.info("결제일 알림 체크 시작");

        List<UserSubscription> activeSubscriptions = userSubscriptionRepository.findAllActiveWithServiceAndUser();

        LocalDate today = LocalDate.now();

        for (UserSubscription subscription : activeSubscriptions) {
            try {
                LocalDate nextBillingDate = subscription.getNextBillingDate();
                long daysUntilBilling = ChronoUnit.DAYS.between(today, nextBillingDate);

                // 3일 전 알림
                if (daysUntilBilling == 3) {
                    String title = "결제일 3일 전 알림";
                    String message = String.format("%s 구독이 3일 후(%s)에 결제됩니다. 금액: %,d원",
                            subscription.getService().getServiceName(),
                            nextBillingDate,
                            subscription.getMonthlyPrice());

                    notificationService.createNotification(
                            subscription.getUser().getId(),
                            NotificationType.PAYMENT_DUE_3DAYS,
                            title,
                            message,
                            subscription.getId()
                    );
                }

                // 1일 전 알림
                if (daysUntilBilling == 1) {
                    String title = "결제일 1일 전 알림";
                    String message = String.format("%s 구독이 내일(%s) 결제됩니다. 금액: %,d원",
                            subscription.getService().getServiceName(),
                            nextBillingDate,
                            subscription.getMonthlyPrice());

                    notificationService.createNotification(
                            subscription.getUser().getId(),
                            NotificationType.PAYMENT_DUE_1DAY,
                            title,
                            message,
                            subscription.getId()
                    );
                }
            } catch (Exception e) {
                log.error("결제일 알림 생성 실패 - subscriptionId: {}", subscription.getId(), e);
            }
        }

        log.info("결제일 알림 체크 완료");
    }

    // 매일 자정에 실행 (예산 초과 알림 체크)
    @Scheduled(cron = "0 0 0 * * *")
    @PreAuthorize("permitAll()")
    public void checkBudgetExceededNotifications() {
        log.info("예산 초과 알림 체크 시작");

        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();

        // 현재 월 예산만 조회 (User JOIN FETCH)
        List<Budget> budgets = budgetRepository.findByYearAndMonthWithUser(currentYear, currentMonth);

        // 활성 구독만 조회 (Service, User JOIN FETCH)
        List<UserSubscription> activeSubscriptions = userSubscriptionRepository.findAllActiveWithServiceAndUser();

        // 사용자별 총 지출 계산
        Map<Long, Long> userExpenseMap = activeSubscriptions.stream()
                .collect(Collectors.groupingBy(
                        subscription -> subscription.getUser().getId(),
                        Collectors.summingLong(subscription -> subscription.getMonthlyPrice().longValue())
                ));

        // 예산 초과 체크
        for (Budget budget : budgets) {
            try {
                Long userId = budget.getUser().getId();
                Long totalExpense = userExpenseMap.getOrDefault(userId, 0L);
                Long budgetLimit = budget.getMonthlyLimit();

                if (totalExpense > budgetLimit) {
                    String title = "예산 초과 알림";
                    String message = String.format("이번 달 구독 지출(%,d원)이 설정한 예산(%,d원)을 %,d원 초과했습니다.",
                            totalExpense,
                            budgetLimit,
                            totalExpense - budgetLimit);

                    notificationService.createNotification(
                            userId,
                            NotificationType.BUDGET_EXCEEDED,
                            title,
                            message,
                            null
                    );

                    log.info("예산 초과 알림 생성 - userId: {}, 예산: {}, 지출: {}", userId, budgetLimit, totalExpense);
                }
            } catch (Exception e) {
                log.error("예산 초과 알림 생성 실패 - budgetId: {}", budget.getId(), e);
            }
        }

        log.info("예산 초과 알림 체크 완료");
    }

    // 매주 월요일 자정에 실행 (미사용 구독 감지 알림 체크)
    @Scheduled(cron = "0 0 0 * * MON")
    @PreAuthorize("permitAll()")
    public void checkUnusedSubscriptionNotifications() {
        log.info("미사용 구독 감지 알림 체크 시작");

        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.minusDays(90);

        // 90일 이상 지속된 활성 구독 조회
        List<UserSubscription> longTermSubscriptions = userSubscriptionRepository.findAllActiveWithServiceAndUser()
                .stream()
                .filter(subscription -> subscription.getCreatedAt().toLocalDate().isBefore(thresholdDate))
                .toList();

        for (UserSubscription subscription : longTermSubscriptions) {
            try {
                long daysActive = ChronoUnit.DAYS.between(subscription.getCreatedAt().toLocalDate(), today);

                String title = "장기 구독 확인 필요";
                String message = String.format("%s 구독을 %d일째 사용 중입니다. 계속 사용하시나요? 불필요한 구독은 해지하여 비용을 절약하세요.",
                        subscription.getService().getServiceName(),
                        daysActive);

                notificationService.createNotification(
                        subscription.getUser().getId(),
                        NotificationType.UNUSED_SUBSCRIPTION,
                        title,
                        message,
                        subscription.getId()
                );

                log.info("미사용 구독 감지 알림 생성 - subscriptionId: {}, 활성 기간: {}일", subscription.getId(), daysActive);
            } catch (Exception e) {
                log.error("미사용 구독 감지 알림 생성 실패 - subscriptionId: {}", subscription.getId(), e);
            }
        }

        log.info("미사용 구독 감지 알림 체크 완료");
    }

    // 매일 자정에 실행 (구독 갱신 알림 체크)
    @Scheduled(cron = "0 0 0 * * *")
    @PreAuthorize("permitAll()")
    public void checkSubscriptionRenewalNotifications() {
        log.info("구독 갱신 알림 체크 시작");

        List<UserSubscription> activeSubscriptions = userSubscriptionRepository.findAllActiveWithServiceAndUser();

        LocalDate today = LocalDate.now();
        int todayDay = today.getDayOfMonth();

        for (UserSubscription subscription : activeSubscriptions) {
            try {
                // 오늘이 결제일인 구독 찾기
                if (subscription.getBillingDate().equals(todayDay)) {
                    String title = "구독 갱신 완료";
                    String message = String.format("%s 구독이 오늘 갱신되었습니다. 결제 금액: %,d원",
                            subscription.getService().getServiceName(),
                            subscription.getMonthlyPrice());

                    notificationService.createNotification(
                            subscription.getUser().getId(),
                            NotificationType.SUBSCRIPTION_RENEWAL,
                            title,
                            message,
                            subscription.getId()
                    );

                    log.info("구독 갱신 알림 생성 - subscriptionId: {}, userId: {}",
                            subscription.getId(), subscription.getUser().getId());
                }
            } catch (Exception e) {
                log.error("구독 갱신 알림 생성 실패 - subscriptionId: {}", subscription.getId(), e);
            }
        }

        log.info("구독 갱신 알림 체크 완료");
    }
}
