package com.taedibear.studio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

// Meta Graph API, Gemini API, Google/Kakao/Naver OAuth 호출에 공통으로 사용하는 WebClient.
@Configuration
public class WebClientConfig {

	@Bean
	public WebClient.Builder webClientBuilder() {
		return WebClient.builder();
	}

	// PaymentService처럼 WebClient를 직접 주입받는 빈을 위한 기본 인스턴스.
	// (GeminiService 등은 Builder를 주입받아 baseUrl을 붙여 따로 build해서 사용)
	@Bean
	public WebClient webClient(WebClient.Builder builder) {
		return builder.build();
	}
}
