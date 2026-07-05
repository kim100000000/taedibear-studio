package com.taedibear.studio.instagram;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * C6: Instagram 연동 OAuth state 관리.
 * 기존에는 Meta state 파라미터에 JWT를 그대로 실어 URL(히스토리·로그·프록시)에 노출됐다.
 * 이제 랜덤 nonce를 발급해 userId와 매핑해 두고, 콜백에서 nonce로 사용자를 복원한다 (일회성, TTL 10분).
 *
 * 저장소는 인메모리 — 단일 인스턴스 배포 전제. 연동 도중 서버가 재시작되면 사용자가
 * 연동을 한 번 더 시도하면 된다. 다중 인스턴스 확장 시 DB/Redis로 교체 필요.
 */
@Service
public class ConnectStateService {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final long TTL_SECONDS = 600; // 10분

	private record Entry(Long userId, Instant expiresAt) {
	}

	private final Map<String, Entry> states = new ConcurrentHashMap<>();

	// nonce 발급 + userId 매핑 저장
	public String issue(Long userId) {
		purgeExpired();
		byte[] bytes = new byte[16];
		RANDOM.nextBytes(bytes);
		String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		states.put(nonce, new Entry(userId, Instant.now().plusSeconds(TTL_SECONDS)));
		return nonce;
	}

	// 콜백에서 nonce 검증·소비 (일회성). 유효하지 않으면 null.
	public Long consume(String nonce) {
		if (nonce == null || nonce.isBlank()) return null;
		Entry entry = states.remove(nonce);
		if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
			return null;
		}
		return entry.userId();
	}

	private void purgeExpired() {
		Instant now = Instant.now();
		states.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
	}
}
