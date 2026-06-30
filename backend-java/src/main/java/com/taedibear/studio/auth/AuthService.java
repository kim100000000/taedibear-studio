package com.taedibear.studio.auth;

import com.taedibear.studio.auth.dto.AuthResponse;
import com.taedibear.studio.auth.dto.LoginRequest;
import com.taedibear.studio.auth.dto.RegisterRequest;
import com.taedibear.studio.auth.dto.UserDto;
import com.taedibear.studio.common.exception.ApiException;
import com.taedibear.studio.domain.User;
import com.taedibear.studio.repository.UserRepository;
import com.taedibear.studio.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
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

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw ApiException.badRequest("이미 사용 중인 이메일이에요.");
		}

		User user = User.builder()
				.name(request.name())
				.email(request.email())
				.passwordHash(passwordEncoder.encode(request.password()))
				.build();
		userRepository.save(user);

		return new AuthResponse(jwtTokenProvider.generateToken(user), UserDto.from(user));
	}

	public AuthResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.email())
				.orElseThrow(() -> ApiException.unauthorized("이메일 또는 비밀번호가 올바르지 않아요."));

		if (user.getPasswordHash() == null
				|| !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw ApiException.unauthorized("이메일 또는 비밀번호가 올바르지 않아요.");
		}

		return new AuthResponse(jwtTokenProvider.generateToken(user), UserDto.from(user));
	}

	// 만료된 토큰도 허용해서 새 토큰을 발급한다 (서명만 검증).
	public String refresh(String token) {
		Claims claims;
		try {
			claims = jwtTokenProvider.parseClaimsIgnoringExpiration(token);
		} catch (Exception ex) {
			throw ApiException.unauthorized("유효하지 않은 토큰이에요.");
		}

		Long userId = jwtTokenProvider.getUserId(claims);
		User user = userRepository.findById(userId)
				.orElseThrow(() -> ApiException.unauthorized("유효하지 않은 토큰이에요."));

		return jwtTokenProvider.generateToken(user);
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

	public String generateToken(User user) {
		return jwtTokenProvider.generateToken(user);
	}
}
