package com.project.subing.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.subing.domain.optimization.entity.OptimizationEngineConfig;
import com.project.subing.domain.user.entity.User;
import com.project.subing.domain.user.entity.UserRole;
import com.project.subing.domain.user.entity.UserTier;
import com.project.subing.repository.OptimizationEngineConfigRepository;
import com.project.subing.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminOptimizationConfigControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OptimizationEngineConfigRepository optimizationEngineConfigRepository;

    private Long adminUserId;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/admin/optimization-config";

        User admin = userRepository.save(User.builder()
                .name("최적화 정책 관리자")
                .email("admin-opt-" + System.nanoTime() + "@example.com")
                .password("password123!")
                .tier(UserTier.PRO)
                .role(UserRole.ADMIN)
                .build());

        adminUserId = admin.getId();
    }

    @AfterEach
    void tearDown() {
        if (adminUserId == null) {
            return;
        }

        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.exchange(
                baseUrl + "/rollback",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class
        );
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Test-User-Id", String.valueOf(adminUserId));
        return headers;
    }

    @Test
    void 최적화_정책_조회_성공() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("effectivePolicy").path("maxChangesPerRun").asInt()).isGreaterThan(0);
        assertThat(body.path("data").path("defaultPolicy").path("topKPlansPerService").asInt()).isGreaterThan(0);
    }

    @Test
    void 최적화_정책_업데이트_후_롤백_성공() throws Exception {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String updateRequest = """
                {
                  "overrides": {
                    "portfolio.maxChangesPerRun": "2",
                    "performance.topKPlansPerService": "4",
                    "tracking.enabled": "false"
                  }
                }
                """;

        ResponseEntity<String> updateResponse = restTemplate.exchange(
                baseUrl,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, headers),
                String.class
        );

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode updateData = objectMapper.readTree(updateResponse.getBody()).path("data");
        assertThat(updateData.path("effectivePolicy").path("maxChangesPerRun").asInt()).isEqualTo(2);
        assertThat(updateData.path("effectivePolicy").path("topKPlansPerService").asInt()).isEqualTo(4);
        assertThat(updateData.path("effectivePolicy").path("trackingEnabled").asBoolean()).isFalse();
        assertThat(updateData.path("activeOverrides").path("portfolio.maxChangesPerRun").asText()).isEqualTo("2");
        assertThat(updateData.path("recentAudits").isArray()).isTrue();
        assertThat(updateData.path("recentAudits").size()).isGreaterThan(0);

        ResponseEntity<String> rollbackResponse = restTemplate.exchange(
                baseUrl + "/rollback",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(rollbackResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode rollbackData = objectMapper.readTree(rollbackResponse.getBody()).path("data");
        assertThat(rollbackData.path("effectivePolicy").path("maxChangesPerRun").asInt())
                .isEqualTo(rollbackData.path("defaultPolicy").path("maxChangesPerRun").asInt());
        assertThat(rollbackData.path("activeOverrides").size()).isZero();
    }

    @Test
    void 최적화_정책_키_단위_롤백_성공() throws Exception {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String firstUpdateRequest = """
                {
                  "overrides": {
                    "portfolio.maxChangesPerRun": "5"
                  }
                }
                """;

        String secondUpdateRequest = """
                {
                  "overrides": {
                    "portfolio.maxChangesPerRun": "2"
                  }
                }
                """;

        ResponseEntity<String> firstUpdateResponse = restTemplate.exchange(
                baseUrl,
                HttpMethod.PUT,
                new HttpEntity<>(firstUpdateRequest, headers),
                String.class
        );
        assertThat(firstUpdateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> secondUpdateResponse = restTemplate.exchange(
                baseUrl,
                HttpMethod.PUT,
                new HttpEntity<>(secondUpdateRequest, headers),
                String.class
        );
        assertThat(secondUpdateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode secondUpdateData = objectMapper.readTree(secondUpdateResponse.getBody()).path("data");
        assertThat(secondUpdateData.path("effectivePolicy").path("maxChangesPerRun").asInt()).isEqualTo(2);

        ResponseEntity<String> rollbackByKeyResponse = restTemplate.exchange(
                baseUrl + "/rollback/portfolio.maxChangesPerRun",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(rollbackByKeyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode rollbackByKeyData = objectMapper.readTree(rollbackByKeyResponse.getBody()).path("data");
        assertThat(rollbackByKeyData.path("effectivePolicy").path("maxChangesPerRun").asInt()).isEqualTo(5);
        assertThat(rollbackByKeyData.path("activeOverrides").path("portfolio.maxChangesPerRun").asText()).isEqualTo("5");

        ResponseEntity<String> secondRollbackByKeyResponse = restTemplate.exchange(
                baseUrl + "/rollback/portfolio.maxChangesPerRun",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(secondRollbackByKeyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode secondRollbackByKeyData = objectMapper.readTree(secondRollbackByKeyResponse.getBody()).path("data");
        assertThat(secondRollbackByKeyData.path("effectivePolicy").path("maxChangesPerRun").asInt())
                .isEqualTo(secondRollbackByKeyData.path("defaultPolicy").path("maxChangesPerRun").asInt());
        assertThat(secondRollbackByKeyData.path("activeOverrides").has("portfolio.maxChangesPerRun")).isFalse();
    }

    @Test
    void 최적화_정책_변경_이력_조회_성공() throws Exception {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String updateRequest = """
                {
                  "overrides": {
                    "portfolio.maxChangesPerRun": "6"
                  }
                }
                """;

        ResponseEntity<String> updateResponse = restTemplate.exchange(
                baseUrl,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, headers),
                String.class
        );
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> historyResponse = restTemplate.exchange(
                baseUrl + "/audits?configKey=portfolio.maxChangesPerRun&limit=5",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode historyBody = objectMapper.readTree(historyResponse.getBody());
        assertThat(historyBody.path("success").asBoolean()).isTrue();
        JsonNode firstAudit = historyBody.path("data").get(0);
        assertThat(firstAudit.path("configKey").asText()).isEqualTo("portfolio.maxChangesPerRun");
        assertThat(firstAudit.path("actionType").asText()).isEqualTo("UPSERT");
        assertThat(firstAudit.path("changedByUserId").asLong()).isEqualTo(adminUserId);
    }

    @Test
    void audit_없는_레거시_override도_키_롤백_성공() throws Exception {
        String legacyKey = "pricing.crossCategoryPenalty";
        optimizationEngineConfigRepository.save(OptimizationEngineConfig.builder()
                .configKey(legacyKey)
                .configValue("7777")
                .isActive(true)
                .build());

        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> rollbackByKeyResponse = restTemplate.exchange(
                baseUrl + "/rollback/" + legacyKey,
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(rollbackByKeyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(rollbackByKeyResponse.getBody()).path("data");
        assertThat(data.path("effectivePolicy").path("crossCategoryPenalty").asInt())
                .isEqualTo(data.path("defaultPolicy").path("crossCategoryPenalty").asInt());
        assertThat(data.path("activeOverrides").has(legacyKey)).isFalse();
    }
}
