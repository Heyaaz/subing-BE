package com.project.subing.config;

import com.project.subing.security.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
 * 테스트 환경용 Security 설정.
 * TestPrincipalFilter를 standalone servlet filter로 등록하여
 * security chain 이전에 request attribute로 SecurityContext를 설정.
 * SecurityContextHolderFilter가 RequestAttributeSecurityContextRepository에서 context를 로드.
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
                .anyRequest().permitAll()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * TestPrincipalFilter를 standalone servlet filter로 등록.
     * order를 security chain보다 앞(-200)으로 설정하여 request attribute를
     * SecurityContextHolderFilter보다 먼저 세팅.
     */
    @Bean
    public FilterRegistrationBean<TestPrincipalFilter> testPrincipalFilterRegistration() {
        FilterRegistrationBean<TestPrincipalFilter> registration =
                new FilterRegistrationBean<>(new TestPrincipalFilter());
        registration.setOrder(-200);
        return registration;
    }

    /**
     * JwtAuthenticationFilter @Component auto-register 방지.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> disableJwtFilterAutoRegistration(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(jwtAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }
}
