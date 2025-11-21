package com.project.subing.domain.service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_plans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SubscriptionPlan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;
    
    @Column(nullable = false, length = 100)
    private String planName;
    
    @Column(nullable = false)
    private Integer monthlyPrice;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String features;  // JSON 형태로 저장
    
    @Column
    @Builder.Default
    private Boolean isPopular = false;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // 비즈니스 로직
    public void updatePrice(Integer newPrice) {
        this.monthlyPrice = newPrice;
    }

    public void updateInfo(String planName, Integer monthlyPrice, String description,
                          String features, Boolean isPopular) {
        if (planName != null) {
            this.planName = planName;
        }
        if (monthlyPrice != null) {
            this.monthlyPrice = monthlyPrice;
        }
        if (description != null) {
            this.description = description;
        }
        if (features != null) {
            this.features = features;
        }
        if (isPopular != null) {
            this.isPopular = isPopular;
        }
    }
}
