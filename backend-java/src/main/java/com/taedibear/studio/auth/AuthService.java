package com.taedibear.studio.auth;

import com.taedibear.studio.auth.dto.AuthResponse;
import com.taedibear.studio.auth.dto.LoginRequest;
import com.taedibear.studio.auth.dto.RegisterRequest;
import com.taedibear.studio.auth.dto.UserDto;
import com.taedibear.studio.common.exception.ApiException;
import com.taedibear.studio.domain.User;
import com.taedibear.studio.repository.UserRepository;
import com.taedibear.studio.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenService refreshTokenService;

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
						return user;
					}
					return userRepository.save(User.builder()
							.name(name)
							.email(email)
							.googleId(googleId)
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
						return user;
					}
					return userRepository.save(User.builder()
							.name(name)
							.email(email)
							.kakaoId(kakaoId)
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
						return user;
					}
					return userRepository.save(User.builder()
							.name(name)
							.email(email)
							.naverId(naverId)
							.build());
				});
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
