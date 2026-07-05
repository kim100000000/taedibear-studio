package com.taedibear.studio.config;

import com.taedibear.studio.security.jwt.JwtAuthenticationEntryPoint;
import com.taedibear.studio.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	// C3: CORS 허용 오리진 — 프론트엔드 주소만 허용 (개발: http://localhost:5173, 운영: CLIENT_URL)
	@Value("${app.client-url}")
	private String clientUrl;

	// bcrypt: Node 버전(bcryptjs, salt rounds 10)과 동일한 알고리즘.
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(10);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/health").permitAll()
						.requestMatchers("/api/auth/**").permitAll()
						// C6: /callback은 Meta가 브라우저 리다이렉트로 호출 — state nonce로 사용자를 복원하므로 permitAll.
						// (기존 /connect?token= 엔드포인트는 제거됨 — /connect-url 은 일반 JWT 인증을 탄다)
						.requestMatchers("/api/instagram/callback").permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		// C3: 와일드카드("*") + allowCredentials(true) 조합은 어떤 사이트든 인증 요청을
		// 위조할 수 있게 하므로 금지. 프론트엔드 오리진만 명시적으로 허용한다.
		configuration.setAllowedOrigins(List.of(clientUrl));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
