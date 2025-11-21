package com.project.subing.controller;

import com.project.subing.domain.preference.entity.PreferenceQuestion;
import com.project.subing.domain.preference.entity.UserPreference;
import com.project.subing.dto.common.ApiResponse;
import com.project.subing.dto.preference.PreferenceQuestionResponse;
import com.project.subing.dto.preference.SubmitAnswersRequest;
import com.project.subing.dto.preference.UserPreferenceResponse;
import com.project.subing.service.PreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
public class PreferenceController {

    private final PreferenceService preferenceService;

    /**
     * GET /api/v1/preferences/questions
     * 질문 목록 조회
     */
    @GetMapping("/questions")
    public ResponseEntity<ApiResponse<List<PreferenceQuestionResponse>>> getQuestions() {
        List<PreferenceQuestion> questions = preferenceService.getAllQuestions();
        List<PreferenceQuestionResponse> responses = questions.stream()
            .map(PreferenceQuestionResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(
            ApiResponse.success(responses, "질문 목록을 성공적으로 조회했습니다.")
        );
    }

    /**
     * POST /api/v1/preferences/submit
     * 답변 제출 및 분석
     */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<UserPreferenceResponse>> submitAnswers(
        @RequestParam Long userId,
        @RequestBody SubmitAnswersRequest request
    ) {
        UserPreference userPreference = preferenceService.submitAnswers(userId, request.getAnswers());
        UserPreferenceResponse response = UserPreferenceResponse.from(userPreference);

        return ResponseEntity.ok(
            ApiResponse.success(response, "성향 분석이 완료되었습니다.")
        );
    }

    /**
     * GET /api/v1/preferences/profile
     * 내 성향 프로필 조회
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserPreferenceResponse>> getProfile(
        @RequestParam Long userId
    ) {
        UserPreference userPreference = preferenceService.getUserPreference(userId)
            .orElseThrow(() -> new IllegalArgumentException("성향 프로필을 찾을 수 없습니다. 먼저 테스트를 완료해주세요."));

        UserPreferenceResponse response = UserPreferenceResponse.from(userPreference);

        return ResponseEntity.ok(
            ApiResponse.success(response, "성향 프로필을 성공적으로 조회했습니다.")
        );
    }

    /**
     * DELETE /api/v1/preferences/profile
     * 성향 프로필 삭제 (재검사 준비)
     */
    @DeleteMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> deleteProfile(
        @RequestParam Long userId
    ) {
        preferenceService.deleteUserPreference(userId);

        return ResponseEntity.ok(
            ApiResponse.success(null, "성향 프로필이 삭제되었습니다. 재검사를 진행할 수 있습니다.")
        );
    }
}
