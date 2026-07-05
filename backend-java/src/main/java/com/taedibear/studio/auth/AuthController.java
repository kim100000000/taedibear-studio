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

	// C4: refresh token 쿠키 설정 — 운영에서 프론트/백엔드 도메인이 다르면 SameSite=None + Secure=true 필요
	@Value("${app.auth.cookie-secure:false}")
	private boolean authCookieSecure;

	@Value("${app.auth.cookie-same-site:Lax}")
	private String authCookieSameSite;

	@Value("${app.auth.refresh-expiration-ms}")
	private long refreshExpirationMs;

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final String STATE_COOKIE = "oauth_state";
	private static final String REFRESH_COOKIE = "refresh_token";

	// C4: refresh token을 HttpOnly 쿠키로 발급 — JS에서 접근 불가(XSS 탈취 방지),
	// path=/api/auth 로 제한해 다른 API 요청에는 실리지 않게 한다.
	private void issueRefreshCookie(HttpServletResponse response, String refreshToken) {
		ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, refreshToken)
				.httpOnly(true)
				.secure(authCookieSecure)
				.path("/api/auth")
				.sameSite(authCookieSameSite)
				.maxAge(Duration.ofMillis(refreshExpirationMs))
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	private void clearRefreshCookie(HttpServletResponse response) {
		ResponseCookie cleared = ResponseCookie.from(REFRESH_COOKIE, "")
				.httpOnly(true).secure(authCookieSecure).path("/api/auth")
				.sameSite(authCookieSameSite).maxAge(0).build();
		response.addHeader(HttpHeaders.SET_COOKIE, cleared.toString());
	}

	private String readRefreshCookie(HttpServletRequest request) {
		if (request.getCookies() == null) return null;
		for (Cookie c : request.getCookies()) {
			if (REFRESH_COOKIE.equals(c.getName())) {
				return c.getValue();
			}
		}
		return null;
	}

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
	// 실패 시 원인(쿠키 없음/파라미터 없음/불일치)을 로그로 남긴다 — 운영에서 error=<provider> 원인 추적용.
	private boolean isValidOAuthState(String provider, HttpServletRequest request, HttpServletResponse response, String stateParam) {
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

		boolean valid = stateParam != null && !stateParam.isBlank()
				&& cookieState != null && cookieState.equals(stateParam);
		if (!valid) {
			log.warn("[{} login] OAuth state 검증 실패 — state 파라미터 {}, oauth_state 쿠키 {}",
					provider,
					(stateParam == null || stateParam.isBlank()) ? "없음" : "있음",
					cookieState == null ? "없음(브라우저 쿠키 차단 또는 5분 초과 가능성)" : "불일치");
		}
		return valid;
	}

	// POST /api/auth/register — access token은 body, refresh token은 HttpOnly 쿠키
	@PostMapping("/register")
	public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
		AuthService.AuthResult result = authService.register(request);
		issueRefreshCookie(response, result.refreshToken());
		return ApiResponse.ok(result.response());
	}

	// POST /api/auth/login — access token은 body, refresh token은 HttpOnly 쿠키
	@PostMapping("/login")
	public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
		AuthService.AuthResult result = authService.login(request);
		issueRefreshCookie(response, result.refreshToken());
		return ApiResponse.ok(result.response());
	}

	// POST /api/auth/refresh — C4: refresh 쿠키를 검증·회전하고 새 access token을 발급한다.
	// (기존: 만료된 JWT도 서명만 맞으면 무기한 재발급 → 탈취 시 영구 유효 토큰이었음)
	@PostMapping("/refresh")
	public ApiResponse<?> refresh(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = readRefreshCookie(request);
		if (refreshToken == null) {
			throw ApiException.unauthorized("세션이 만료됐어요. 다시 로그인해주세요.");
		}
		AuthService.RefreshResult result = authService.refresh(refreshToken);
		issueRefreshCookie(response, result.newRefreshToken());
		return ApiResponse.ok(java.util.Map.of("token", result.accessToken()));
	}

	// POST /api/auth/logout — C4/M9: 서버측에서 refresh token을 폐기하고 쿠키를 삭제한다.
	@PostMapping("/logout")
	public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = readRefreshCookie(request);
		if (refreshToken != null) {
			authService.revokeRefreshToken(refreshToken);
		}
		clearRefreshCookie(response);
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
		if (code == null || !isValidOAuthState("google", request, response, state)) {
			response.sendRedirect(clientUrl + "/login?error=google");
			return;
		}
		try {
			GoogleOAuthClient.Profile profile = googleOAuthClient.authenticate(code);
			User user = authService.findOrCreateGoogleUser(profile.googleId(), profile.email(), profile.name());
			// C6: JWT를 URL에 노출하지 않는다 — refresh 쿠키만 심고 /auth 에서 토큰 교환
			issueRefreshCookie(response, authService.issueRefreshToken(user));
			response.sendRedirect(clientUrl + "/auth");
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
		if (code == null || !isValidOAuthState("kakao", request, response, state)) {
			response.sendRedirect(clientUrl + "/login?error=kakao");
			return;
		}
		try {
			KakaoOAuthClient.Profile profile = kakaoOAuthClient.authenticate(code);
			String email = profile.email() != null ? profile.email() : "kakao_" + profile.kakaoId() + "@kakao.taedibear.local";
			User user = authService.findOrCreateKakaoUser(profile.kakaoId(), email, profile.name());
			// C6: JWT를 URL에 노출하지 않는다 — refresh 쿠키만 심고 /auth 에서 토큰 교환
			issueRefreshCookie(response, authService.issueRefreshToken(user));
			response.sendRedirect(clientUrl + "/auth");
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
		if (code == null || !isValidOAuthState("naver", request, response, state)) {
			response.sendRedirect(clientUrl + "/login?error=naver");
			return;
		}
		try {
			NaverOAuthClient.Profile profile = naverOAuthClient.authenticate(code, state);
			String email = profile.email() != null ? profile.email() : "naver_" + profile.naverId() + "@naver.taedibear.local";
			String name = profile.name() != null ? profile.name() : "Taedibear User";
			User user = authService.findOrCreateNaverUser(profile.naverId(), email, name);
			// C6: JWT를 URL에 노출하지 않는다 — refresh 쿠키만 심고 /auth 에서 토큰 교환
			issueRefreshCookie(response, authService.issueRefreshToken(user));
			response.sendRedirect(clientUrl + "/auth");
		} catch (Exception ex) {
			log.error("[Naver login error]", ex);
			response.sendRedirect(clientUrl + "/login?error=naver");
		}
	}
}
