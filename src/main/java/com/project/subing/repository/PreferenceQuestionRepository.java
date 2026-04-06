package com.project.subing.repository;

import com.project.subing.domain.preference.entity.PreferenceQuestion;
import com.project.subing.domain.preference.enums.QuestionCategory;
import org.springframework.data.jpa.repository.EntityGraph;
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
     * 옵션까지 함께 로드한 순서별 질문 조회
     */
    @EntityGraph(attributePaths = "options")
    List<PreferenceQuestion> findAllByOrderByOrderIndexAsc();
}
