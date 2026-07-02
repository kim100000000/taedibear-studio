package com.taedibear.studio.user;

import com.taedibear.studio.common.exception.ApiException;
import com.taedibear.studio.domain.Plan;
import com.taedibear.studio.domain.User;
import com.taedibear.studio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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

	// Phase 2-1: 온보딩 완료 보너스 +2 크레딧 (1회만)
	@Transactional
	public int completeOnboarding(Long id) {
		User user = getById(id);
		user.setCredits(user.getCredits() + 2);
		return user.getCredits();
	}

	// Phase 2-1: 광고 시청 충전 +1 크레딧, 하루 최대 2회
	@Transactional
	public int watchAd(Long id) {
		User user = getById(id);
		LocalDate today = LocalDate.now();

		if (today.equals(user.getLastAdWatchDate())) {
			if (user.getAdWatchCount() >= 2) {
				throw ApiException.badRequest("광고 충전은 하루 최대 2회까지 가능해요.");
			}
			user.setAdWatchCount(user.getAdWatchCount() + 1);
		} else {
			user.setLastAdWatchDate(today);
			user.setAdWatchCount(1);
		}

		user.setCredits(user.getCredits() + 1);
		return user.getCredits();
	}

	// Phase 2-1: 캡션 생성 시 크레딧 1개 차감 (Free 플랜만)
	@Transactional
	public void deductCreditForCaption(Long id) {
		User user = getById(id);
		if (user.getPlan() == Plan.pro) return;  // Pro는 차감 없음
		if (user.getCredits() <= 0) {
			throw ApiException.forbidden("크레딧이 부족해요. 광고를 보고 충전해주세요.");
		}
		user.setCredits(user.getCredits() - 1);
	}
}
