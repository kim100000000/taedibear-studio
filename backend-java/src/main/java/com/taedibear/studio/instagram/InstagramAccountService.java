package com.taedibear.studio.instagram;

import com.taedibear.studio.common.exception.ApiException;
import com.taedibear.studio.domain.InstagramAccount;
import com.taedibear.studio.instagram.dto.InstagramAccountResponse;
import com.taedibear.studio.repository.InstagramAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InstagramAccountService {

	private final InstagramAccountRepository instagramAccountRepository;

	@Transactional
	public InstagramAccount upsert(Long userId, String instagramUserId, String username,
			String accessToken, LocalDateTime tokenExpiresAt) {
		InstagramAccount account = instagramAccountRepository
				.findByUserIdAndInstagramUserId(userId, instagramUserId)
				.orElseGet(() -> InstagramAccount.builder().userId(userId).instagramUserId(instagramUserId).build());

		account.setUsername(username);
		account.setAccessToken(accessToken);
		account.setTokenExpiresAt(tokenExpiresAt);
		return instagramAccountRepository.save(account);
	}

	public List<InstagramAccountResponse> listAccounts(Long userId) {
		return instagramAccountRepository.findAllByUserId(userId).stream()
				.map(a -> new InstagramAccountResponse(a.getId(), a.getInstagramUserId(), a.getUsername(), a.getConnectedAt()))
				.toList();
	}

	@Transactional
	public void disconnect(Long accountId, Long userId) {
		InstagramAccount account = instagramAccountRepository.findById(accountId)
				.orElseThrow(() -> ApiException.notFound("계정을 찾을 수 없어요."));
		if (!account.getUserId().equals(userId)) {
			throw ApiException.forbidden("본인 계정만 해제할 수 있어요.");
		}
		instagramAccountRepository.delete(account);
	}
}
