package com.project.subing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.subing.dto.user.LoginRequest;
import com.project.subing.dto.user.SignupRequest;
import com.project.subing.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
public class UserControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void 회원가입_성공() {
        // given
        String requestJson = """
                {
                    "email": "test@example.com",
                    "password": "password123!",
                    "name": "테스트 사용자"
                }
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(requestJson, headers);

        // when
        String url = "http://localhost:" + port + "/api/v1/users/signup";
        String response = restTemplate.postForObject(url, request, String.class);

        // then
        assert response != null;
        assert response.contains("test@example.com");
        assert response.contains("테스트 사용자");
    }

    @Test
    public void 로그인_성공() {
        // given - 먼저 회원가입
        String signupJson = """
                {
                    "email": "test@example.com",
                    "password": "password123!",
                    "name": "테스트 사용자"
                }
                """;

        HttpHeaders signupHeaders = new HttpHeaders();
        signupHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> signupRequest = new HttpEntity<>(signupJson, signupHeaders);
        String signupUrl = "http://localhost:" + port + "/api/v1/users/signup";
        restTemplate.postForObject(signupUrl, signupRequest, String.class);

        // when
        String loginJson = """
                {
                    "email": "test@example.com",
                    "password": "password123!"
                }
                """;

        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> loginRequest = new HttpEntity<>(loginJson, loginHeaders);
        String loginUrl = "http://localhost:" + port + "/api/v1/users/login";
        String response = restTemplate.postForObject(loginUrl, loginRequest, String.class);

        // then
        assert response != null;
        assert response.contains("test@example.com");
    }
}
