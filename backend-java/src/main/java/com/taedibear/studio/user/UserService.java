package com.taedibear.studio.user;

import com.taedibear.studio.common.exception.ApiException;
import com.taedibear.studio.domain.Plan;
import com.taedibear.studio.domain.User;
import com.taedibear.studio.repository.InstagramAccountRepository;
import com.taedibear.studio.repository.PostRepository;
import com.taedibear.studio.repository.RefreshTokenRepository;
import com.taedibear.studio.repository.ScheduledPostRepository;
import com.taedibear.studio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

	// 광고 시청 최소 간격(초) — 리워드 광고 최소 재생 시간 기준
	private static final long MIN_AD_INTERVAL_SECONDS = 30;

	private final UserRepository userRepository;
	private final PostRepository postRepository;
	private final ScheduledPostRepository scheduledPostRepository;
	private final InstagramAccountRepository instagramAccountRepository;
	private final RefreshTokenRepository refreshTokenRepository;

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
	// 행 잠금(FOR UPDATE)으로 동시 요청을 직렬화하고, onboardingCompleted 플래그로 재지급을 막는다.
	// 이미 받은 경우 에러 대신 현재 크레딧을 그대로 반환한다(멱등 처리 — 프론트 수정 불필요).
	@Transactional
	public int completeOnboarding(Long id) {
		User user = getByIdForUpdate(id);
		if (user.isOnboardingCompleted()) {
			return user.getCredits();
		}
		user.setOnboardingCompleted(true);
		user.setCredits(user.getCredits() + 2);
		return user.getCredits();
	}

	// Phase 2-1: 광고 시청 충전 +1 크레딧, 하루 최대 2회
	// 행 잠금으로 하루 2회 제한의 race condition을 막고,
	// 최소 시청 간격(30초)으로 스크립트 연타를 1차 차단한다.
	// TODO: 광고 SDK 도입 시 서버사이드 검증(SSV) 콜백 방식으로 교체 — 현재는 실제 시청 여부를 알 수 없음.
	@Transactional
	public int watchAd(Long id) {
		User user = getByIdForUpdate(id);
		LocalDate today = LocalDate.now();
		LocalDateTime now = LocalDateTime.now();

		if (user.getLastAdWatchAt() != null
				&& Duration.between(user.getLastAdWatchAt(), now).getSeconds() < MIN_AD_INTERVAL_SECONDS) {
			throw ApiException.badRequest("광고 시청 후 잠시 뒤에 다시 시도해주세요.");
		}

		if (today.equals(user.getLastAdWatchDate())) {
			if (user.getAdWatchCount() >= 2) {
				throw ApiException.badRequest("광고 충전은 하루 최대 2회까지 가능해요.");
			}
			user.setAdWatchCount(user.getAdWatchCount() + 1);
		} else {
			user.setLastAdWatchDate(today);
			user.setAdWatchCount(1);
		}

		user.setLastAdWatchAt(now);
		user.setCredits(user.getCredits() + 1);
		return user.getCredits();
	}

	// Phase 2-1: 캡션 생성 시 크레딧 1개 차감 (Free 플랜만)
	// 검사(credits > 0)와 차감을 UPDATE 한 문장으로 처리해 동시 요청으로도 음수가 될 수 없다.
	@Transactional
	public void deductCreditForCaption(Long id) {
		User user = getById(id);
		if (user.getPlan() == Plan.pro) return;  // Pro는 차감 없음

		int updated = userRepository.deductOneCredit(id);
		if (updated == 0) {
			throw ApiException.forbidden("크레딧이 부족해요. 광고를 보고 충전해주세요.");
		}
	}

	private User getByIdForUpdate(Long id) {
		return userRepository.findByIdForUpdate(id)
				.orElseThrow(() -> ApiException.notFound("사용자를 찾을 수 없어요."));
	}

	// 회원 탈퇴 — 연관 데이터를 FK 안전 순서로 삭제하고 사용자를 제거한다.
	// 결제(payments)는 법적 보관 의무로 남긴다. payments.user_id는 FK가 아닌 단순 컬럼이라
	// 사용자 삭제 후에도 과거 기록으로 유지되며, 조회할 사용자 행이 없으므로 사실상 익명화된다.
	// 삭제 순서: scheduled_posts → posts → instagram_accounts → users
	//   (posts는 users와 instagram_accounts를, scheduled_posts는 posts를 참조하므로 자식부터 삭제)
	@Transactional
	public void deleteAccount(Long userId) {
		User user = getById(userId);

		scheduledPostRepository.deleteAllByPostUserId(userId);
		postRepository.deleteAllByUserId(userId);
		instagramAccountRepository.deleteAllByUserId(userId);
		refreshTokenRepository.deleteAllByUserId(userId);  // C4: 모든 로그인 세션 폐기
		userRepository.delete(user);

		log.info("[회원 탈퇴] userId={} 및 연관 데이터 삭제 완료 (결제 내역은 보관)", userId);
	}
}
