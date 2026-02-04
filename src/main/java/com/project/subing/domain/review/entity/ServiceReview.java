package com.project.subing.domain.review.entity;

import com.project.subing.domain.common.SoftDeletableEntity;
import com.project.subing.domain.service.entity.ServiceEntity;
import com.project.subing.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
    name = "service_reviews",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "service_id"})
    }
)
@SQLDelete(sql = "UPDATE service_reviews SET del_yn = 'Y' WHERE id = ?")
@SQLRestriction("del_yn = 'N'")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ServiceReview extends SoftDeletableEntity {

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
