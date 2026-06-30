package com.taedibear.studio.instagram;

import com.taedibear.studio.common.ApiResponse;
import com.taedibear.studio.domain.InstagramAccount;
import com.taedibear.studio.instagram.dto.InstagramAccountResponse;
import com.taedibear.studio.security.UserPrincipal;
import com.taedibear.studio.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// docs/05_API명세서.md 2. 인스타그램 연동 API
// /connect, /callback은 브라우저 전체 페이지 리다이렉트라 Authorization 헤더 대신
// JWT를 ?token= 쿼리/state 파라미터로 직접 주고받는다 (Node 버전과 동일한 방식).
@Slf4j
@RestController
@RequestMapping("/api/instagram")
@RequiredArgsConstructor
public class InstagramController {

	private final MetaApiClient metaApiClient;
	private final InstagramAccountService instagramAccountService;
	private final JwtTokenProvider jwtTokenProvider;

	@Value("${app.client-url}")
	private String clientUrl;

	@GetMapping("/connect")
	public void connect(@RequestParam(required = false) String token, HttpServletResponse response) throws IOException {
		if (token == null) {
			response.setStatus(401);
			response.getWriter().write("{\"success\":false,\"error\":\"토큰이 필요해요.\"}");
			return;
		}
		try {
			jwtTokenProvider.parseClaims(token);
		} catch (JwtException | IllegalArgumentException ex) {
			response.setStatus(401);
			response.getWriter().write("{\"success\":false,\"error\":\"유효하지 않은 토큰이에요.\"}");
			return;
		}
		response.sendRedirect(metaApiClient.getLoginUrl(token));
	}

	@GetMapping("/callback")
	public void callback(
			@RequestParam(required = false) String code,
			@RequestParam(required = false) String state,
			HttpServletResponse response) throws IOException {
		if (code == null || state == null) {
			response.sendRedirect(clientUrl + "/settings?connected=false");
			return;
		}
		try {
			Long userId = jwtTokenProvider.getUserId(jwtTokenProvider.parseClaims(state));

			String shortLivedToken = metaApiClient.exchangeCodeForToken(code);
			MetaApiClient.LongLivedToken longLived = metaApiClient.getLongLivedToken(shortLivedToken);
			MetaApiClient.InstagramBusinessAccount igAccount =
					metaApiClient.getInstagramBusinessAccount(longLived.accessToken());

			LocalDateTime expiresAt = longLived.expiresIn() != null
					? LocalDateTime.now().plusSeconds(longLived.expiresIn())
					: null;

			instagramAccountService.upsert(userId, igAccount.instagramUserId(), igAccount.username(),
					longLived.accessToken(), expiresAt);

			response.sendRedirect(clientUrl + "/settings?connected=true");
		} catch (Exception ex) {
			log.error("[Meta connect error]", ex);
			response.sendRedirect(clientUrl + "/settings?connected=false");
		}
	}

	// GET /api/instagram/accounts
	@GetMapping("/accounts")
	public ApiResponse<List<InstagramAccountResponse>> listAccounts(@AuthenticationPrincipal UserPrincipal principal) {
		return ApiResponse.ok(instagramAccountService.listAccounts(principal.getId()));
	}

	// DELETE /api/instagram/accounts/:id
	@DeleteMapping("/accounts/{id}")
	public Map<String, Boolean> disconnect(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
		instagramAccountService.disconnect(id, principal.getId());
		return Map.of("success", true);
	}
}
