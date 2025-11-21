package com.project.subing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.subing.domain.common.ServiceCategory;
import com.project.subing.domain.service.entity.ServiceEntity;
import com.project.subing.domain.service.entity.SubscriptionPlan;
import com.project.subing.repository.ServiceRepository;
import com.project.subing.repository.SubscriptionPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Commit;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ServiceControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/services";
        
        // 테스트 데이터 생성
        ServiceEntity netflix = ServiceEntity.builder()
                .serviceName("Netflix")
                .description("스트리밍 서비스")
                .category(ServiceCategory.OTT)
                .officialUrl("https://netflix.com")
                .iconUrl("https://netflix.com/logo.png")
                .build();
        netflix = serviceRepository.save(netflix);

        ServiceEntity spotify = ServiceEntity.builder()
                .serviceName("Spotify")
                .description("음악 스트리밍")
                .category(ServiceCategory.MUSIC)
                .officialUrl("https://spotify.com")
                .iconUrl("https://spotify.com/logo.png")
                .build();
        spotify = serviceRepository.save(spotify);

        // Netflix 플랜
        SubscriptionPlan netflixBasic = SubscriptionPlan.builder()
                .service(netflix)
                .planName("Basic")
                .description("기본 플랜")
                .monthlyPrice(9500)
                .features("SD 화질, 1개 기기")
                .isPopular(false)
                .build();
        subscriptionPlanRepository.save(netflixBasic);

        SubscriptionPlan netflixStandard = SubscriptionPlan.builder()
                .service(netflix)
                .planName("Standard")
                .description("표준 플랜")
                .monthlyPrice(13500)
                .features("HD 화질, 2개 기기")
                .isPopular(true)
                .build();
        subscriptionPlanRepository.save(netflixStandard);

        // Spotify 플랜
        SubscriptionPlan spotifyFree = SubscriptionPlan.builder()
                .service(spotify)
                .planName("Free")
                .description("무료 플랜")
                .monthlyPrice(0)
                .features("광고 포함, 제한된 기능")
                .isPopular(false)
                .build();
        subscriptionPlanRepository.save(spotifyFree);

        SubscriptionPlan spotifyPremium = SubscriptionPlan.builder()
                .service(spotify)
                .planName("Premium")
                .description("프리미엄 플랜")
                .monthlyPrice(10900)
                .features("광고 없음, 오프라인 재생")
                .isPopular(true)
                .build();
        subscriptionPlanRepository.save(spotifyPremium);
    }

    @Test
    void 서비스_목록_조회_성공() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Netflix");
        assertThat(response.getBody()).contains("Spotify");
    }

    @Test
    void 서비스_상세_조회_성공() {
        List<ServiceEntity> services = serviceRepository.findAll();
        Long serviceId = services.get(0).getId();
        
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/" + serviceId, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("서비스 정보를 조회했습니다");
    }

    @Test
    void 카테고리별_서비스_조회_성공() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/category/OTT", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("카테고리별 서비스 목록을 조회했습니다");
    }

    @Test
    void 서비스_비교_성공() {
        List<ServiceEntity> services = serviceRepository.findAll();
        Long serviceId1 = services.get(0).getId();
        Long serviceId2 = services.get(1).getId();
        
        String requestBody = String.format("{\"serviceIds\": [%d, %d]}", serviceId1, serviceId2);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/compare", 
                requestBody, 
                String.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("서비스 비교 결과를 조회했습니다");
    }

    @Test
    void 서비스_비교_실패_서비스_없음() {
        String requestBody = "{\"serviceIds\": [999, 998]}";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/compare", 
                requestBody, 
                String.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
