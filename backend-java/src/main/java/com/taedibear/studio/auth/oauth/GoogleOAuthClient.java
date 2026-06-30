package com.taedibear.studio.auth.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Map;

// Google OAuth2를 spring-oauth2-client 없이 WebClient로 직접 구현한다.
// (Node 버전의 passport-google-oauth20과 동일한 scope: profile, email)
@Component
public class GoogleOAuthClient {

	private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
	private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
	private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

	private final WebClient webClient;
	private final String clientId;
	private final String clientSecret;
	private final String redirectUri;

	public GoogleOAuthClient(
			WebClient.Builder webClientBuilder,
			@Value("${app.oauth.google.client-id}") String clientId,
			@Value("${app.oauth.google.client-secret}") String clientSecret,
			@Value("${app.oauth.google.redirect-uri}") String redirectUri) {
		this.webClient = webClientBuilder.build();
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.redirectUri = redirectUri;
	}

	public String getLoginUrl(String state) {
		return UriComponentsBuilder.fromHttpUrl(AUTH_URL)
				.queryParam("client_id", clientId)
				.queryParam("redirect_uri", redirectUri)
				.queryParam("response_type", "code")
				.queryParam("scope", "profile email")
				.queryParam("state", state)
				.encode(StandardCharsets.UTF_8)
				.build()
				.toUriString();
	}

	public record Profile(String googleId, String email, String name) {
	}

	public Profile authenticate(String code) {
		JsonNode tokenRes = webClient.post()
				.uri(TOKEN_URL)
				.bodyValue(Map.of(
						"client_id", clientId,
						"client_secret", clientSecret,
						"redirect_uri", redirectUri,
						"grant_type", "authorization_code",
						"code", code))
				.retrieve()
				.bodyToMono(JsonNode.class)
				.block();

		String accessToken = tokenRes.get("access_token").asText();

		JsonNode profile = webClient.get()
				.uri(USERINFO_URL)
				.headers(h -> h.setBearerAuth(accessToken))
				.retrieve()
				.bodyToMono(JsonNode.class)
				.block();

		return new Profile(
				profile.get("sub").asText(),
				profile.hasNonNull("email") ? profile.get("email").asText() : null,
				profile.hasNonNull("name") ? profile.get("name").asText() : "Taedibear User");
	}
}
