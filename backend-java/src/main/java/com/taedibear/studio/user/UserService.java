package com.taedibear.studio.user;

import com.taedibear.studio.common.exception.ApiException;
import com.taedibear.studio.domain.User;
import com.taedibear.studio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	public User getById(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> ApiException.notFound("사용자를 찾을 수 없어요."));
	}

	@Transactional
	public User updateName(Long id, String name) {
		User user = getById(id);
		if (name != null && !name.isBlank()) {
			user.setName(name);
		}
		return user;
	}

	// Phase 2-3: 캡션 생성 시 업종/분위기 자동 저장
	@Transactional
	public void updateBusinessProfile(Long id, String businessType, String mood) {
		User user = getById(id);
		if (businessType != null && !businessType.isBlank()) {
			user.setBusinessType(businessType);
		}
		if (mood != null && !mood.isBlank()) {
			user.setMood(mood);
		}
	}
}
