package com.project.subing.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.subing.domain.common.BillingCycle;
import com.project.subing.domain.common.ServiceCategory;
import com.project.subing.domain.service.entity.ServiceEntity;
import com.project.subing.domain.service.entity.SubscriptionPlan;
import com.project.subing.domain.subscription.entity.UserSubscription;
import com.project.subing.domain.user.entity.User;
import com.project.subing.domain.user.entity.UserRole;
import com.project.subing.domain.user.entity.UserTier;
import com.project.subing.repository.ServiceRepository;
import com.project.subing.repository.SubscriptionPlanRepository;
import com.project.subing.repository.UserRepository;
import com.project.subing.repository.UserSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 최적화 API 통합 테스트.
 * 중복 서비스 감지, 다운그레이드 우선 정렬, 구독별 최대 절감 합산 로직을 검증.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class OptimizationControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Autowired
    private UserSubscriptionRepository userSubscriptionRepository;

    private Long testUserId;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/optimization";

        String email = "optimization-test-" + System.nanoTime() + "@example.com";
        User user = userRepository.save(User.builder()
                .name("최적화 테스트 사용자")
                .email(email)
                .password("password123!")
                .tier(UserTier.FREE)
                .role(UserRole.USER)
                .build());
        testUserId = user.getId();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Test-User-Id", String.valueOf(testUserId));
        return headers;
    }

    @Test
    void 구독_없으면_최적화_결과_비어있음() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/suggestions",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.path("success").asBoolean()).isTrue();

        JsonNode data = body.path("data");
        assertThat(data.path("duplicateServices")).isEmpty();
        assertThat(data.path("cheaperAlternatives")).isEmpty();
        assertThat(data.path("optimizedAlternatives")).isEmpty();
        assertThat(data.path("totalPotentialSavings").asInt()).isZero();
    }

    @Test
    void 같은_카테고리_2개이상_구독시_중복_감지() throws Exception {
        // given - OTT 카테고리에 Netflix, Disney+ 구독
        ServiceEntity netflix = serviceRepository.save(ServiceEntity.builder()
                .serviceName("Netflix-dup-" + System.nanoTime())
                .category(ServiceCategory.OTT)
                .description("스트리밍")
                .officialUrl("https://netflix.com")
                .iconUrl("icon.png")
                .build());

        ServiceEntity disney = serviceRepository.save(ServiceEntity.builder()
                .serviceName("Disney+-dup-" + System.nanoTime())
                .category(ServiceCategory.OTT)
                .description("스트리밍")
                .officialUrl("https://disneyplus.com")
                .iconUrl("icon.png")
                .build());

        User user = userRepository.findById(testUserId).orElseThrow();
        userSubscriptionRepository.save(UserSubscription.builder()
                .user(user).service(netflix)
                .planName("프리미엄").monthlyPrice(17000)
                .billingDate(15).billingCycle(BillingCycle.MONTHLY)
                .isActive(true).build());
        userSubscriptionRepository.save(UserSubscription.builder()
                .user(user).service(disney)
                .planName("스탠다드").monthlyPrice(9900)
                .billingDate(1).billingCycle(BillingCycle.MONTHLY)
                .isActive(true).build());

        // when
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/suggestions",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");

        assertThat(data.path("duplicateServices").size()).isGreaterThanOrEqualTo(1);
        JsonNode group = data.path("duplicateServices").get(0);
        assertThat(group.path("totalCost").asInt()).isEqualTo(17000 + 9900);
        assertThat(group.path("subscriptions").size()).isEqualTo(2);
    }

    @Test
    void 동일서비스_다운그레이드가_타서비스보다_우선_정렬() throws Exception {
        // given - Netflix 프리미엄(17000) 구독, Netflix 스탠다드(13500) 플랜 존재, 다른 OTT 서비스 저렴한 플랜도 존재
        ServiceEntity netflix = serviceRepository.save(ServiceEntity.builder()
                .serviceName("Netflix-sort-" + System.nanoTime())
                .category(ServiceCategory.OTT)
                .description("스트리밍")
                .officialUrl("https://netflix.com")
                .iconUrl("icon.png")
                .build());

        ServiceEntity wavve = serviceRepository.save(ServiceEntity.builder()
                .serviceName("Wavve-sort-" + System.nanoTime())
                .category(ServiceCategory.OTT)
                .description("스트리밍")
                .officialUrl("https://wavve.com")
                .iconUrl("icon.png")
                .build());

        // Netflix 플랜: 스탠다드 13500
        subscriptionPlanRepository.save(SubscriptionPlan.builder()
                .service(netflix).planName("스탠다드").monthlyPrice(13500)
                .description("HD").build());

        // Wavve 플랜: 베이직 7900 (더 저렴하지만 다른 서비스)
        subscriptionPlanRepository.save(SubscriptionPlan.builder()
                .service(wavve).planName("베이직").monthlyPrice(7900)
                .description("SD").build());

        // 사용자가 Netflix 프리미엄 17000원 구독 중
        User user = userRepository.findById(testUserId).orElseThrow();
        userSubscriptionRepository.save(UserSubscription.builder()
                .user(user).service(netflix)
                .planName("프리미엄").monthlyPrice(17000)
                .billingDate(15).billingCycle(BillingCycle.MONTHLY)
                .isActive(true).build());

        // when
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/suggestions",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode alternatives = objectMapper.readTree(response.getBody())
                .path("data").path("cheaperAlternatives");

        assertThat(alternatives.size()).isGreaterThanOrEqualTo(2);

        // 첫 번째: 동일 서비스 다운그레이드 (isSameService=true)
        assertThat(alternatives.get(0).path("isSameService").asBoolean()).isTrue();
        assertThat(alternatives.get(0).path("suggestionType").asText()).isEqualTo("DOWNGRADE");
        assertThat(alternatives.get(0).path("netSavings").asInt()).isGreaterThan(0);
        assertThat(alternatives.get(0).path("confidence").asInt()).isBetween(0, 100);
        assertThat(alternatives.get(0).path("reasonCodes").isArray()).isTrue();

        // 마지막: 타 서비스 대안 (isSameService=false)
        boolean hasSwitch = false;
        for (int i = 0; i < alternatives.size(); i++) {
            if (!alternatives.get(i).path("isSameService").asBoolean()) {
                hasSwitch = true;
                assertThat(alternatives.get(i).path("suggestionType").asText()).isEqualTo("SWITCH");
                assertThat(alternatives.get(i).path("switchCost").asInt()).isGreaterThan(0);
                break;
            }
        }
        assertThat(hasSwitch).isTrue();
    }

    @Test
    void 구독별_최대절감만_합산_과대계산_방지() throws Exception {
        // given - Netflix 프리미엄(17000) 구독, 스탠다드(13500) / 베이직(5500) 두 플랜 대안 존재
        // 최대 절감 = 17000-5500 = 11500 (베이직), 두 대안 단순합산 = 3500+11500 = 15000
        ServiceEntity netflix = serviceRepository.save(ServiceEntity.builder()
                .serviceName("Netflix-savings-" + System.nanoTime())
                .category(ServiceCategory.OTT)
                .description("스트리밍")
                .officialUrl("https://netflix.com")
                .iconUrl("icon.png")
                .build());

        subscriptionPlanRepository.save(SubscriptionPlan.builder()
                .service(netflix).planName("스탠다드").monthlyPrice(13500).build());
        subscriptionPlanRepository.save(SubscriptionPlan.builder()
                .service(netflix).planName("베이직").monthlyPrice(5500).build());

        User user = userRepository.findById(testUserId).orElseThrow();
        userSubscriptionRepository.save(UserSubscription.builder()
                .user(user).service(netflix)
                .planName("프리미엄").monthlyPrice(17000)
                .billingDate(15).billingCycle(BillingCycle.MONTHLY)
                .isActive(true).build());

        // when
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/suggestions",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");

        // 대안은 최소 2개 (스탠다드 13500, 베이직 5500 - 다른 테스트 데이터로 추가 대안 있을 수 있음)
        assertThat(data.path("cheaperAlternatives").size()).isGreaterThanOrEqualTo(2);

        // 핵심 검증: totalPotentialSavings = 구독별 최대 절감만 합산
        // Netflix 하나만 구독 중이므로, 가장 저렴한 대안(베이직 5500)으로 전환 시 17000-5500=11500 절감
        int totalSavings = data.path("totalPotentialSavings").asInt();
        assertThat(totalSavings).isEqualTo(11500);
    }

    @Test
    void 비활성_구독은_최적화_대상에서_제외() throws Exception {
        // given - 비활성 구독
        ServiceEntity netflix = serviceRepository.save(ServiceEntity.builder()
                .serviceName("Netflix-inactive-" + System.nanoTime())
                .category(ServiceCategory.OTT)
                .description("스트리밍")
                .officialUrl("https://netflix.com")
                .iconUrl("icon.png")
                .build());

        User user = userRepository.findById(testUserId).orElseThrow();
        userSubscriptionRepository.save(UserSubscription.builder()
                .user(user).service(netflix)
                .planName("프리미엄").monthlyPrice(17000)
                .billingDate(15).billingCycle(BillingCycle.MONTHLY)
                .isActive(false).build());

        // when
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/suggestions",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        assertThat(data.path("duplicateServices")).isEmpty();
        assertThat(data.path("cheaperAlternatives")).isEmpty();
    }

    @Test
    void 연간결제_구독은_월환산_기준으로_대안을_계산() throws Exception {
        // given - 연 120,000원 구독(월환산 10,000원), 동일 서비스 대안 9,000원
        ServiceEntity cloudService = serviceRepository.save(ServiceEntity.builder()
                .serviceName("Cloud-yearly-" + System.nanoTime())
                .category(ServiceCategory.CLOUD)
                .description("클라우드 스토리지")
                .officialUrl("https://cloud.example.com")
                .iconUrl("icon.png")
                .build());

        subscriptionPlanRepository.save(SubscriptionPlan.builder()
                .service(cloudService)
                .planName("라이트")
                .monthlyPrice(9000)
                .description("기본 플랜")
                .build());

        User user = userRepository.findById(testUserId).orElseThrow();
        userSubscriptionRepository.save(UserSubscription.builder()
                .user(user).service(cloudService)
                .planName("연간 플랜").monthlyPrice(120000)
                .billingDate(1).billingCycle(BillingCycle.YEARLY)
                .isActive(true).build());

        // when
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/suggestions",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode alternatives = objectMapper.readTree(response.getBody())
                .path("data")
                .path("cheaperAlternatives");

        JsonNode normalizedAlt = null;
        for (int i = 0; i < alternatives.size(); i++) {
            JsonNode candidate = alternatives.get(i);
            if (candidate.path("alternativeServiceId").asLong() == cloudService.getId()
                    && candidate.path("alternativePlan").path("planName").asText().equals("라이트")) {
                normalizedAlt = candidate;
                break;
            }
        }

        assertThat(normalizedAlt).as("연간 결제 월환산 대안").isNotNull();
        assertThat(normalizedAlt.path("currentPrice").asInt()).isEqualTo(10000);
        assertThat(normalizedAlt.path("alternativePrice").asInt()).isEqualTo(9000);
        assertThat(normalizedAlt.path("savings").asInt()).isEqualTo(1000);
        assertThat(normalizedAlt.path("netSavings").asInt()).isEqualTo(1000);
    }

    @Test
    void 전역최적화_MVP는_최대_변경건수_제약을_적용한다() throws Exception {
        // given - 4개 활성 구독 각각에 저렴한 대안 1개씩 제공 (기본 maxChangesPerRun=3)
        User user = userRepository.findById(testUserId).orElseThrow();

        for (int i = 0; i < 4; i++) {
            ServiceEntity service = serviceRepository.save(ServiceEntity.builder()
                    .serviceName("Global-opt-" + i + "-" + System.nanoTime())
                    .category(ServiceCategory.OTT)
                    .description("스트리밍")
                    .officialUrl("https://example.com/" + i)
                    .iconUrl("icon.png")
                    .build());

            subscriptionPlanRepository.save(SubscriptionPlan.builder()
                    .service(service)
                    .planName("가성비 플랜")
                    .monthlyPrice(5000 + (i * 100))
                    .description("할인 플랜")
                    .build());

            userSubscriptionRepository.save(UserSubscription.builder()
                    .user(user).service(service)
                    .planName("프리미엄")
                    .monthlyPrice(15000 + (i * 100))
                    .billingDate(10).billingCycle(BillingCycle.MONTHLY)
                    .isActive(true).build());
        }

        // when
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/suggestions",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        JsonNode optimizedAlternatives = data.path("optimizedAlternatives");

        assertThat(optimizedAlternatives.isArray()).isTrue();
        assertThat(optimizedAlternatives.size()).isEqualTo(3);
    }

    @Test
    void 최적화_이벤트_기록_성공() {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String eventRequestJson = """
                {
                  "eventType": "IMPRESSION",
                  "currentSubscriptionId": 1,
                  "alternativeServiceId": 2,
                  "suggestionType": "DOWNGRADE",
                  "source": "OPTIMIZATION_PAGE",
                  "metadata": {
                    "confidence": 85
                  }
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/events",
                HttpMethod.POST,
                new HttpEntity<>(eventRequestJson, headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void 최적화_이벤트_타입이_유효하지_않으면_400() {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String eventRequestJson = """
                {
                  "eventType": "UNKNOWN_EVENT",
                  "source": "OPTIMIZATION_PAGE"
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/events",
                HttpMethod.POST,
                new HttpEntity<>(eventRequestJson, headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
