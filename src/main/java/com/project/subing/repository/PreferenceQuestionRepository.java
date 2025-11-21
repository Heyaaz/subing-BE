package com.project.subing.repository;

import com.project.subing.domain.preference.entity.PreferenceQuestion;
import com.project.subing.domain.preference.enums.QuestionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PreferenceQuestionRepository extends JpaRepository<PreferenceQuestion, Long> {

    /**
     * 카테고리별 질문 조회
     */
    List<PreferenceQuestion> findByCategory(QuestionCategory category);

    /**
     * 순서대로 정렬된 모든 질문 조회
     */
    List<PreferenceQuestion> findAllByOrderByOrderIndexAsc();
}
