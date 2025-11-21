package com.project.subing.domain.preference.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 성향 테스트 질문 옵션
 */
@Entity
@Table(name = "preference_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PreferenceOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private PreferenceQuestion question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String optionText; // 옵션 텍스트 (예: "1만원도 아까워!")

    @Column(length = 100)
    private String subtext; // 부제목 (예: "초절약형")

    @Column(length = 10)
    private String emoji; // 이모지 (예: "🪶")

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String scoreImpact; // 점수 영향 (JSON 형식)
    // 예시: {"contentScore": 25, "priceSensitivityScore": -5}

    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    private String categoryTags; // 카테고리 태그 (JSON 배열)
    // 예시: ["STREAMING", "VIDEO"]
}
