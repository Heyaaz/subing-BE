package com.project.subing.domain.service.entity;

import com.project.subing.domain.common.ServiceCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "services")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ServiceEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String serviceName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ServiceCategory category;
    
    @Column(length = 255)
    private String iconUrl;
    
    @Column(length = 255)
    private String officialUrl;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // 비즈니스 로직
    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public void updateInfo(String serviceName, ServiceCategory category, String iconUrl,
                          String officialUrl, String description, Boolean isActive) {
        if (serviceName != null) {
            this.serviceName = serviceName;
        }
        if (category != null) {
            this.category = category;
        }
        if (iconUrl != null) {
            this.iconUrl = iconUrl;
        }
        if (officialUrl != null) {
            this.officialUrl = officialUrl;
        }
        if (description != null) {
            this.description = description;
        }
        if (isActive != null) {
            this.isActive = isActive;
        }
    }
}
