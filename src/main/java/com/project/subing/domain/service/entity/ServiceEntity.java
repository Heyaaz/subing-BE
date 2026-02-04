package com.project.subing.domain.service.entity;

import com.project.subing.domain.common.ServiceCategory;
import com.project.subing.domain.common.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "services")
@SQLDelete(sql = "UPDATE services SET del_yn = 'Y' WHERE id = ?")
@SQLRestriction("del_yn = 'N'")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ServiceEntity extends SoftDeletableEntity {

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
