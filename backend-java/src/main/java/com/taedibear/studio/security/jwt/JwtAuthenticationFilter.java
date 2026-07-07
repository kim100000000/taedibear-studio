package com.taedibear.studio.security.jwt;

import com.taedibear.studio.security.UserPrincipal;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;

	// Phase 5-1: 관리자 판별 — ADMIN_EMAIL 환경변수(app.mail.admin)와 이메일 일치 여부.
	// DB 컬럼 없이 운영자 1인을 지정하는 가장 단순한 방식. 비어 있으면 관리자가 없다.
	@Value("${app.mail.admin:}")
	private String adminEmail;

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		String token = resolveToken(request);

		if (token != null) {
			try {
				Claims claims = jwtTokenProvider.parseClaims(token);
				Long userId = jwtTokenProvider.getUserId(claims);
				String email = claims.get("email", String.class);

				boolean isAdmin = adminEmail != null && !adminEmail.isBlank()
						&& adminEmail.equalsIgnoreCase(email);
				UserPrincipal principal = new UserPrincipal(userId, email, isAdmin);
				UsernamePasswordAuthenticationToken authentication =
						new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (JwtException | IllegalArgumentException ex) {
				// 유효하지 않은 토큰이면 인증 없이 통과시키고, 보호된 엔드포인트는
				// AuthenticationEntryPoint가 401을 반환하게 한다.
				SecurityContextHolder.clearContext();
			}
		}

		filterChain.doFilter(request, response);
	}

	private String resolveToken(HttpServletRequest request) {
		String bearer = request.getHeader("Authorization");
		if (bearer != null && bearer.startsWith("Bearer ")) {
			return bearer.substring(7);
		}
		return null;
	}
}
