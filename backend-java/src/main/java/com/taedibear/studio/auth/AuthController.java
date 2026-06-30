package com.taedibear.studio.auth;

import com.taedibear.studio.auth.dto.AuthResponse;
import com.taedibear.studio.auth.dto.LoginRequest;
import com.taedibear.studio.auth.dto.RegisterRequest;
import com.taedibear.studio.auth.oauth.GoogleOAuthClient;
import com.taedibear.studio.auth.oauth.KakaoOAuthClient;
import com.taedibear.studio.auth.oauth.NaverOAuthClient;
import com.taedibear.studio.common.ApiResponse;
import com.taedibear.studio.common.exception.ApiException;
import com.taedibear.studio.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final GoogleOAuthClient googleOAuthClient;
	private final KakaoOAuthClient kakaoOAuthClient;
	private final NaverOAuthClient naverOAuthClient;

	@Value("${app.client-url}")
	private String clientUrl;

	private static final SecureRandom RANDOM = new SecureRandom();

	private String randomState() {
		byte[] bytes = new byte[16];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	// POST /api/auth/register
	@PostMapping("/register")
	public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ApiResponse.ok(authService.register(request));
	}

	// POST /api/auth/login
	@PostMapping("/login")
	public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.ok(authService.login(request));
	}

	// POST /api/auth/refresh — 만료된 토큰도 허용해서 새 토큰을 발급한다.
	@PostMapping("/refresh")
	public ApiResponse<?> refresh(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		String token = (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
		if (token == null) {
			throw ApiException.unauthorized("토큰이 필요해요.");
		}
		return ApiResponse.ok(java.util.Map.of("token", authService.refresh(token)));
	}

	// POST /api/auth/logout
	@PostMapping("/logout")
	public ApiResponse<Void> logout() {
		return ApiResponse.ok();
	}

	// GET /api/auth/google
	@GetMapping("/google")
	public void googleLogin(HttpServletResponse response) throws java.io.IOException {
		response.sendRedirect(googleOAuthClient.getLoginUrl(randomState()));
	}

	// GET /api/auth/google/callback
	@GetMapping("/google/callback")
	public void googleCallback(@RequestParam(required = false) String code, HttpServletResponse response) throws java.io.IOException {
		if (code == null) {
			response.sendRedirect(clientUrl + "/login?error=google");
			return;
		}
		try {
			GoogleOAuthClient.Profile profile = googleOAuthClient.authenticate(code);
			User user = authService.findOrCreateGoogleUser(profile.googleId(), profile.email(), profile.name());
			response.sendRedirect(clientUrl + "/auth?token=" + authService.generateToken(user));
		} catch (Exception ex) {
			log.error("[Google login error]", ex);
			response.sendRedirect(clientUrl + "/login?error=google");
		}
	}

	// GET /api/auth/kakao
	@GetMapping("/kakao")
	public void kakaoLogin(HttpServletResponse response) throws java.io.IOException {
		response.sendRedirect(kakaoOAuthClient.getLoginUrl(randomState()));
	}

	// GET /api/auth/kakao/callback
	@GetMapping("/kakao/callback")
	public void kakaoCallback(@RequestParam(required = false) String code, HttpServletResponse response) throws java.io.IOException {
		if (code == null) {
			response.sendRedirect(clientUrl + "/login?error=kakao");
			return;
		}
		try {
			KakaoOAuthClient.Profile profile = kakaoOAuthClient.authenticate(code);
			String email = profile.email() != null ? profile.email() : "kakao_" + profile.kakaoId() + "@kakao.taedibear.local";
			User user = authService.findOrCreateKakaoUser(profile.kakaoId(), email, profile.name());
			response.sendRedirect(clientUrl + "/auth?token=" + authService.generateToken(user));
		} catch (Exception ex) {
			log.error("[Kakao login error]", ex);
			response.sendRedirect(clientUrl + "/login?error=kakao");
		}
	}

	// GET /api/auth/naver
	@GetMapping("/naver")
	public void naverLogin(HttpServletResponse response) throws java.io.IOException {
		response.sendRedirect(naverOAuthClient.getLoginUrl(randomState()));
	}

	// GET /api/auth/naver/callback
	@GetMapping("/naver/callback")
	public void naverCallback(
			@RequestParam(required = false) String code,
			@RequestParam(required = false) String state,
			HttpServletResponse response) throws java.io.IOException {
		if (code == null) {
			response.sendRedirect(clientUrl + "/login?error=naver");
			return;
		}
		try {
			NaverOAuthClient.Profile profile = naverOAuthClient.authenticate(code, state);
			String email = profile.email() != null ? profile.email() : "naver_" + profile.naverId() + "@naver.taedibear.local";
			String name = profile.name() != null ? profile.name() : "Taedibear User";
			User user = authService.findOrCreateNaverUser(profile.naverId(), email, name);
			response.sendRedirect(clientUrl + "/auth?token=" + authService.generateToken(user));
		} catch (Exception ex) {
			log.error("[Naver login error]", ex);
			response.sendRedirect(clientUrl + "/login?error=naver");
		}
	}
}
