package com.project.subing.domain.subscription.entity;

import com.project.subing.domain.common.BillingCycle;
import com.project.subing.domain.service.entity.ServiceEntity;
import com.project.subing.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserSubscription {
    
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
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // 비즈니스 로직
    public LocalDate getNextBillingDate() {
        LocalDate today = LocalDate.now();
        int currentDay = today.getDayOfMonth();
        
        LocalDate nextBilling;
        if (currentDay < this.billingDate) {
            // 이번 달에 결제일이 남아있음
            nextBilling = today.withDayOfMonth(this.billingDate);
        } else {
            // 다음 달 결제일
            nextBilling = today.plusMonths(1).withDayOfMonth(this.billingDate);
        }
        
        // 연간 결제인 경우 1년 추가
        if (this.billingCycle == BillingCycle.YEARLY) {
            nextBilling = nextBilling.plusYears(1);
        }
        
        return nextBilling;
    }
    
    public void updatePrice(Integer newPrice) {
        this.monthlyPrice = newPrice;
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
    
    public void cancel() {
        this.isActive = false;
    }
    
    public void reactivate() {
        this.isActive = true;
    }
}
