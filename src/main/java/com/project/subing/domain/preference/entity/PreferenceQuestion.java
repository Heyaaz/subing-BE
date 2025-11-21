package com.project.subing.domain.preference.entity;

import com.project.subing.domain.preference.enums.QuestionCategory;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 성향 테스트 질문
 */
@Entity
@Table(name = "preference_questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PreferenceQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private QuestionCategory category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(length = 10)
    private String emoji;

    @Column(nullable = false)
    private Integer orderIndex; // 질문 순서 (1~12)

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PreferenceOption> options = new ArrayList<>();
}
