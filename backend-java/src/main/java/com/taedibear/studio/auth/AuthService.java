package com.taedibear.studio.auth;

import com.taedibear.studio.auth.dto.AuthResponse;
import com.taedibear.studio.auth.dto.LoginRequest;
import com.taedibear.studio.auth.dto.RegisterRequest;
import com.taedibear.studio.auth.dto.UserDto;
import com.taedibear.studio.common.exception.ApiException;
import com.taedibear.studio.domain.AuthTokenType;
import com.taedibear.studio.domain.User;
import com.taedibear.studio.email.EmailService;
import com.taedibear.studio.repository.UserRepository;
import com.taedibear.studio.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	// Phase 6: 이메일 링크 유효기간
	private static final Duration PASSWORD_RESET_TTL = Duration.ofMinutes(30);
	private static final Duration EMAIL_VERIFY_TTL = Duration.ofHours(24);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenService refreshTokenService;
	private final AuthTokenService authTokenService;
	private final EmailService emailService;

	// C4: 로그인/회원가입 결과 — access token(응답 body) + refresh token 원문(HttpOnly 쿠키용)
	public record AuthResult(AuthResponse response, String refreshToken) {
	}

	@Transactional
	public AuthResult register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw ApiException.badRequest("이미 사용 중인 이메일이에요.");
		}

		User user = User.builder()
				.name(request.name())
				.email(request.email())
				.passwordHash(passwordEncoder.encode(request.password()))
				.build();
		userRepository.save(user);

		// Phase 6: 인증 메일 발송 — 실패해도 가입은 유지 (재발송 버튼으로 복구 가능)
		sendVerificationMail(user);

		return new AuthResult(
				new AuthResponse(jwtTokenProvider.generateToken(user), UserDto.from(user)),
				refreshTokenService.issue(user.getId()));
	}

	@Transactional
	public AuthResult login(LoginRequest request) {
		User user = userRepository.findByEmail(request.email())
				.orElseThrow(() -> ApiException.unauthorized("이메일 또는 비밀번호가 올바르지 않아요."));

		if (user.getPasswordHash() == null
				|| !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw ApiException.unauthorized("이메일 또는 비밀번호가 올바르지 않아요.");
		}

		return new AuthResult(
				new AuthResponse(jwtTokenProvider.generateToken(user), UserDto.from(user)),
				refreshTokenService.issue(user.getId()));
	}

	// C4: refresh token 회전 — DB에 저장된 유효한 refresh token만 새 access token으로 교환 가능.
	// (기존: 만료 무시 + 서명만 검증 → 한 번 탈취된 JWT를 무기한 재발급받을 수 있었음)
	public record RefreshResult(String accessToken, String newRefreshToken) {
	}

	@Transactional
	public RefreshResult refresh(String rawRefreshToken) {
		RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(rawRefreshToken);
		User user = userRepository.findById(rotation.userId())
				.orElseThrow(() -> ApiException.unauthorized("세션이 만료됐어요. 다시 로그인해주세요."));
		return new RefreshResult(jwtTokenProvider.generateToken(user), rotation.newRefreshToken());
	}

	@Transactional
	public User findOrCreateGoogleUser(String googleId, String email, String name) {
		return userRepository.findByGoogleId(googleId)
				.orElseGet(() -> {
					User user = userRepository.findByEmail(email).orElse(null);
					if (user != null) {
						user.setGoogleId(googleId);
						user.setEmailVerified(true); // Phase 6: 제공자가 이메일 검증 완료
						return user;
					}
					return userRepository.save(User.builder()
							.name(name)
							.email(email)
							.googleId(googleId)
							.emailVerified(true) // Phase 6
							.build());
				});
	}

	@Transactional
	public User findOrCreateKakaoUser(String kakaoId, String email, String name) {
		return userRepository.findByKakaoId(kakaoId)
				.orElseGet(() -> {
					User user = (email != null) ? userRepository.findByEmail(email).orElse(null) : null;
					if (user != null) {
						user.setKakaoId(kakaoId);
						user.setEmailVerified(true); // Phase 6
						return user;
					}
					return userRepository.save(User.builder()
							.name(name)
							.email(email)
							.kakaoId(kakaoId)
							.emailVerified(true) // Phase 6
							.build());
				});
	}

	@Transactional
	public User findOrCreateNaverUser(String naverId, String email, String name) {
		return userRepository.findByNaverId(naverId)
				.orElseGet(() -> {
					User user = (email != null) ? userRepository.findByEmail(email).orElse(null) : null;
					if (user != null) {
						user.setNaverId(naverId);
						user.setEmailVerified(true); // Phase 6
						return user;
					}
					return userRepository.save(User.builder()
							.name(name)
							.email(email)
							.naverId(naverId)
							.emailVerified(true) // Phase 6
							.build());
				});
	}

	// ── Phase 6: 비밀번호 찾기 / 이메일 인증 ─────────────────────────────────

	/**
	 * 비밀번호 재설정 요청. 계정 존재 여부를 노출하지 않기 위해 항상 조용히 성공한다.
	 * 소셜 전용 계정(passwordHash null)도 허용 — 본인 이메일이 확인되므로 비밀번호를 새로 설정해
	 * 이메일 로그인도 쓸 수 있게 된다.
	 */
	@Transactional
	public void forgotPassword(String email) {
		userRepository.findByEmail(email).ifPresentOrElse(user -> {
			String raw = authTokenService.issue(user.getId(), AuthTokenType.password_reset, PASSWORD_RESET_TTL);
			emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), raw);
		}, () -> log.info("[forgot-password] 미가입 이메일 요청 — 무시 (존재 여부 비노출)"));
	}

	/** 재설정 링크의 토큰으로 새 비밀번호 설정 + 모든 세션 폐기(탈취 대비) */
	@Transactional
	public void resetPassword(String rawToken, String newPassword) {
		if (newPassword == null || newPassword.length() < 8) {
			throw ApiException.badRequest("비밀번호는 8자 이상이어야 해요.");
		}
		Long userId = authTokenService.consume(rawToken, AuthTokenType.password_reset);
		User user = userRepository.findById(userId)
				.orElseThrow(() -> ApiException.badRequest("유효하지 않거나 만료된 링크예요. 다시 요청해주세요."));
		user.setPasswordHash(passwordEncoder.encode(newPassword));
		refreshTokenService.revokeAllForUser(userId);
	}

	/** 인증 메일 링크의 토큰으로 이메일 인증 완료 */
	@Transactional
	public void verifyEmail(String rawToken) {
		Long userId = authTokenService.consume(rawToken, AuthTokenType.email_verify);
		userRepository.findById(userId).ifPresent(user -> user.setEmailVerified(true));
	}

	/** 인증 메일 재발송 (로그인 상태에서 호출) */
	@Transactional
	public void resendVerification(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> ApiException.notFound("사용자를 찾을 수 없어요."));
		if (user.isEmailVerified()) {
			throw ApiException.badRequest("이미 인증된 이메일이에요.");
		}
		sendVerificationMail(user);
	}

	private void sendVerificationMail(User user) {
		try {
			String raw = authTokenService.issue(user.getId(), AuthTokenType.email_verify, EMAIL_VERIFY_TTL);
			emailService.sendVerificationEmail(user.getEmail(), user.getName(), raw);
		} catch (Exception ex) {
			log.error("[verify-email] 인증 메일 발송 준비 실패 userId={} — {}", user.getId(), ex.getMessage());
		}
	}

	// C6: 소셜 로그인 콜백 — JWT를 URL에 싣지 않고 refresh token(HttpOnly 쿠키)만 발급한다.
	// 프론트는 /auth 페이지에서 POST /api/auth/refresh 로 access token을 교환한다.
	public String issueRefreshToken(User user) {
		return refreshTokenService.issue(user.getId());
	}

	// C4/M9: 로그아웃 — 쿠키로 받은 refresh token을 DB에서 폐기한다.
	public void revokeRefreshToken(String rawRefreshToken) {
		refreshTokenService.revoke(rawRefreshToken);
	}
}
