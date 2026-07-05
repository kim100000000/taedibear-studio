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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Duration;
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

	// C5: 운영(HTTPS)에서는 true로 설정해 state 쿠키를 Secure로 발급. 로컬(http)은 false.
	@Value("${app.oauth.cookie-secure:false}")
	private boolean cookieSecure;

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final String STATE_COOKIE = "oauth_state";

	private String randomState() {
		byte[] bytes = new byte[16];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	// C5: OAuth state를 생성해 HttpOnly 쿠키로 심고 그 값을 반환한다.
	// 콜백에서 쿼리의 state와 이 쿠키를 대조해 CSRF(공격자 계정 강제 연결)를 차단한다.
	private String issueOAuthState(HttpServletResponse response) {
		String state = randomState();
		ResponseCookie cookie = ResponseCookie.from(STATE_COOKIE, state)
				.httpOnly(true)
				.secure(cookieSecure)
				.path("/")
				.sameSite("Lax")
				.maxAge(Duration.ofMinutes(5))
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
		return state;
	}

	// C5: 쿠키의 state와 콜백으로 돌아온 state 파라미터를 대조하고, 검증 후 쿠키를 삭제한다.
	private boolean isValidOAuthState(HttpServletRequest request, HttpServletResponse response, String stateParam) {
		String cookieState = null;
		if (request.getCookies() != null) {
			for (Cookie c : request.getCookies()) {
				if (STATE_COOKIE.equals(c.getName())) {
					cookieState = c.getValue();
					break;
				}
			}
		}
		// 일회성: 검증 여부와 무관하게 쿠키를 즉시 만료시켜 재사용을 막는다.
		ResponseCookie cleared = ResponseCookie.from(STATE_COOKIE, "")
				.httpOnly(true).secure(cookieSecure).path("/").sameSite("Lax").maxAge(0).build();
		response.addHeader(HttpHeaders.SET_COOKIE, cleared.toString());

		return stateParam != null && !stateParam.isBlank()
				&& cookieState != null && cookieState.equals(stateParam);
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
		response.sendRedirect(googleOAuthClient.getLoginUrl(issueOAuthState(response)));
	}

	// GET /api/auth/google/callback
	@GetMapping("/google/callback")
	public void googleCallback(
			@RequestParam(required = false) String code,
			@RequestParam(required = false) String state,
			HttpServletRequest request,
			HttpServletResponse response) throws java.io.IOException {
		if (code == null || !isValidOAuthState(request, response, state)) {
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
		response.sendRedirect(kakaoOAuthClient.getLoginUrl(issueOAuthState(response)));
	}

	// GET /api/auth/kakao/callback
	@GetMapping("/kakao/callback")
	public void kakaoCallback(
			@RequestParam(required = false) String code,
			@RequestParam(required = false) String state,
			HttpServletRequest request,
			HttpServletResponse response) throws java.io.IOException {
		if (code == null || !isValidOAuthState(request, response, state)) {
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
		response.sendRedirect(naverOAuthClient.getLoginUrl(issueOAuthState(response)));
	}

	// GET /api/auth/naver/callback
	@GetMapping("/naver/callback")
	public void naverCallback(
			@RequestParam(required = false) String code,
			@RequestParam(required = false) String state,
			HttpServletRequest request,
			HttpServletResponse response) throws java.io.IOException {
		if (code == null || !isValidOAuthState(request, response, state)) {
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
