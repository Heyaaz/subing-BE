package com.project.subing.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.subing.domain.user.entity.User;
import com.project.subing.domain.service.entity.ServiceEntity;
import com.project.subing.domain.common.ServiceCategory;
import com.project.subing.repository.UserRepository;
import com.project.subing.repository.UserSubscriptionRepository;
import com.project.subing.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 구독 API 통합 테스트.
 * HTTP 요청으로 서버를 호출하므로 @Transactional 미사용 - 서버가 setUp()에서 저장한 데이터를 보려면 커밋이 필요함.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SubscriptionControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSubscriptionRepository userSubscriptionRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Long testUserId;
    private Long testServiceId;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성 (@Transactional 없음 → 테스트 간 이메일 중복 방지)
        String email = "subscription-test-" + System.nanoTime() + "@example.com";
        User testUser = User.builder()
                .name("테스트 사용자")
                .email(email)
                .password("password123!")
                .tier(com.project.subing.domain.user.entity.UserTier.FREE)
                .role(com.project.subing.domain.user.entity.UserRole.USER)
                .build();
        User savedUser = userRepository.save(testUser);
        testUserId = savedUser.getId();
        
        // 테스트용 서비스 생성
        ServiceEntity testService = ServiceEntity.builder()
                .serviceName("Netflix")
                .category(ServiceCategory.OTT)
                .description("스트리밍 서비스")
                .officialUrl("https://netflix.com")
                .iconUrl("https://netflix.com/icon.png")
                .build();
        ServiceEntity savedService = serviceRepository.save(testService);
        testServiceId = savedService.getId();
    }

    @Test
    public void 구독_추가_성공() {
        // given
        String requestJson = String.format("""
                {
                    "serviceId": %d,
                    "planName": "프리미엄",
                    "monthlyPrice": 17000,
                    "billingDate": 15,
                    "billingCycle": "MONTHLY",
                    "notes": "가족 공유 중"
                }
                """, testServiceId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(requestJson, headers);

        // when
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.set("X-Test-User-Id", String.valueOf(testUserId));
        HttpEntity<String> authRequest = new HttpEntity<>(requestJson, authHeaders);
        String url = "http://localhost:" + port + "/api/v1/subscriptions";
        String response = restTemplate.postForObject(url, authRequest, String.class);

        // then
        assert response != null;
        assert response.contains("success");
    }

    @Test
    public void 구독_목록_조회_성공() {
        // when
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Test-User-Id", String.valueOf(testUserId));
        String url = "http://localhost:" + port + "/api/v1/subscriptions";
        String response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers), String.class).getBody();

        // then
        assert response != null;
        assert response.contains("success");
    }

    @Test
    public void 구독_수정_성공() throws Exception {
        // given - 먼저 구독을 생성
        String createRequestJson = String.format("""
                {
                    "serviceId": %d,
                    "planName": "프리미엄",
                    "monthlyPrice": 17000,
                    "billingDate": 15,
                    "billingCycle": "MONTHLY",
                    "notes": "가족 공유 중"
                }
                """, testServiceId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Test-User-Id", String.valueOf(testUserId));
        HttpEntity<String> createRequest = new HttpEntity<>(createRequestJson, headers);

        String createUrl = "http://localhost:" + port + "/api/v1/subscriptions";
        org.springframework.http.ResponseEntity<String> createEntity = restTemplate.exchange(
                createUrl, org.springframework.http.HttpMethod.POST, createRequest, String.class);
        assertThat(createEntity.getStatusCode())
                .as("구독 생성 응답 (body=%s)", createEntity.getBody())
                .isEqualTo(org.springframework.http.HttpStatus.CREATED);
        String createResponse = createEntity.getBody();
        assertThat(createResponse).isNotNull();
        Long subscriptionId = objectMapper.readTree(createResponse).path("data").path("id").asLong();
        assertThat(subscriptionId).as("생성된 구독 ID").isPositive();

        String updateRequestJson = String.format("""
                {
                    "serviceId": %d,
                    "planName": "프리미엄 플러스",
                    "monthlyPrice": 20000,
                    "billingDate": 20,
                    "billingCycle": "MONTHLY",
                    "notes": "업그레이드된 플랜"
                }
                """, testServiceId);

        HttpEntity<String> updateRequest = new HttpEntity<>(updateRequestJson, headers);

        // when
        String updateUrl = "http://localhost:" + port + "/api/v1/subscriptions/" + subscriptionId;
        org.springframework.http.ResponseEntity<String> responseEntity = restTemplate.exchange(
                updateUrl, org.springframework.http.HttpMethod.PUT, updateRequest, String.class);
        String response = responseEntity.getBody();

        // then - 수정 성공: HTTP 200, 본문에 success 포함
        assertThat(responseEntity.getStatusCode())
                .as("PUT 응답 상태 (body=%s)", response)
                .isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response)
                .as("응답 본문")
                .isNotNull()
                .contains("success");
    }

    @Test
    public void 구독_삭제_성공() {
        // given - 먼저 구독을 생성
        String createRequestJson = String.format("""
                {
                    "serviceId": %d,
                    "planName": "프리미엄",
                    "monthlyPrice": 17000,
                    "billingDate": 15,
                    "billingCycle": "MONTHLY",
                    "notes": "가족 공유 중"
                }
                """, testServiceId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Test-User-Id", String.valueOf(testUserId));
        HttpEntity<String> createRequest = new HttpEntity<>(createRequestJson, headers);

        String createUrl = "http://localhost:" + port + "/api/v1/subscriptions";
        restTemplate.postForObject(createUrl, createRequest, String.class);
        
        Long subscriptionId = 1L;

        // when
        String deleteUrl = "http://localhost:" + port + "/api/v1/subscriptions/" + subscriptionId;
        String response = restTemplate.exchange(deleteUrl, org.springframework.http.HttpMethod.DELETE, 
                new HttpEntity<>(headers), String.class).getBody();

        // then
        assert response != null;
        assert response.contains("success");
    }

    @Test
    public void 구독_상태_변경_성공() {
        // given - 먼저 구독을 생성
        String createRequestJson = String.format("""
                {
                    "serviceId": %d,
                    "planName": "프리미엄",
                    "monthlyPrice": 17000,
                    "billingDate": 15,
                    "billingCycle": "MONTHLY",
                    "notes": "가족 공유 중"
                }
                """, testServiceId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Test-User-Id", String.valueOf(testUserId));
        HttpEntity<String> createRequest = new HttpEntity<>(createRequestJson, headers);

        String createUrl = "http://localhost:" + port + "/api/v1/subscriptions";
        restTemplate.postForObject(createUrl, createRequest, String.class);
        
        Long subscriptionId = 1L;

        String statusRequestJson = """
                {
                    "isActive": false
                }
                """;

        HttpEntity<String> statusRequest = new HttpEntity<>(statusRequestJson, headers);

        // when
        String statusUrl = "http://localhost:" + port + "/api/v1/subscriptions/" + subscriptionId + "/status";
        String response = restTemplate.exchange(statusUrl, org.springframework.http.HttpMethod.PATCH, 
                statusRequest, String.class).getBody();

        // then
        assert response != null;
        assert response.contains("success");
    }
}
