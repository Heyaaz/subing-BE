package com.project.subing.config;

import com.project.subing.domain.preference.entity.PreferenceOption;
import com.project.subing.domain.preference.entity.PreferenceQuestion;
import com.project.subing.domain.preference.enums.QuestionCategory;
import com.project.subing.repository.PreferenceQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 성향 테스트 초기 데이터 로더
 * 애플리케이션 시작 시 12개 질문 + 48개 옵션 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")  // 테스트 환경에서는 실행하지 않음
public class PreferenceDataLoader implements ApplicationRunner {

    private final PreferenceQuestionRepository questionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 이미 데이터가 있으면 스킵
        if (questionRepository.count() > 0) {
            log.info("성향 테스트 데이터가 이미 존재합니다. 스킵합니다.");
            return;
        }

        log.info("성향 테스트 초기 데이터 로딩 시작...");

        List<PreferenceQuestion> questions = createQuestions();
        questionRepository.saveAll(questions);

        log.info("성향 테스트 초기 데이터 로딩 완료! ({}개 질문, {}개 옵션)",
            questions.size(), questions.stream().mapToInt(q -> q.getOptions().size()).sum());
    }

    private List<PreferenceQuestion> createQuestions() {
        List<PreferenceQuestion> questions = new ArrayList<>();

        // Q1: 디지털 월세 예산
        questions.add(createQuestion(1, QuestionCategory.BUDGET,
            "한 달 디지털 월세(구독료)로 얼마까지 쓸 수 있어?", "💰",
            List.of(
                createOption("1만원도 아까워!", "초절약형", "🪶",
                    "{\"priceSensitivityScore\": 40, \"budgetRange\": \"LOW\"}", "[\"BUDGET_CONSCIOUS\"]"),
                createOption("2~3만원 정도면 적당해", "알뜰형", "💵",
                    "{\"priceSensitivityScore\": 25, \"budgetRange\": \"MEDIUM\"}", "[\"MODERATE_SPENDER\"]"),
                createOption("5만원까지는 괜찮아", "여유형", "💳",
                    "{\"priceSensitivityScore\": -10, \"budgetRange\": \"HIGH\"}", "[\"FLEXIBLE_BUDGET\"]"),
                createOption("돈? 가치있으면 상관없어!", "프리미엄형", "💎",
                    "{\"priceSensitivityScore\": -30, \"budgetRange\": \"PREMIUM\"}", "[\"PREMIUM_SEEKER\"]")
            )
        ));

        // Q2: 무료 체험
        questions.add(createQuestion(2, QuestionCategory.BUDGET,
            "무료 체험 끝나면 나는?", "🎁",
            List.of(
                createOption("바로 해지! 다음 무료 체험 찾기", "무료 체험 헌터", "🏹",
                    "{\"priceSensitivityScore\": 35}", "[\"TRIAL_HUNTER\"]"),
                createOption("좋으면 결제, 아니면 해지", "합리적 판단형", "🤔",
                    "{\"priceSensitivityScore\": 15}", "[\"RATIONAL_USER\"]"),
                createOption("귀찮아서 그냥 쓴다", "자동 결제형", "😴",
                    "{\"priceSensitivityScore\": -15}", "[\"AUTO_RENEW\"]"),
                createOption("무료 체험을 안 써봐서...", "유료 직행형", "💸",
                    "{\"priceSensitivityScore\": -25}", "[\"PREMIUM_DIRECT\"]")
            )
        ));

        // Q3: 심심할 때
        questions.add(createQuestion(3, QuestionCategory.CONTENT,
            "심심할 때 나는?", "🎬",
            List.of(
                createOption("넷플릭스/티빙 정주행 시작!", "드라마/영화 덕후", "🎬",
                    "{\"contentScore\": 30}", "[\"STREAMING\", \"VIDEO\"]"),
                createOption("유튜브 쇼츠 무한루프", "숏폼 중독", "📱",
                    "{\"contentScore\": 25}", "[\"SHORT_FORM\", \"VIDEO\"]"),
                createOption("음악 틀고 멍때리기", "음악 러버", "🎵",
                    "{\"contentScore\": 20}", "[\"MUSIC\", \"AUDIO\"]"),
                createOption("웹툰/책 보기", "독서가", "📚",
                    "{\"contentScore\": 20}", "[\"READING\", \"BOOKS\"]")
            )
        ));

        // Q4: 출퇴근/등하교 시간
        questions.add(createQuestion(4, QuestionCategory.CONTENT,
            "출퇴근/등하교 시간에는?", "🚇",
            List.of(
                createOption("넷플릭스 다운로드해서 보기", "영상형", "🍿",
                    "{\"contentScore\": 30}", "[\"STREAMING\", \"VIDEO\"]"),
                createOption("플레이리스트 틀기", "음악형", "🎧",
                    "{\"contentScore\": 25}", "[\"MUSIC\", \"AUDIO\"]"),
                createOption("팟캐스트/오디오북 듣기", "오디오형", "🎙️",
                    "{\"contentScore\": 20}", "[\"PODCAST\", \"AUDIO\"]"),
                createOption("밀리의서재/전자책 읽기", "독서형", "📖",
                    "{\"contentScore\": 20}", "[\"READING\", \"EBOOK\"]")
            )
        ));

        // Q5: 요즘 빠진 콘텐츠
        questions.add(createQuestion(5, QuestionCategory.CONTENT,
            "요즘 빠진 콘텐츠는?", "📺",
            List.of(
                createOption("넷플릭스/디즈니+ 오리지널", "해외 드라마/영화", "🌍",
                    "{\"contentScore\": 30}", "[\"STREAMING\", \"PREMIUM\"]"),
                createOption("유튜브 크리에이터 콘텐츠", "유튜브 팬", "▶️",
                    "{\"contentScore\": 25}", "[\"YOUTUBE\", \"FREE\"]"),
                createOption("멜론/스포티파이 플레이리스트", "음악 스트리밍", "🎶",
                    "{\"contentScore\": 25}", "[\"MUSIC\"]"),
                createOption("웹툰/웹소설", "웹콘텐츠", "📲",
                    "{\"contentScore\": 20}", "[\"WEBTOON\", \"READING\"]")
            )
        ));

        // Q6: 구독 서비스 개수
        questions.add(createQuestion(6, QuestionCategory.SUBSCRIPTION,
            "현재 쓰고 있는 구독 서비스는?", "📦",
            List.of(
                createOption("1~2개", "미니멀리스트", "🧘",
                    "{\"priceSensitivityScore\": 20}", "[\"MINIMAL\"]"),
                createOption("3~5개", "적당주의자", "⚖️",
                    "{\"priceSensitivityScore\": 0}", "[\"MODERATE\"]"),
                createOption("6~10개", "구독 애호가", "📦",
                    "{\"priceSensitivityScore\": -15, \"contentScore\": 15}", "[\"HEAVY_USER\"]"),
                createOption("10개 이상", "구독 덕후", "🏆",
                    "{\"priceSensitivityScore\": -30, \"contentScore\": 25}", "[\"COLLECTOR\"]")
            )
        ));

        // Q7: 구독 해지
        questions.add(createQuestion(7, QuestionCategory.SUBSCRIPTION,
            "구독 서비스 해지할 때 나는?", "✂️",
            List.of(
                createOption("안 쓰면 바로 해지", "철저 관리형", "✂️",
                    "{\"priceSensitivityScore\": 25}", "[\"STRICT_MANAGER\"]"),
                createOption("가끔 정리함", "보통 관리형", "📋",
                    "{\"priceSensitivityScore\": 10}", "[\"CASUAL_MANAGER\"]"),
                createOption("귀찮아서 안 함", "방치형", "🤷",
                    "{\"priceSensitivityScore\": -10}", "[\"LAZY_USER\"]"),
                createOption("해지가 뭐야? 처음 들어봄", "영구 구독형", "♾️",
                    "{\"priceSensitivityScore\": -25}", "[\"PERMANENT_SUBSCRIBER\"]")
            )
        ));

        // Q8: 건강 관리
        questions.add(createQuestion(8, QuestionCategory.HEALTH,
            "요즘 건강 관리는?", "💪",
            List.of(
                createOption("헬스/홈트 열심히!", "운동 러버", "💪",
                    "{\"healthScore\": 40}", "[\"FITNESS\", \"HEALTH\"]"),
                createOption("산책이나 가볍게", "건강 인식형", "🚶",
                    "{\"healthScore\": 25}", "[\"LIGHT_EXERCISE\"]"),
                createOption("생각만...", "관심형", "💭",
                    "{\"healthScore\": 10}", "[\"INTERESTED\"]"),
                createOption("나는 패스~", "무관심형", "🛋️",
                    "{\"healthScore\": 0}", "[]")
            )
        ));

        // Q9: 피트니스 앱
        questions.add(createQuestion(9, QuestionCategory.HEALTH,
            "다이어트 앱/피트니스 앱 써본 적 있어?", "📲",
            List.of(
                createOption("쓰고 있어! 유료 결제도 했어", "앱 활용형", "📲",
                    "{\"healthScore\": 35}", "[\"FITNESS_APP\", \"PREMIUM\"]"),
                createOption("무료로 써봤어", "체험형", "🆓",
                    "{\"healthScore\": 20}", "[\"FITNESS_APP\", \"FREE\"]"),
                createOption("다운만 받았어", "관심형", "📥",
                    "{\"healthScore\": 10}", "[]"),
                createOption("필요 없어", "불필요형", "❌",
                    "{\"healthScore\": 0}", "[]")
            )
        ));

        // Q10: 자기계발 투자
        questions.add(createQuestion(10, QuestionCategory.SELF_DEV,
            "자기계발에 돈 쓰는 편이야?", "📈",
            List.of(
                createOption("당연하지! 투자는 필수", "자기계발 러버", "📈",
                    "{\"selfDevelopmentScore\": 40}", "[\"LEARNING\", \"PREMIUM\"]"),
                createOption("필요하면 씀", "합리형", "💡",
                    "{\"selfDevelopmentScore\": 25}", "[\"LEARNING\"]"),
                createOption("무료 강의만 봄", "무료 러버", "🆓",
                    "{\"selfDevelopmentScore\": 15, \"priceSensitivityScore\": 10}", "[\"LEARNING\", \"FREE\"]"),
                createOption("별로 안 씀", "무관심형", "🙅",
                    "{\"selfDevelopmentScore\": 0}", "[]")
            )
        ));

        // Q11: 배우고 싶은 것
        questions.add(createQuestion(11, QuestionCategory.SELF_DEV,
            "요즘 배우고 싶은 거 있어?", "💻",
            List.of(
                createOption("코딩/디자인 같은 실무 스킬", "실용형", "💻",
                    "{\"selfDevelopmentScore\": 35, \"digitalToolScore\": 15}", "[\"PRACTICAL_SKILL\"]"),
                createOption("영어/일본어 같은 외국어", "언어형", "🗣️",
                    "{\"selfDevelopmentScore\": 30}", "[\"LANGUAGE\"]"),
                createOption("요리/베이킹 같은 취미", "취미형", "🍳",
                    "{\"selfDevelopmentScore\": 20}", "[\"HOBBY\"]"),
                createOption("딱히 없어", "무관심형", "😶",
                    "{\"selfDevelopmentScore\": 0}", "[]")
            )
        ));

        // Q12: 파일 저장
        questions.add(createQuestion(12, QuestionCategory.DIGITAL,
            "파일 저장은 어떻게 해?", "☁️",
            List.of(
                createOption("구글 드라이브/아이클라우드", "클라우드 애호가", "☁️",
                    "{\"digitalToolScore\": 40}", "[\"CLOUD\", \"GOOGLE\"]"),
                createOption("노션/드롭박스 쓰는 중", "생산성 도구 유저", "📊",
                    "{\"digitalToolScore\": 35, \"selfDevelopmentScore\": 10}", "[\"PRODUCTIVITY\", \"CLOUD\"]"),
                createOption("컴퓨터/휴대폰에 저장", "로컬 저장형", "💾",
                    "{\"digitalToolScore\": 10}", "[\"LOCAL\"]"),
                createOption("저장? 그냥 지우는데", "무저장형", "🗑️",
                    "{\"digitalToolScore\": 0}", "[]")
            )
        ));

        return questions;
    }

    private PreferenceQuestion createQuestion(
        int orderIndex,
        QuestionCategory category,
        String questionText,
        String emoji,
        List<PreferenceOption.PreferenceOptionBuilder> optionBuilders
    ) {
        PreferenceQuestion question = PreferenceQuestion.builder()
            .category(category)
            .questionText(questionText)
            .emoji(emoji)
            .orderIndex(orderIndex)
            .options(new ArrayList<>())
            .build();

        // 옵션 빌더를 사용해서 옵션 생성 및 질문 연결
        optionBuilders.forEach(builder -> {
            PreferenceOption option = builder.question(question).build();
            question.getOptions().add(option);
        });

        return question;
    }

    private PreferenceOption.PreferenceOptionBuilder createOption(
        String optionText,
        String subtext,
        String emoji,
        String scoreImpact,
        String categoryTags
    ) {
        return PreferenceOption.builder()
            .optionText(optionText)
            .subtext(subtext)
            .emoji(emoji)
            .scoreImpact(scoreImpact)
            .categoryTags(categoryTags);
    }
}
