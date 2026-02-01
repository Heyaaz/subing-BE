package com.project.subing.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 테스트 전용 필터.
 * 헤더 X-Test-User-Id가 있으면 해당 값을 principal로 설정하여 @AuthenticationPrincipal Long userId가 동작하도록 함.
 */
public class TestPrincipalFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String userIdHeader = request.getHeader("X-Test-User-Id");
		if (userIdHeader != null && !userIdHeader.isBlank()) {
			try {
				Long userId = Long.parseLong(userIdHeader.trim());
				UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
						userId,
						null,
						List.of(new SimpleGrantedAuthority("ROLE_USER"))
				);
				SecurityContextHolder.getContext().setAuthentication(auth);
			} catch (NumberFormatException ignored) {
			}
		}
		filterChain.doFilter(request, response);
	}
}
