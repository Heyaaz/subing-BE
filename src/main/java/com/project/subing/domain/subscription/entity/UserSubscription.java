package com.project.subing.domain.subscription.entity;

import com.project.subing.domain.common.BillingCycle;
import com.project.subing.domain.common.Currency;
import com.project.subing.domain.common.SoftDeletableEntity;
import com.project.subing.domain.service.entity.ServiceEntity;
import com.project.subing.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@Table(name = "user_subscriptions")
@SQLDelete(sql = "UPDATE user_subscriptions SET del_yn = 'Y' WHERE id = ?")
@SQLRestriction("del_yn = 'N'")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserSubscription extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    @Column(length = 100)
    private String planName;

    @Column(nullable = false)
    private Integer monthlyPrice;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)  // nullable: 기존 행 호환, 없으면 KRW로 처리
    @Builder.Default
    private Currency currency = Currency.KRW;

    @Column(nullable = false)
    private Integer billingDate;  // 1-31

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BillingCycle billingCycle = BillingCycle.MONTHLY;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** 구독 시작월 (년-월만 사용, 일은 1일로 저장). nullable: 기존 데이터 호환 */
    @Column(name = "started_at")
    private LocalDate startedAt;

    // 비즈니스 로직
    public LocalDate getNextBillingDate() {
        LocalDate today = LocalDate.now();
        int currentDay = today.getDayOfMonth();

        LocalDate targetMonth = (currentDay < this.billingDate) ? today : today.plusMonths(1);
        int safeDay = Math.min(this.billingDate, targetMonth.lengthOfMonth());

        LocalDate nextBilling = targetMonth.withDayOfMonth(safeDay);

        // 연간 결제인 경우 1년 추가
        if (this.billingCycle == BillingCycle.YEARLY) {
            nextBilling = nextBilling.plusYears(1);
        }

        return nextBilling;
    }

    public void updatePrice(Integer newPrice) {
        this.monthlyPrice = newPrice;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public void setBillingDate(Integer billingDate) {
        this.billingDate = billingDate;
    }

    public void setBillingCycle(BillingCycle billingCycle) {
        this.billingCycle = billingCycle;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setStartedAt(LocalDate startedAt) {
        this.startedAt = startedAt;
    }

    public void cancel() {
        this.isActive = false;
    }

    public void reactivate() {
        this.isActive = true;
    }
}
