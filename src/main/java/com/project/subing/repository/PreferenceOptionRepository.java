package com.project.subing.repository;

import com.project.subing.domain.preference.entity.PreferenceOption;
import com.project.subing.domain.preference.entity.PreferenceQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PreferenceOptionRepository extends JpaRepository<PreferenceOption, Long> {

    /**
     * 특정 질문의 옵션 목록 조회
     */
    List<PreferenceOption> findByQuestion(PreferenceQuestion question);

    /**
     * 특정 질문 ID의 옵션 목록 조회
     */
    List<PreferenceOption> findByQuestionId(Long questionId);
}
