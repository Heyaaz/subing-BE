package com.project.subing.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 테스트 전용 필터 (standalone servlet filter).
 * 헤더 X-Test-User-Id가 있으면 SecurityContext를 request attribute에 저장하여
 * SecurityContextHolderFilter → RequestAttributeSecurityContextRepository가 인식하도록 함.
 * Spring Security 6.x 호환.
 */
public class TestPrincipalFilter extends OncePerRequestFilter {

	private static final String SECURITY_CONTEXT_ATTR =
			RequestAttributeSecurityContextRepository.class.getName() + ".SPRING_SECURITY_CONTEXT";

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
				SecurityContext context = new SecurityContextImpl(auth);
				request.setAttribute(SECURITY_CONTEXT_ATTR, context);
			} catch (NumberFormatException ignored) {
			}
		}
		filterChain.doFilter(request, response);
	}
}
