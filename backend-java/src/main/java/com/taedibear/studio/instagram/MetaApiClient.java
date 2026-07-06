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
	// Phase 4-2/4-3: 인사이트 조회(instagram_manage_insights), 댓글 답글(instagram_manage_comments) 권한 추가.
	// 주의: 이 권한들은 Meta 앱 대시보드 "Facebook 로그인이 포함된 API 설정 > 권한 및 기능"에
	// 먼저 추가돼 있어야 하고, 기존에 연동된 계정은 이 권한 없이 발급된 토큰이므로
	// 재연동(계정 재연결)해야 인사이트/댓글 기능을 쓸 수 있다.
	public String getLoginUrl(String state) {
		return UriComponentsBuilder
				.fromHttpUrl("https://www.facebook.com/" + graphVersion + "/dialog/oauth")
				.queryParam("client_id", appId)
				.queryParam("redirect_uri", redirectUri)
				.queryParam("scope", String.join(",", List.of(
						"instagram_basic", "instagram_content_publish",
						"pages_show_list", "pages_read_engagement", "business_management",
						"instagram_manage_insights", "instagram_manage_comments")))
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

	// ── Phase 4-2: 분석 대시보드 고도화 (Meta Graph API 인사이트) ──────────────────

	/** IG 계정의 현재 팔로워 수 (instagram_manage_insights 권한 필요) */
	public long getFollowersCount(String igUserId, String accessToken) {
		JsonNode res = webClient.get()
				.uri(uriBuilder -> UriComponentsBuilder.fromHttpUrl(baseUrl + "/" + igUserId)
						.queryParam("fields", "followers_count")
						.queryParam("access_token", accessToken)
						.build()
						.toUri())
				.retrieve()
				.bodyToMono(JsonNode.class)
				.block();
		return res.path("followers_count").asLong(0);
	}

	public record MediaInsights(long engagement, long impressions, long reach, long likeCount, long commentsCount) {
	}

	/**
	 * 게시물(미디어) 하나의 인사이트를 조회한다.
	 * engagement/impressions/reach는 /insights 엔드포인트, like_count/comments_count는 미디어 객체 필드에서 가져온다.
	 */
	public MediaInsights getMediaInsights(String mediaId, String accessToken) {
		JsonNode mediaRes = webClient.get()
				.uri(uriBuilder -> UriComponentsBuilder.fromHttpUrl(baseUrl + "/" + mediaId)
						.queryParam("fields", "like_count,comments_count")
						.queryParam("access_token", accessToken)
						.build()
						.toUri())
				.retrieve()
				.bodyToMono(JsonNode.class)
				.block();

		JsonNode insightsRes;
		try {
			insightsRes = webClient.get()
					.uri(uriBuilder -> UriComponentsBuilder.fromHttpUrl(baseUrl + "/" + mediaId + "/insights")
							.queryParam("metric", "engagement,impressions,reach")
							.queryParam("access_token", accessToken)
							.build()
							.toUri())
					.retrieve()
					.bodyToMono(JsonNode.class)
					.block();
		} catch (Exception ex) {
			// 스토리 등 일부 미디어 타입은 insights 메트릭이 다르거나 만료되어 조회가 실패할 수 있다.
			insightsRes = null;
		}

		long engagement = 0, impressions = 0, reach = 0;
		if (insightsRes != null) {
			for (JsonNode metric : insightsRes.path("data")) {
				String name = metric.path("name").asText("");
				long value = metric.path("values").isArray() && metric.path("values").size() > 0
						? metric.path("values").get(0).path("value").asLong(0) : 0;
				switch (name) {
					case "engagement" -> engagement = value;
					case "impressions" -> impressions = value;
					case "reach" -> reach = value;
					default -> { }
				}
			}
		}

		return new MediaInsights(engagement, impressions, reach,
				mediaRes.path("like_count").asLong(0), mediaRes.path("comments_count").asLong(0));
	}

	// ── Phase 4-3: 리뷰(댓글) 자동 답글 ────────────────────────────────────────────

	public record MediaSummary(String id, String caption) {
	}

	/** 최근 게시물 목록 (댓글 조회 대상 후보) */
	public List<MediaSummary> getRecentMedia(String igUserId, String accessToken, int limit) {
		JsonNode res = webClient.get()
				.uri(uriBuilder -> UriComponentsBuilder.fromHttpUrl(baseUrl + "/" + igUserId + "/media")
						.queryParam("fields", "id,caption")
						.queryParam("limit", limit)
						.queryParam("access_token", accessToken)
						.build()
						.toUri())
				.retrieve()
				.bodyToMono(JsonNode.class)
				.block();

		List<MediaSummary> result = new java.util.ArrayList<>();
		for (JsonNode media : res.path("data")) {
			result.add(new MediaSummary(media.path("id").asText(), media.path("caption").asText(null)));
		}
		return result;
	}

	public record Comment(String id, String text, String username, String timestamp, String mediaId) {
	}

	/** 특정 게시물(미디어)에 달린 댓글 목록 */
	public List<Comment> getComments(String mediaId, String accessToken) {
		JsonNode res = webClient.get()
				.uri(uriBuilder -> UriComponentsBuilder.fromHttpUrl(baseUrl + "/" + mediaId + "/comments")
						.queryParam("fields", "id,text,username,timestamp")
						.queryParam("access_token", accessToken)
						.build()
						.toUri())
				.retrieve()
				.bodyToMono(JsonNode.class)
				.block();

		List<Comment> result = new java.util.ArrayList<>();
		for (JsonNode c : res.path("data")) {
			result.add(new Comment(c.path("id").asText(), c.path("text").asText(""),
					c.path("username").asText(null), c.path("timestamp").asText(null), mediaId));
		}
		return result;
	}

	/** 댓글에 답글을 단다 (instagram_manage_comments 권한 필요) */
	public String replyToComment(String commentId, String accessToken, String message) {
		JsonNode res = webClient.post()
				.uri(baseUrl + "/" + commentId + "/replies")
				.bodyValue(Map.of("message", message, "access_token", accessToken))
				.retrieve()
				.bodyToMono(JsonNode.class)
				.block();
		return res.get("id").asText();
	}
}
