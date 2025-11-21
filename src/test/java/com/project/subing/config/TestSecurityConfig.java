package com.project.subing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 테스트 환경용 Security 설정
 * 모든 엔드포인트에 대한 인증을 비활성화하여 테스트 편의성 제공
 */
@Configuration
@EnableWebSecurity
@Profile("test")
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()  // 테스트 환경에서는 모든 요청 허용
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}