package com.taedibear.studio.instagram;

import com.fasterxml.jackson.databind.JsonNode;
import com.taedibear.studio.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

// Node 버전 services/meta.service.js를 그대로 포팅한 구현.
@Component
public class MetaApiClient {

	private final WebClient webClient;
	private final String appId;
	private final String appSecret;
	private final String redirectUri;
	private final String graphVersion;
	private final String baseUrl;

	public MetaApiClient(
			WebClient.Builder webClientBuilder,
			@Value("${app.meta.app-id}") String appId,
			@Value("${app.meta.app-secret}") String appSecret,
			@Value("${app.meta.redirect-uri}") String redirectUri,
			@Value("${app.meta.graph-api-version}") String graphVersion) {
		this.webClient = webClientBuilder.build();
		this.appId = appId;
		this.appSecret = appSecret;
		this.redirectUri = redirectUri;
		this.graphVersion = graphVersion;
		this.baseUrl = "https://graph.facebook.com/" + graphVersion;
	}

	// docs/05_API명세서.md: GET /api/instagram/connect
	public String getLoginUrl(String state) {
		return UriComponentsBuilder
				.fromHttpUrl("https://www.facebook.com/" + graphVersion + "/dialog/oauth")
				.queryParam("client_id", appId)
				.queryParam("redirect_uri", redirectUri)
				.queryParam("scope", String.join(",", List.of(
						"instagram_basic", "instagram_content_publish",
						"pages_show_list", "pages_read_engagement", "business_management")))
				.queryParam("response_type", "code")
				.queryParam("state", state)
				.encode(StandardCharsets.UTF_8)
				.build()
				.toUriString();
	}

	public String exchangeCodeForToken(String code) {
		JsonNode res = webClient.get()
				.uri(uriBuilder -> UriComponentsBuilder.fromHttpUrl(baseUrl + "/oauth/access_token")
						.queryParam("client_id", appId)
						.queryParam("client_secret", appSecret)
						.queryParam("redirect_uri", redirectUri)
						.queryParam("code", code)
						.build()
						.toUri())
				.retrieve()
				.bodyToMono(JsonNode.class)
				.block();
		return res.get("access_token").asText();
	}

	public record LongLivedToken(String accessToken, Long expiresIn) {
	}

	public LongLivedToken getLongLivedToken(String shortLivedToken) {
		JsonNode res = webClient.get()
				.uri(uriBuilder -> UriComponentsBuilder.fromHttpUrl(baseUrl + "/oauth/access_token")
						.queryParam("grant_type", "fb_exchange_token")
						.queryParam("client_id", appId)
						.queryParam("client_secret", appSecret)
						.queryParam("fb_exchange_token", shortLivedToken)
						.build()
						.toUri())
				.retrieve()
				.bodyToMono(JsonNode.class)
				.block();
		Long expiresIn = res.hasNonNull("expires_in") ? res.get("expires_in").asLong() : null;
		return new LongLivedToken(res.get("access_token").asText(), expiresIn);
	}

	public record InstagramBusinessAccount(String instagramUserId, String username) {
	}

	public InstagramBusinessAccount getInstagramBusinessAccount(String accessToken) {
		JsonNode res = webClient.get()
				.uri(uriBuilder -> UriComponentsBuilder.fromHttpUrl(baseUrl + "/me/accounts")
						.queryParam("access_token", accessToken)
						.queryParam("fields", "instagram_business_account{id,username}")
						.build()
						.toUri())
				.retrieve()
				.bodyToMono(JsonNode.class)
				.block();

		for (JsonNode page : res.path("data")) {
			JsonNode ig = page.get("instagram_business_account");
			if (ig != null) {
				return new InstagramBusinessAccount(ig.get("id").asText(), ig.path("username").asText(null));
			}
		}
		throw ApiException.badRequest("연결된 페이지에 Instagram 비즈니스 계정이 없어요.");
	}

	/**
	 * Instagram Business 계정에 게시물을 발행한다. (1. 미디어 컨테이너 생성 -> 2. 게시)
	 */
	public String publishToInstagram(String igUserId, String accessToken, String imageUrl, String caption) {
		JsonNode createRes = webClient.post()
				.uri(baseUrl + "/" + igUserId + "/media")
				.bodyValue(Map.of("image_url", imageUrl, "caption", caption, "access_token", accessToken))
				.retrieve()
				.bodyToMono(JsonNode.class)
				.block();

		String creationId = createRes.get("id").asText();

		JsonNode publishRes = webClient.post()
				.uri(baseUrl + "/" + igUserId + "/media_publish")
				.bodyValue(Map.of("creation_id", creationId, "access_token", accessToken))
				.retrieve()
				.bodyToMono(JsonNode.class)
				.block();

		return publishRes.get("id").asText();
	}
}
