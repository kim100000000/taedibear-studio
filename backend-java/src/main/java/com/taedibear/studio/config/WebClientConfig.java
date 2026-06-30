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
}
