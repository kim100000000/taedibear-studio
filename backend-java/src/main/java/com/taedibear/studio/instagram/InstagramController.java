package com.taedibear.studio.instagram;

import com.taedibear.studio.common.ApiResponse;
import com.taedibear.studio.instagram.dto.InstagramAccountResponse;
import com.taedibear.studio.security.UserPrincipal;
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
// C6: 기존 GET /connect?token=<JWT> 방식(JWT URL 노출)을 제거하고,
// 인증된 GET /connect-url 이 랜덤 nonce state가 담긴 Meta 로그인 URL을 반환한다.
// 콜백은 nonce로 사용자를 복원하므로 URL에 JWT가 실리지 않는다.
@Slf4j
@RestController
@RequestMapping("/api/instagram")
@RequiredArgsConstructor
public class InstagramController {

	private final MetaApiClient metaApiClient;
	private final InstagramAccountService instagramAccountService;
	private final ConnectStateService connectStateService;

	@Value("${app.client-url}")
	private String clientUrl;

	// GET /api/instagram/connect-url — 인증 필요(Authorization 헤더).
	// 프론트가 이 URL을 받아 window.location으로 이동한다.
	@GetMapping("/connect-url")
	public ApiResponse<Map<String, String>> connectUrl(@AuthenticationPrincipal UserPrincipal principal) {
		String nonce = connectStateService.issue(principal.getId());
		return ApiResponse.ok(Map.of("url", metaApiClient.getLoginUrl(nonce)));
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
			// C6: state는 JWT가 아닌 일회성 nonce — 유효하지 않으면 연동 실패 처리
			Long userId = connectStateService.consume(state);
			if (userId == null) {
				log.warn("[Meta connect] 유효하지 않은 state로 콜백 수신");
				response.sendRedirect(clientUrl + "/settings?connected=false");
				return;
			}

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
