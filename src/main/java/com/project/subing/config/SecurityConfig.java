package com.project.subing.config;

import com.project.subing.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.EnumSet;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Profile("!test")  // 테스트 프로파일에서는 비활성화
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 인증 관련 API 및 Spring Boot 에러 페이지 허용
                .requestMatchers("/api/v1/users/signup", "/api/v1/users/login", "/api/v1/users/login/google", "/error").permitAll()
                // Swagger UI 허용
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // WebSocket 엔드포인트 허용
                .requestMatchers("/ws/**").permitAll()
                // 공개 브라우징에서도 사용자별 확인 API는 인증 유지
                .requestMatchers(HttpMethod.GET, "/api/v1/reviews/my", "/api/v1/reviews/service/*/check").authenticated()
                // 공개 브라우징 페이지에서 사용하는 조회 API 허용
                .requestMatchers(HttpMethod.GET, "/api/v1/services", "/api/v1/services/*", "/api/v1/services/category/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/services/compare").permitAll()
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/reviews/service/*",
                    "/api/v1/reviews/service/*/rating",
                    "/api/v1/reviews/*"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/preferences/questions").permitAll()
                // 관리자 API는 ADMIN 역할 필요
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // 나머지 API는 인증 필요
                .requestMatchers("/api/v1/**").authenticated()
                // 정적 리소스 및 SPA 라우팅을 위해 나머지 경로는 모두 허용
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 프론트엔드 분리 후 허용할 Origins
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:3000",      // 로컬 개발 환경
            "http://localhost:5173",      // Vite 개발 환경
            "https://subing.app",         // 프로덕션 (추후 도메인으로 변경)
            "https://subing.store",       // 프로덕션 도메인
            "https://*.vercel.app"        // Vercel 배포 환경
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);  // 프리플라이트 요청 캐시 시간

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * SecurityFilterAutoConfiguration 제외 후 수동 등록.
     * DispatcherType.REQUEST만 지정하여 SseEmitter.complete() 시 발생하는
     * ASYNC 디스패치에서 보안 필터 체인이 실행되지 않도록 함.
     */
    @Bean
    public FilterRegistrationBean<DelegatingFilterProxy> securityFilterChainRegistration() {
        FilterRegistrationBean<DelegatingFilterProxy> registration =
                new FilterRegistrationBean<>(new DelegatingFilterProxy("springSecurityFilterChain"));
        registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
        registration.setOrder(SecurityProperties.BASIC_AUTH_ORDER);
        return registration;
    }
}
