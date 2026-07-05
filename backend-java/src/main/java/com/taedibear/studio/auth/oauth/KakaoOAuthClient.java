package com.taedibear.studio.auth.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;

// 카카오는 REST API 키만 사용하는 클라이언트 시크릿 미사용 앱도 많아 client-secret 없이 구현한다.
// (Node 버전의 passport-kakao와 동일한 흐름)
@Component
public class KakaoOAuthClient {

	private static final String AUTH_URL = "https://kauth.kakao.com/oauth/authorize";
	private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
	private static final String USERINFO_URL = "https://kapi.kakao.com/v2/user/me";

	private final WebClient webClient;
	private final String clientId;
	private final String redirectUri;

	public KakaoOAuthClient(
			WebClient.Builder webClientBuilder,
			@Value("${app.oauth.kakao.client-id}") String clientId,
			@Value("${app.oauth.kakao.redirect-uri}") String redirectUri) {
		this.webClient = webClientBuilder.build();
		this.clientId = clientId;
		this.redirectUri = redirectUri;
	}

	public String getLoginUrl(String state) {
		return UriComponentsBuilder.fromHttpUrl(AUTH_URL)
				.queryParam("client_id", clientId)
				.queryParam("redirect_uri", redirectUri)
				.queryParam("response_type", "code")
				.queryParam("state", state)
				.encode(StandardCharsets.UTF_8)
				.build()
				.toUriString();
	}

	public record Profile(String kakaoId, String email, String name) {
	}

	public Profile authenticate(String code) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", clientId);
		form.add("redirect_uri", redirectUri);
		form.add("code", code);

		JsonNode tokenRes = webClient.post()
				.uri(TOKEN_URL)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form)
				.retrieve()
				.onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
						response -> response.bodyToMono(String.class)
								.map(body -> new RuntimeException("카카오 토큰 요청 실패: " + body)))
				.bodyToMono(JsonNode.class)
				.block();

		if (tokenRes == null || !tokenRes.has("access_token")) {
			String error = tokenRes != null ? tokenRes.path("error").asText("unknown") : "null response";
			throw new RuntimeException("카카오 액세스 토큰 없음: " + error);
		}

		String accessToken = tokenRes.get("access_token").asText();

		JsonNode profile = webClient.get()
				.uri(USERINFO_URL)
				.headers(h -> h.setBearerAuth(accessToken))
				.retrieve()
				.bodyToMono(JsonNode.class)
				.block();

		String kakaoId = profile.get("id").asText();
		JsonNode account = profile.get("kakao_account");
		// 카카오 비즈니스 채널 연동 전에는 email scope가 제공되지 않을 수 있다 (docs/05_API명세서.md 참고).
		String email = (account != null && account.hasNonNull("email")) ? account.get("email").asText() : null;
		String name = (account != null && account.has("profile") && account.get("profile").hasNonNull("nickname"))
				? account.get("profile").get("nickname").asText()
				: "Taedibear User";

		return new Profile(kakaoId, email, name);
	}
}
