package com.project.subing.domain.recommendation.entity;

import com.project.subing.domain.recommendation.enums.PromptVersion;
import com.project.subing.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RecommendationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "quiz_data", columnDefinition = "TEXT", nullable = false)
    private String quizData;

    @Column(name = "result_data", columnDefinition = "TEXT", nullable = false)
    private String resultData;

    @Enumerated(EnumType.STRING)
    @Column(name = "prompt_version", length = 20)
    private PromptVersion promptVersion; // A/B 테스트용 프롬프트 버전

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
