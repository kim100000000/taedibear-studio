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

	// 크레딧 보유 상한 — 쌓아두기만 하고 결제하지 않는 것을 방지.
	// 광고 충전(하루 2회)을 한 달 내내 돌려도 이 이상 쌓이지 않는다.
	public static final int MAX_CREDITS = 10;

	private final UserRepository userRepository;
	private final PostRepository postRepository;
	private final ScheduledPostRepository scheduledPostRepository;
	private final InstagramAccountRepository instagramAccountRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final com.taedibear.studio.repository.AuthTokenRepository authTokenRepository;
	private final com.taedibear.studio.repository.SavedCaptionRepository savedCaptionRepository;

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
		user.setCredits(Math.min(user.getCredits() + 2, MAX_CREDITS));
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

		// 보유 상한: 가득 찬 상태에서 광고만 보고 크레딧을 못 받는 일이 없도록 사전 차단
		if (user.getCredits() >= MAX_CREDITS) {
			throw ApiException.badRequest("크레딧이 가득 찼어요. (최대 " + MAX_CREDITS + "개)");
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
		user.setCredits(Math.min(user.getCredits() + 1, MAX_CREDITS));
		return user.getCredits();
	}

	// Phase 6: 이메일 인증 게이트 — 크레딧을 소모하는 기능(캡션 생성) 진입 전 호출.
	// 미인증 다계정으로 무료 크레딧을 수확하는 것을 막는다 (docs/10 8절 리스크).
	public void assertEmailVerified(Long id) {
		User user = getById(id);
		if (!user.isEmailVerified()) {
			throw ApiException.forbidden("이메일 인증 후 이용할 수 있어요. 메일함(스팸함 포함)을 확인해주세요.");
		}
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
		authTokenRepository.deleteAllByUserId(userId);     // Phase 6: 재설정/인증 토큰 정리
		savedCaptionRepository.deleteAllByUserId(userId);  // 캡션 보관함 정리
		userRepository.delete(user);

		log.info("[회원 탈퇴] userId={} 및 연관 데이터 삭제 완료 (결제 내역은 보관)", userId);
	}
}
