package com.taedibear.studio.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taedibear.studio.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Google Gemini REST API(generateContent)로 이미지 기반 캡션/해시태그를 생성한다.
 * Node 버전(services/gemini.service.js, @google/generative-ai SDK)과 동일한 동작을 REST 호출로 구현한다.
 * docs/05_API명세서.md: POST /api/posts/caption, 모델 gemini-2.5-flash, 무료 한도 분당 15회.
 */
@Slf4j
@Service
public class GeminiService {

	private static final String SYSTEM_INSTRUCTION =
			"당신은 소상공인을 위한 SNS 마케팅 카피라이터입니다. 첨부된 이미지를 보고 매력적인 인스타그램 게시물 캡션과 해시태그를 작성하세요. " +
					"반드시 다른 설명 없이 아래 형식의 순수 JSON으로만 응답하세요: " +
					"{\"caption\": \"게시물 캡션 (이모지 포함 가능, 2~4문장)\", \"hashtags\": [\"#태그1\", \"#태그2\", ... 10개]}";

	private final WebClient webClient;
	private final ObjectMapper objectMapper;
	private final String apiKey;
	private final String model;

	// C7: 서버가 다운로드를 허용하는 이미지 URL prefix — 자사 S3 버킷만
	private final String allowedImageUrlPrefix;

	public GeminiService(
			WebClient.Builder webClientBuilder,
			ObjectMapper objectMapper,
			@Value("${app.gemini.api-key}") String apiKey,
			@Value("${app.gemini.model}") String model,
			@Value("${app.aws.s3-bucket}") String s3Bucket,
			@Value("${app.aws.region}") String awsRegion) {
		this.webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
		this.objectMapper = objectMapper;
		this.apiKey = apiKey;
		this.model = model;
		// S3Service.uploadImage가 만드는 URL 형식과 동일하게 유지
		this.allowedImageUrlPrefix = String.format("https://%s.s3.%s.amazonaws.com/", s3Bucket, awsRegion);
	}

	public record CaptionResult(String caption, List<String> hashtags) {
	}

	public CaptionResult generateCaption(String imageUrl, String businessType, String mood,
	                                     String specialMenu, String eventPromotion, String keywords) {
		// C7: SSRF 차단 — 임의 URL(내부망, 클라우드 메타데이터 엔드포인트 등)을 서버가
		// 대신 다운로드하는 것을 막는다. 업로드 API가 발급한 자사 S3 URL만 허용.
		if (imageUrl == null || !imageUrl.startsWith(allowedImageUrlPrefix)) {
			log.warn("[Gemini] 허용되지 않은 image_url 차단: {}", imageUrl);
			throw ApiException.badRequest("이미지 URL이 올바르지 않아요. 업로드 후 발급된 이미지 주소를 사용해주세요.");
		}

		byte[] imageBytes = downloadImage(imageUrl);
		String mimeType = guessMimeType(imageUrl);
		String base64Image = Base64.getEncoder().encodeToString(imageBytes);

		// Phase 2-3: 선택 입력 필드를 프롬프트에 추가
		StringBuilder promptBuilder = new StringBuilder();
		promptBuilder.append("업종: ").append(businessType).append("\n");
		promptBuilder.append("분위기: ").append(mood).append("\n");
		if (specialMenu != null && !specialMenu.isBlank()) {
			promptBuilder.append("오늘의 특별 메뉴: ").append(specialMenu).append("\n");
		}
		if (eventPromotion != null && !eventPromotion.isBlank()) {
			promptBuilder.append("이벤트/프로모션: ").append(eventPromotion).append("\n");
		}
		if (keywords != null && !keywords.isBlank()) {
			promptBuilder.append("강조할 키워드: ").append(keywords).append("\n");
		}
		promptBuilder.append("위 정보와 이미지를 참고해서 캡션과 해시태그 10개를 만들어줘.");
		String prompt = promptBuilder.toString();

		Map<String, Object> requestBody = Map.of(
				"systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM_INSTRUCTION))),
				"contents", List.of(Map.of(
						"role", "user",
						"parts", List.of(
								Map.of("text", prompt),
								Map.of("inlineData", Map.of("mimeType", mimeType, "data", base64Image))
						)
				))
		);

		JsonNode response;
		try {
			response = webClient.post()
					.uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
					.bodyValue(requestBody)
					.retrieve()
					.bodyToMono(JsonNode.class)
					.block();
		} catch (WebClientResponseException ex) {
			log.error("[Gemini caption error] status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
			if (ex.getStatusCode() == HttpStatusCode.valueOf(429)) {
				throw ApiException.tooManyRequests("잠시 후 다시 시도해주세요.");
			}
			throw ApiException.internal("AI 캡션 생성에 실패했어요.");
		}

		try {
			String text = response
					.path("candidates").get(0)
					.path("content").path("parts").get(0)
					.path("text").asText();

			String cleaned = text.replaceAll("```json\\s*|```\\s*", "").trim();
			JsonNode parsed = objectMapper.readTree(cleaned);

			String caption = parsed.path("caption").asText(null);
			List<String> hashtags = new ArrayList<>();
			if (parsed.has("hashtags") && parsed.get("hashtags").isArray()) {
				parsed.get("hashtags").forEach(node -> hashtags.add(node.asText()));
			}
			return new CaptionResult(caption, hashtags);
		} catch (Exception ex) {
			log.error("[Gemini caption parse error]", ex);
			throw ApiException.internal("AI 캡션 생성에 실패했어요.");
		}
	}

	private byte[] downloadImage(String imageUrl) {
		return webClient.get()
				.uri(imageUrl)
				.retrieve()
				.bodyToMono(byte[].class)
				.block();
	}

	private String guessMimeType(String imageUrl) {
		String lower = imageUrl.toLowerCase();
		if (lower.endsWith(".png")) return "image/png";
		if (lower.endsWith(".webp")) return "image/webp";
		return "image/jpeg";
	}
}
