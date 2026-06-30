package com.taedibear.studio.auth.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;

// Node 버전 services/naver.service.js를 그대로 포팅한 구현 (axios -> WebClient).
@Component
public class NaverOAuthClient {

	private static final String AUTH_URL = "https://nid.naver.com/oauth2.0/authorize";
	private static final String TOKEN_URL = "https://nid.naver.com/oauth2.0/token";
	private static final String USERINFO_URL = "https://openapi.naver.com/v1/nid/me";

	private final WebClient webClient;
	private final String clientId;
	private final String clientSecret;
	private final String redirectUri;

	public NaverOAuthClient(
			WebClient.Builder webClientBuilder,
			@Value("${app.oauth.naver.client-id}") String clientId,
			@Value("${app.oauth.naver.client-secret}") String clientSecret,
			@Value("${app.oauth.naver.redirect-uri}") String redirectUri) {
		this.webClient = webClientBuilder.build();
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.redirectUri = redirectUri;
	}

	public String getLoginUrl(String state) {
		return UriComponentsBuilder.fromHttpUrl(AUTH_URL)
				.queryParam("response_type", "code")
				.queryParam("client_id", clientId)
				.queryParam("redirect_uri", redirectUri)
				.queryParam("state", state)
				.encode(StandardCharsets.UTF_8)
				.build()
				.toUriString();
	}

	public record Profile(String naverId, String email, String name) {
	}

	public Profile authenticate(String code, String state) {
		JsonNode tokenRes = webClient.get()
				.uri(uriBuilder -> UriComponentsBuilder.fromHttpUrl(TOKEN_URL)
						.queryParam("grant_type", "authorization_code")
						.queryParam("client_id", clientId)
						.queryParam("client_secret", clientSecret)
						.queryParam("code", code)
						.queryParam("state", state)
						.build()
						.toUri())
				.retrieve()
				.bodyToMono(JsonNode.class)
				.block();

		String accessToken = tokenRes.get("access_token").asText();

		JsonNode body = webClient.get()
				.uri(USERINFO_URL)
				.headers(h -> h.setBearerAuth(accessToken))
				.retrieve()
				.bodyToMono(JsonNode.class)
				.block();

		JsonNode response = body.get("response");
		String naverId = response.get("id").asText();
		String email = response.hasNonNull("email") ? response.get("email").asText() : null;
		String name = response.hasNonNull("name") ? response.get("name").asText() : null;

		return new Profile(naverId, email, name);
	}
}
