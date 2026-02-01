package com.project.subing.controller;

import com.project.subing.domain.common.ServiceCategory;
import com.project.subing.domain.service.entity.ServiceEntity;
import com.project.subing.domain.subscription.entity.UserSubscription;
import com.project.subing.domain.user.entity.User;
import com.project.subing.dto.common.ApiResponse;
import com.project.subing.repository.ServiceRepository;
import com.project.subing.repository.UserRepository;
import com.project.subing.repository.UserSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.project.subing.domain.user.entity.UserRole.*;
import static com.project.subing.domain.user.entity.UserTier.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class StatisticsControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private UserSubscriptionRepository userSubscriptionRepository;

    private String baseUrl;
    private User testUser;
    private ServiceEntity netflix;
    private ServiceEntity spotify;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/statistics";
        
        // 테스트 사용자 생성 (@Transactional로 롤백되므로 deleteAll 불필요)
        testUser = User.builder()
                .email("statistics-test-" + System.currentTimeMillis() + "@example.com")
                .name("통계 테스트 사용자")
                .password("password123!")
                .tier(FREE)
                .role(USER)
                .build();
        testUser = userRepository.save(testUser);
        
        // 테스트 서비스 생성
        netflix = ServiceEntity.builder()
                .serviceName("Netflix")
                .description("스트리밍 서비스")
                .category(ServiceCategory.OTT)
                .officialUrl("https://netflix.com")
                .iconUrl("https://netflix.com/logo.png")
                .build();
        netflix = serviceRepository.save(netflix);
        
        spotify = ServiceEntity.builder()
                .serviceName("Spotify")
                .description("음악 스트리밍")
                .category(ServiceCategory.MUSIC)
                .officialUrl("https://spotify.com")
                .iconUrl("https://spotify.com/logo.png")
                .build();
        spotify = serviceRepository.save(spotify);
        
        // 테스트 구독 생성
        UserSubscription netflixSubscription = UserSubscription.builder()
                .user(testUser)
                .service(netflix)
                .planName("Standard")
                .monthlyPrice(13500)
                .billingCycle(com.project.subing.domain.common.BillingCycle.MONTHLY)
                .billingDate(15)
                .notes("월 15일 결제")
                .isActive(true)
                .build();
        userSubscriptionRepository.save(netflixSubscription);
        
        UserSubscription spotifySubscription = UserSubscription.builder()
                .user(testUser)
                .service(spotify)
                .planName("Premium Individual")
                .monthlyPrice(10900)
                .billingCycle(com.project.subing.domain.common.BillingCycle.MONTHLY)
                .billingDate(1)
                .notes("월 1일 결제")
                .isActive(true)
                .build();
        userSubscriptionRepository.save(spotifySubscription);
    }

    @Test
    void 월별_지출_통계_조회_성공() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Test-User-Id", String.valueOf(testUser.getId()));
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                baseUrl + "/monthly?year=2024&month=10",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                ApiResponse.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).contains("월별 지출 통계를 조회했습니다");
    }

    @Test
    void 지출_분석_조회_성공() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Test-User-Id", String.valueOf(testUser.getId()));
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                baseUrl + "/analysis",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                ApiResponse.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).contains("지출 분석 결과를 조회했습니다");
    }
}
