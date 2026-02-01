package com.project.subing.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 인증된 사용자(principal) 추출 유틸.
 * JWT 필터에서 principal은 Long userId로 설정됨.
 * 내 데이터 API에서는 클라이언트가 보낸 userId를 받지 않고, 여기서 추출한 principal만 사용 (/me 패턴).
 */
public final class SecurityUtils {

	private SecurityUtils() {
	}

	/**
	 * 현재 인증된 사용자 ID. 인증되지 않았으면 null.
	 */
	public static Long getCurrentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getPrincipal() == null || !(auth.getPrincipal() instanceof Long)) {
			return null;
		}
		return (Long) auth.getPrincipal();
	}
}
