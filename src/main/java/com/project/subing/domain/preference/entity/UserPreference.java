package com.project.subing.domain.preference.entity;

import com.project.subing.domain.common.SoftDeletableEntity;
import com.project.subing.domain.preference.enums.ProfileType;
import com.project.subing.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

/**
 * 사용자 성향 프로필 (테스트 결과)
 */
@Entity
@Table(name = "user_preferences")
@SQLDelete(sql = "UPDATE user_preferences SET del_yn = 'Y' WHERE id = ?")
@SQLRestriction("del_yn = 'N'")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserPreference extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProfileType profileType; // 프로필 타입 (8가지 중 1개)

    // 점수 (0~100)
    @Column(nullable = false)
    private Integer contentScore; // 콘텐츠 소비 점수

    @Column(nullable = false)
    private Integer priceSensitivityScore; // 가성비 선호 점수

    @Column(nullable = false)
    private Integer healthScore; // 건강 관심 점수

    @Column(nullable = false)
    private Integer selfDevelopmentScore; // 자기계발 점수

    @Column(nullable = false)
    private Integer digitalToolScore; // 디지털 도구 활용 점수

    // 추가 정보
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private String interestedCategories; // 관심 카테고리 (JSON 배열)
    // 예시: ["스트리밍", "음악", "독서"]

    @Column(length = 50)
    private String budgetRange; // 예산 범위 (예: "월 1~3만원")

    /**
     * 점수 업데이트
     */
    public void updateScores(
        Integer contentScore,
        Integer priceSensitivityScore,
        Integer healthScore,
        Integer selfDevelopmentScore,
        Integer digitalToolScore
    ) {
        this.contentScore = contentScore;
        this.priceSensitivityScore = priceSensitivityScore;
        this.healthScore = healthScore;
        this.selfDevelopmentScore = selfDevelopmentScore;
        this.digitalToolScore = digitalToolScore;
    }

    /**
     * 프로필 타입 업데이트
     */
    public void updateProfileType(ProfileType profileType) {
        this.profileType = profileType;
    }

    /**
     * 추가 정보 업데이트
     */
    public void updateAdditionalInfo(String interestedCategories, String budgetRange) {
        this.interestedCategories = interestedCategories;
        this.budgetRange = budgetRange;
    }
}
