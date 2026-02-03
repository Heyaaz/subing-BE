package com.project.subing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.subing.domain.preference.entity.UserPreference;
import com.project.subing.domain.recommendation.entity.RecommendationClick;
import com.project.subing.domain.recommendation.entity.RecommendationFeedback;
import com.project.subing.domain.recommendation.entity.RecommendationResult;
import com.project.subing.domain.recommendation.enums.PromptVersion;
import com.project.subing.domain.service.entity.ServiceEntity;
import com.project.subing.domain.service.entity.SubscriptionPlan;
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
import com.project.subing.repository.SubscriptionPlanRepository;
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
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public RecommendationResponse getRecommendations(Long userId, QuizRequest quiz) {
        // 0-1. 사용자 성향 데이터 조회 (Optional)
        UserPreference userPreference = userPreferenceRepository.findByUserId(userId).orElse(null);

        // 0-2. A/B 테스트를 위한 랜덤 프롬프트 버전 선택
        PromptVersion promptVersion = PromptVersion.random();

        // 1. 캐시된 추천 결과 조회 (없으면 GPT API 호출)
        RecommendationResponse result = getRecommendationFromCache(userId, quiz, userPreference, promptVersion);

        // 2. 가격 정보 추가
        enrichPriceInfo(result);

        // 3. DB에 저장
        saveRecommendationResult(userId, quiz, result, promptVersion);

        return result;
    }

    @Cacheable(value = "gptRecommendations", key = "#userId + '_' + #quiz.hashCode() + '_' + (#userPreference != null ? #userPreference.id : 'no-pref') + '_' + #promptVersion.name()")
    public RecommendationResponse getRecommendationFromCache(Long userId, QuizRequest quiz, UserPreference userPreference, PromptVersion promptVersion) {
        // 1. 프롬프트 생성
        String prompt = buildPrompt(quiz, userPreference);
        System.out.println("[GPT 추천] 프롬프트에 서비스 ID 포함 여부 확인:");
        System.out.println(prompt.substring(0, Math.min(500, prompt.length())) + "...");

        // 2. GPT API 호출
        String response = callGPTAPI(prompt, promptVersion);
        System.out.println("[GPT 추천] GPT 응답 미리보기:");
        System.out.println(response.substring(0, Math.min(300, response.length())) + "...");

        // 3. JSON 파싱
        RecommendationResponse parsedResponse = parseResponse(response);
        System.out.println("[GPT 추천] 파싱된 추천 개수: " + parsedResponse.getRecommendations().size());
        parsedResponse.getRecommendations().forEach(item ->
            System.out.println("[GPT 추천] - serviceId: " + item.getServiceId() + ", serviceName: " + item.getServiceName())
        );

        return parsedResponse;
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
                System.out.println("[GPT 스트리밍] 프롬프트에 서비스 ID 포함 여부 확인:");
                System.out.println(prompt.substring(0, Math.min(500, prompt.length())) + "...");
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
                            // 완료 시 - Reactor 스레드에서 blocking 작업(chatModel.call) 방지 위해 별도 스레드 사용
                            String responseText = fullResponse.toString();
                            System.out.println("[스트리밍 완료] 전체 응답 길이: " + responseText.length());

                            executorService.execute(() -> {
                                try {
                                    // 한글 띄어쓰기 보정 (blocking call이므로 executor 스레드에서 실행)
                                    String correctedJson = fixKoreanSpacing(responseText);

                                    // DB에 저장하기 위해 파싱
                                    RecommendationResponse parsedResponse = parseResponse(correctedJson);
                                    System.out.println("[GPT 스트리밍] 파싱된 추천 개수: " + parsedResponse.getRecommendations().size());
                                    parsedResponse.getRecommendations().forEach(item ->
                                        System.out.println("[GPT 스트리밍] - serviceId: " + item.getServiceId() + ", serviceName: " + item.getServiceName())
                                    );

                                    // 가격 정보 추가
                                    enrichPriceInfo(parsedResponse);

                                    // 가격 정보가 추가된 결과를 다시 JSON으로 변환
                                    String enrichedJson = objectMapper.writeValueAsString(parsedResponse);

                                    // 보정된 결과를 result 이벤트로 전송 (단일 라인 JSON)
                                    System.out.println("[SSE] result 이벤트 전송 시작");
                                    emitter.send(SseEmitter.event()
                                            .name("result")
                                            .data(enrichedJson));
                                    System.out.println("[SSE] result 이벤트 전송 완료");

                                    // 완료 이벤트 전송
                                    emitter.send(SseEmitter.event()
                                            .name("done")
                                            .data("complete"));

                                    // DB에 저장
                                    saveRecommendationResult(userId, quiz, parsedResponse, promptVersion);

                                    emitter.complete();
                                } catch (Exception e) {
                                    System.err.println("[스트리밍 완료 에러]: " + e.getMessage());
                                    e.printStackTrace();
                                    emitter.completeWithError(e);
                                }
                            });
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
            serviceList.append(String.format("%d. [ID: %d] %s - %s - %s\n",
                    i + 1,
                    service.getId(),
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
            한글 문장(mainReason, pros, cons, tip, summary, alternatives)은 올바른 띄어쓰기를 적용하세요.

            출력 형식:
            {
              "recommendations": [
                {
                  "serviceId": 숫자 (필수: 위 서비스 목록의 [ID: X] 값을 정확히 사용),
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

            중요: serviceId는 반드시 위 서비스 목록의 [ID: X] 값을 정확하게 사용하세요.

            한글 띄어쓰기 규칙 (반드시 준수):
            ❌ 잘못된 예: "구글드라이브는클라우드저장소와파일공유가매우유용하며업무효율성을높일수있습니다"
            ✅ 올바른 예: "구글드라이브는 클라우드 저장소와 파일 공유가 매우 유용하며 업무 효율성을 높일 수 있습니다"
            모든 한글 값(mainReason, pros, cons, tip, summary, alternatives)에 적용하세요.
            """,
                String.join(", ", quiz.getInterests()),
                quiz.getBudget(),
                quiz.getPurposeLabel(),
                quiz.getPrioritiesFormatted(),
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

    /**
     * ChatModel을 활용한 한글 띄어쓰기 보정.
     * GPT가 JSON 내 한글 값에서 띄어쓰기를 빠뜨리는 경우를 수정.
     */
    private String fixKoreanSpacing(String jsonText) {
        try {
            System.out.println("[fixKoreanSpacing] 입력 텍스트 길이: " + jsonText.length());

            List<Message> messages = List.of(
                    new SystemMessage("당신은 한국어 텍스트의 띄어쓰기를 수정하는 전문가입니다. " +
                            "입력받은 JSON의 한글 텍스트 값들에만 올바른 띄어쓰기를 추가하여 반환해주세요. " +
                            "JSON 구조, 키, 숫자 값은 변경하지 마세요. " +
                            "중요: JSON을 한 줄로 압축해서 반환하세요 (개행 문자 없이). JSON만 답하세요."),
                    new UserMessage(jsonText)
            );

            String corrected = chatModel.call(new Prompt(messages))
                    .getResult().getOutput().getText();

            // 코드펜스 제거
            corrected = corrected
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            // SSE 전송을 위해 단일 라인으로 변환 (개행 문자 제거)
            corrected = corrected.replace("\n", "").replace("\r", "");

            System.out.println("[fixKoreanSpacing] 보정 완료, 길이: " + corrected.length());
            System.out.println("[fixKoreanSpacing] 미리보기: " + corrected.substring(0, Math.min(200, corrected.length())) + "...");

            return corrected;
        } catch (Exception e) {
            System.err.println("[fixKoreanSpacing] 보정 실패: " + e.getMessage());
            e.printStackTrace();
            // 보정 실패 시 원본 반환 (개행 제거는 필수)
            return jsonText.replace("\n", "").replace("\r", "");
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

    /**
     * 추천 결과에 가격 정보 추가
     */
    private void enrichPriceInfo(RecommendationResponse response) {
        if (response.getRecommendations() == null) {
            return;
        }

        for (com.project.subing.dto.recommendation.RecommendationItem item : response.getRecommendations()) {
            if (item.getServiceId() == null) {
                continue;
            }

            try {
                // 서비스의 모든 플랜 조회
                List<SubscriptionPlan> plans = subscriptionPlanRepository.findByServiceId(item.getServiceId());

                if (plans.isEmpty()) {
                    continue;
                }

                // 무료 플랜 여부 확인
                boolean hasFreePlan = plans.stream()
                    .anyMatch(plan -> plan.getMonthlyPrice() != null && plan.getMonthlyPrice() == 0);

                // 최저가 조회 (무료 제외)
                Integer minPrice = plans.stream()
                    .filter(plan -> plan.getMonthlyPrice() != null && plan.getMonthlyPrice() > 0)
                    .mapToInt(SubscriptionPlan::getMonthlyPrice)
                    .min()
                    .orElse(0);

                // 최고가 조회 (무료 제외)
                Integer maxPrice = plans.stream()
                    .filter(plan -> plan.getMonthlyPrice() != null && plan.getMonthlyPrice() > 0)
                    .mapToInt(SubscriptionPlan::getMonthlyPrice)
                    .max()
                    .orElse(0);

                // 가격 범위 문자열 생성
                String priceRange;
                if (hasFreePlan && minPrice > 0) {
                    priceRange = String.format("무료 ~ ₩%,d/월", maxPrice);
                } else if (hasFreePlan) {
                    priceRange = "무료";
                } else if (minPrice == maxPrice) {
                    priceRange = String.format("₩%,d/월", minPrice);
                } else {
                    priceRange = String.format("₩%,d ~ ₩%,d/월", minPrice, maxPrice);
                }

                // DTO에 설정
                item.setMinPrice(minPrice > 0 ? minPrice : null);
                item.setHasFreePlan(hasFreePlan);
                item.setPriceRange(priceRange);

                System.out.println(String.format(
                    "[가격 정보] %s - 최저가: %s원, 무료 플랜: %s, 범위: %s",
                    item.getServiceName(),
                    minPrice > 0 ? String.format("%,d", minPrice) : "없음",
                    hasFreePlan ? "있음" : "없음",
                    priceRange
                ));

            } catch (Exception e) {
                System.err.println("[가격 정보 조회 실패] " + item.getServiceName() + ": " + e.getMessage());
            }
        }
    }
}
