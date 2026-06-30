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

	public GeminiService(
			WebClient.Builder webClientBuilder,
			ObjectMapper objectMapper,
			@Value("${app.gemini.api-key}") String apiKey,
			@Value("${app.gemini.model}") String model) {
		this.webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
		this.objectMapper = objectMapper;
		this.apiKey = apiKey;
		this.model = model;
	}

	public record CaptionResult(String caption, List<String> hashtags) {
	}

	public CaptionResult generateCaption(String imageUrl, String businessType, String mood) {
		byte[] imageBytes = downloadImage(imageUrl);
		String mimeType = guessMimeType(imageUrl);
		String base64Image = Base64.getEncoder().encodeToString(imageBytes);

		String prompt = "업종: " + businessType + "\n분위기: " + mood + "\n위 정보와 이미지를 참고해서 캡션과 해시태그 10개를 만들어줘.";

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
