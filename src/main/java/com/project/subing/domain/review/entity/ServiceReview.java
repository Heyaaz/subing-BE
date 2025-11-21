package com.project.subing.domain.review.entity;

import com.project.subing.domain.service.entity.ServiceEntity;
import com.project.subing.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "service_reviews",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "service_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ServiceReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    @Column(nullable = false)
    private Integer rating; // 1-5

    @Column(columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 비즈니스 로직
    public void updateReview(Integer rating, String content) {
        if (rating != null && rating >= 1 && rating <= 5) {
            this.rating = rating;
        }
        if (content != null) {
            this.content = content;
        }
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }
}