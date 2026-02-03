package com.project.subing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.subing.domain.preference.entity.UserPreference;
import com.project.subing.domain.recommendation.entity.RecommendationClick;
import com.project.subing.domain.recommendation.entity.RecommendationFeedback;
import com.project.subing.domain.recommendation.entity.RecommendationResult;
import com.project.subing.domain.recommendation.enums.PromptVersion;
import com.project.subing.domain.service.entity.ServiceEntity;
import com.project.subing.domain.user.entity.User;
import com.project.subing.dto.recommendation.QuizRequest;
import com.project.subing.dto.recommendation.RecommendationResponse;
import com.project.subing.exception.auth.UnauthorizedAccessException;
import com.project.subing.exception.entity.RecommendationNotFoundException;
import com.project.subing.exception.entity.ServiceNotFoundException;
import com.project.subing.exception.entity.UserNotFoundException;
import com.project.subing.exception.external.GptApiException;
import com.project.subing.exception.external.GptParsingException;
import com.project.subing.exception.external.RecommendationSaveException;
import com.project.subing.repository.RecommendationClickRepository;
import com.project.subing.repository.RecommendationFeedbackRepository;
import com.project.subing.repository.RecommendationResultRepository;
import com.project.subing.repository.ServiceRepository;
import com.project.subing.repository.UserPreferenceRepository;
import com.project.subing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Transactional
public class GPTRecommendationService {

    private final ChatModel chatModel;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final RecommendationResultRepository recommendationResultRepository;
    private final RecommendationFeedbackRepository recommendationFeedbackRepository;
    private final RecommendationClickRepository recommendationClickRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public RecommendationResponse getRecommendations(Long userId, QuizRequest quiz) {
        // 0-1. 사용자 성향 데이터 조회 (Optional)
        UserPreference userPreference = userPreferenceRepository.findByUserId(userId).orElse(null);

        // 0-2. A/B 테스트를 위한 랜덤 프롬프트 버전 선택
        PromptVersion promptVersion = PromptVersion.random();

        // 1. 캐시된 추천 결과 조회 (없으면 GPT API 호출)
        RecommendationResponse result = getRecommendationFromCache(userId, quiz, userPreference, promptVersion);

        // 2. DB에 저장
        saveRecommendationResult(userId, quiz, result, promptVersion);

        return result;
    }

    @Cacheable(value = "gptRecommendations", key = "#userId + '_' + #quiz.hashCode() + '_' + (#userPreference != null ? #userPreference.id : 'no-pref') + '_' + #promptVersion.name()")
    public RecommendationResponse getRecommendationFromCache(Long userId, QuizRequest quiz, UserPreference userPreference, PromptVersion promptVersion) {
        // 1. 프롬프트 생성
        String prompt = buildPrompt(quiz, userPreference);

        // 2. GPT API 호출
        String response = callGPTAPI(prompt, promptVersion);

        // 3. JSON 파싱
        return parseResponse(response);
    }

    /**
     * SSE를 통한 스트리밍 추천 (실시간 타이핑 효과)
     */
    public SseEmitter getRecommendationsStream(Long userId, QuizRequest quiz) {
        // SSE Emitter 생성 (타임아웃 5분)
        SseEmitter emitter = new SseEmitter(300000L);

        // 비동기 처리
        executorService.execute(() -> {
            try {
                // 1. 사용자 성향 데이터 조회
                UserPreference userPreference = userPreferenceRepository.findByUserId(userId).orElse(null);

                // 2. 프롬프트 버전 선택
                PromptVersion promptVersion = PromptVersion.random();

                // 3. 프롬프트 생성
                String prompt = buildPrompt(quiz, userPreference);
                String systemPrompt = promptVersion.getSystemPrompt();

                List<Message> messages = List.of(
                        new SystemMessage(systemPrompt),
                        new UserMessage(prompt)
                );

                Prompt gptPrompt = new Prompt(messages);

                // 4. GPT 스트리밍 호출
                Flux<String> streamFlux = chatModel.stream(gptPrompt)
                        .map(chatResponse -> {
                            if (chatResponse.getResult() != null &&
                                chatResponse.getResult().getOutput() != null &&
                                chatResponse.getResult().getOutput().getText() != null) {
                                return chatResponse.getResult().getOutput().getText();
                            }
                            return "";
                        });

                // 5. 전체 응답 누적용
                StringBuilder fullResponse = new StringBuilder();

                // 6. 각 청크를 SSE로 전송
                streamFlux.subscribe(
                        chunk -> {
                            try {
                                if (chunk != null && !chunk.isEmpty()) {
                                    fullResponse.append(chunk);
                                    emitter.send(SseEmitter.event()
                                            .name("message")
                                            .data(chunk));
                                }
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            // 에러 발생 시
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data("GPT API 호출 실패: " + error.getMessage()));
                            } catch (IOException e) {
                                // ignore
                            }
                            emitter.completeWithError(error);
                        },
                        () -> {
                            // 완료 시
                            try {
                                // 완료 이벤트 전송
                                emitter.send(SseEmitter.event()
                                        .name("done")
                                        .data("complete"));

                                // DB에 저장 (비동기)
                                String responseText = fullResponse.toString();
                                RecommendationResponse parsedResponse = parseResponse(responseText);
                                saveRecommendationResult(userId, quiz, parsedResponse, promptVersion);

                                emitter.complete();
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        }
                );

            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("추천 생성 실패: " + e.getMessage()));
                } catch (IOException ioException) {
                    // ignore
                }
                emitter.completeWithError(e);
            }
        });

        // 타임아웃 및 에러 핸들러
        emitter.onTimeout(() -> {
            emitter.complete();
        });

        emitter.onError((error) -> {
            emitter.complete();
        });

        return emitter;
    }

    @Transactional(readOnly = true)
    public List<RecommendationResult> getRecommendationHistory(Long userId) {
        return recommendationResultRepository.findTop5ByUser_IdOrderByCreatedAtDesc(userId);
    }

    public void saveFeedback(Long recommendationId, Long userId, Boolean isHelpful, String comment) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        RecommendationResult recommendationResult = recommendationResultRepository.findById(recommendationId)
                .orElseThrow(() -> new RecommendationNotFoundException(recommendationId));

        if (!recommendationResult.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("해당 추천 결과에 대한 피드백 권한이 없습니다.");
        }

        // 이미 피드백을 남긴 경우 업데이트
        RecommendationFeedback feedback = recommendationFeedbackRepository
                .findByRecommendationResult_IdAndUser_Id(recommendationId, userId)
                .orElse(null);

        if (feedback == null) {
            feedback = RecommendationFeedback.builder()
                    .recommendationResult(recommendationResult)
                    .user(user)
                    .isHelpful(isHelpful)
                    .comment(comment)
                    .build();
        }

        recommendationFeedbackRepository.save(feedback);
    }

    /**
     * 추천 결과 클릭 추적
     * (프론트엔드에서 사용자가 추천 서비스를 클릭했을 때 호출)
     */
    public void trackClick(Long recommendationId, Long userId, Long serviceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        RecommendationResult recommendationResult = recommendationResultRepository.findById(recommendationId)
                .orElseThrow(() -> new RecommendationNotFoundException(recommendationId));

        if (!recommendationResult.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("해당 추천 결과에 대한 클릭 기록 권한이 없습니다.");
        }

        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServiceNotFoundException(serviceId));

        RecommendationClick click = RecommendationClick.builder()
                .recommendationResult(recommendationResult)
                .user(user)
                .service(service)
                .build();

        recommendationClickRepository.save(click);
    }

    private String buildPrompt(QuizRequest quiz, UserPreference userPreference) {
        List<ServiceEntity> services = serviceRepository.findAll();

        StringBuilder serviceList = new StringBuilder();
        for (int i = 0; i < services.size(); i++) {
            ServiceEntity service = services.get(i);
            serviceList.append(String.format("%d. %s - %s - %s\n",
                    i + 1,
                    service.getServiceName(),
                    service.getCategory(),
                    service.getDescription() != null ? service.getDescription() : "서비스 설명 없음"
            ));
        }

        // 성향 데이터 섹션 생성
        StringBuilder preferenceSection = new StringBuilder();
        if (userPreference != null) {
            preferenceSection.append("\n사용자 성향 프로필:\n");
            preferenceSection.append(String.format("- 프로필 타입: %s (%s)\n",
                userPreference.getProfileType().name(),
                userPreference.getProfileType().getDisplayName()));
            preferenceSection.append(String.format("- 콘텐츠 소비 점수: %d/100\n", userPreference.getContentScore()));
            preferenceSection.append(String.format("- 가성비 선호 점수: %d/100\n", userPreference.getPriceSensitivityScore()));
            preferenceSection.append(String.format("- 건강 관심 점수: %d/100\n", userPreference.getHealthScore()));
            preferenceSection.append(String.format("- 자기계발 점수: %d/100\n", userPreference.getSelfDevelopmentScore()));
            preferenceSection.append(String.format("- 디지털 도구 점수: %d/100\n", userPreference.getDigitalToolScore()));

            if (userPreference.getInterestedCategories() != null) {
                preferenceSection.append(String.format("- 관심 카테고리: %s\n", userPreference.getInterestedCategories()));
            }
            if (userPreference.getBudgetRange() != null) {
                preferenceSection.append(String.format("- 예산 범위: %s\n", userPreference.getBudgetRange()));
            }

            preferenceSection.append("\n위 성향 데이터를 적극 활용하여 사용자에게 가장 적합한 서비스를 추천해주세요.\n");
        }

        return String.format("""
            사용자 입력:
            - 관심 분야: %s
            - 월 예산: %,d원
            - 사용 목적: %s
            - 중요도: %s
            %s
            사용 가능한 서비스 목록:
            %s

            상위 3-5개 서비스를 JSON 형식으로 추천해주세요.

            중요: JSON만 응답하세요. 마크다운 코드블록(```)이나 설명 텍스트를 포함하지 마세요.
            반드시 아래 출력 형식을 따라주세요.

            출력 형식:
            {
              "recommendations": [
                {
                  "serviceId": 숫자,
                  "serviceName": "서비스명",
                  "score": 0-100 점수,
                  "mainReason": "추천 이유",
                  "pros": ["장점1", "장점2", "장점3"],
                  "cons": ["단점1", "단점2"],
                  "tip": "실용적인 팁"
                }
              ],
              "summary": "전체 요약",
              "alternatives": "대안 제안"
            }
            """,
                String.join(", ", quiz.getInterests()),
                quiz.getBudget(),
                quiz.getPurpose(),
                String.join(", ", quiz.getPriorities()),
                preferenceSection.toString(),
                serviceList.toString()
        );
    }

    private String callGPTAPI(String prompt, PromptVersion promptVersion) {
        // 프롬프트 버전에 따라 시스템 프롬프트 선택
        String systemPrompt = promptVersion.getSystemPrompt();

        try {
            // Spring AI ChatModel을 사용한 API 호출
            List<Message> messages = List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(prompt)
            );

            Prompt gptPrompt = new Prompt(messages);
            return chatModel.call(gptPrompt)
                    .getResult()
                    .getOutput()
                    .getText();

        } catch (Exception e) {
            throw new GptApiException(e);
        }
    }

    private RecommendationResponse parseResponse(String jsonContent) {
        try {
            // 마크다운 코드펜스 제거 (방어적 파싱)
            String cleaned = jsonContent
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
            return objectMapper.readValue(cleaned, RecommendationResponse.class);
        } catch (Exception e) {
            throw new GptParsingException(e);
        }
    }

    private void saveRecommendationResult(Long userId, QuizRequest quiz, RecommendationResponse result, PromptVersion promptVersion) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId));

            String quizJson = objectMapper.writeValueAsString(quiz);
            String resultJson = objectMapper.writeValueAsString(result);

            RecommendationResult entity = RecommendationResult.builder()
                    .user(user)
                    .quizData(quizJson)
                    .resultData(resultJson)
                    .promptVersion(promptVersion) // A/B 테스트용 프롬프트 버전 저장
                    .build();

            recommendationResultRepository.save(entity);
        } catch (UserNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RecommendationSaveException(e);
        }
    }
}
