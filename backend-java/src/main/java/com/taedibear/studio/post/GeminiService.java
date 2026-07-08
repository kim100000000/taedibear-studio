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
	private final S3Service s3Service;

	public GeminiService(
			WebClient.Builder webClientBuilder,
			ObjectMapper objectMapper,
			@Value("${app.gemini.api-key}") String apiKey,
			@Value("${app.gemini.model}") String model,
			S3Service s3Service) {
		this.webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
		this.objectMapper = objectMapper;
		this.apiKey = apiKey;
		this.model = model;
		this.s3Service = s3Service;
	}

	public record CaptionResult(String caption, List<String> hashtags) {
	}

	public CaptionResult generateCaption(String imageUrl, String businessType, String mood,
	                                     String specialMenu, String eventPromotion, String keywords) {
		// C7: SSRF 차단 — 임의 URL(내부망, 클라우드 메타데이터 엔드포인트 등)을 서버가
		// 대신 다운로드하는 것을 막는다. 업로드 API가 발급한 자사 S3 URL만 허용.
		// URL 형식 검증 및 실제 다운로드는 S3Service.downloadImage에서 처리 (자격증명으로 직접 조회,
		// 버킷이 public-read가 아니어도 동작 — 예전엔 공개 URL로 재요청하다 403으로 500 나던 버그 수정).
		byte[] imageBytes = s3Service.downloadImage(imageUrl);
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

	// Phase 4-3: 리뷰(댓글) 자동 답글 생성 — 이미지 없이 텍스트만으로 호출
	private static final String REPLY_SYSTEM_INSTRUCTION =
			"당신은 소상공인을 대신해 인스타그램 댓글에 답글을 다는 SNS 매니저입니다. " +
					"고객이 남긴 댓글에 친절하고 짧게(1~2문장) 한국어로 답글을 작성하세요. " +
					"반드시 다른 설명 없이 아래 형식의 순수 JSON으로만 응답하세요: " +
					"{\"reply\": \"답글 내용\"}";

	public String generateCommentReply(String commentText, String businessType) {
		String prompt = "업종: " + (businessType == null || businessType.isBlank() ? "가게" : businessType) + "\n" +
				"고객 댓글: " + commentText + "\n" +
				"위 댓글에 대한 답글을 만들어줘.";

		Map<String, Object> requestBody = Map.of(
				"systemInstruction", Map.of("parts", List.of(Map.of("text", REPLY_SYSTEM_INSTRUCTION))),
				"contents", List.of(Map.of(
						"role", "user",
						"parts", List.of(Map.of("text", prompt))
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
			log.error("[Gemini reply error] status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
			if (ex.getStatusCode() == HttpStatusCode.valueOf(429)) {
				throw ApiException.tooManyRequests("잠시 후 다시 시도해주세요.");
			}
			throw ApiException.internal("AI 답글 생성에 실패했어요.");
		}

		try {
			String text = response
					.path("candidates").get(0)
					.path("content").path("parts").get(0)
					.path("text").asText();
			String cleaned = text.replaceAll("```json\\s*|```\\s*", "").trim();
			JsonNode parsed = objectMapper.readTree(cleaned);
			return parsed.path("reply").asText(null);
		} catch (Exception ex) {
			log.error("[Gemini reply parse error]", ex);
			throw ApiException.internal("AI 답글 생성에 실패했어요.");
		}
	}

	private String guessMimeType(String imageUrl) {
		String lower = imageUrl.toLowerCase();
		if (lower.endsWith(".png")) return "image/png";
		if (lower.endsWith(".webp")) return "image/webp";
		return "image/jpeg";
	}
}
