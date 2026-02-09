package com.project.subing.service;

import com.project.subing.dto.user.GoogleTokenResponse;
import com.project.subing.dto.user.GoogleUserInfo;
import com.project.subing.exception.external.GoogleAuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class GoogleOAuthService {

    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;

    public GoogleOAuthService(
            WebClient.Builder webClientBuilder,
            @Value("${oauth.google.client-id}") String clientId,
            @Value("${oauth.google.client-secret}") String clientSecret) {
        this.webClient = webClientBuilder.build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public GoogleTokenResponse exchangeCodeForToken(String code, String redirectUri) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        try {
            return webClient.post()
                    .uri("https://oauth2.googleapis.com/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(params))
                    .retrieve()
                    .bodyToMono(GoogleTokenResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Google 토큰 교환 실패", e);
            throw new GoogleAuthException("토큰 교환에 실패했습니다", e);
        }
    }

    public GoogleUserInfo getUserInfo(String accessToken) {
        try {
            return webClient.get()
                    .uri("https://www.googleapis.com/oauth2/v3/userinfo")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(GoogleUserInfo.class)
                    .block();
        } catch (Exception e) {
            log.error("Google 사용자 정보 조회 실패", e);
            throw new GoogleAuthException("사용자 정보 조회에 실패했습니다", e);
        }
    }
}
