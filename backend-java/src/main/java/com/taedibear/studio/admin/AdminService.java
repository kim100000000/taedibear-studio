package com.taedibear.studio.admin;

import com.taedibear.studio.admin.dto.AdminPageResponse;
import com.taedibear.studio.admin.dto.AdminPaymentItemResponse;
import com.taedibear.studio.admin.dto.AdminSummaryResponse;
import com.taedibear.studio.admin.dto.AdminUserItemResponse;
import com.taedibear.studio.domain.Payment;
import com.taedibear.studio.domain.PaymentStatus;
import com.taedibear.studio.domain.Plan;
import com.taedibear.studio.domain.PostStatus;
import com.taedibear.studio.domain.ScheduleStatus;
import com.taedibear.studio.domain.User;
import com.taedibear.studio.repository.PaymentRepository;
import com.taedibear.studio.repository.PostRepository;
import com.taedibear.studio.repository.ScheduledPostRepository;
import com.taedibear.studio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 5-1: 관리자 대시보드 — 전체 유저/사용량/결제 현황 집계.
 * 접근 제어는 SecurityConfig의 /api/admin/** hasRole("ADMIN")이 담당한다
 * (관리자 = ADMIN_EMAIL 환경변수와 이메일이 일치하는 계정, JwtAuthenticationFilter 참고).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

	private static final int MAX_PAGE_SIZE = 100;

	private final UserRepository userRepository;
	private final PostRepository postRepository;
	private final ScheduledPostRepository scheduledPostRepository;
	private final PaymentRepository paymentRepository;

	public AdminSummaryResponse getSummary() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime sevenDaysAgo = now.minusDays(7);
		LocalDateTime thirtyDaysAgo = now.minusDays(30);
		LocalDateTime startOfMonth = now.withDayOfMonth(1)
				.withHour(0).withMinute(0).withSecond(0).withNano(0);

		var users = new AdminSummaryResponse.Users(
				userRepository.count(),
				userRepository.countByCreatedAtAfter(sevenDaysAgo),
				userRepository.countByCreatedAtAfter(thirtyDaysAgo),
				userRepository.countByPlan(Plan.free),
				userRepository.countByPlan(Plan.pro));

		var posts = new AdminSummaryResponse.Posts(
				postRepository.count(),
				postRepository.countByStatus(PostStatus.posted),
				postRepository.countByStatus(PostStatus.failed),
				postRepository.countByCreatedAtAfter(sevenDaysAgo));

		var schedules = new AdminSummaryResponse.Schedules(
				scheduledPostRepository.countByStatus(ScheduleStatus.pending),
				scheduledPostRepository.countByStatus(ScheduleStatus.failed));

		var revenue = new AdminSummaryResponse.Revenue(
				paymentRepository.sumAmountByStatusSince(PaymentStatus.PAID, startOfMonth),
				paymentRepository.countByStatusAndValidUntilAfter(PaymentStatus.PAID, now));

		return new AdminSummaryResponse(users, posts, schedules, revenue);
	}

	public AdminPageResponse<AdminUserItemResponse> getUsers(int page, int size, String search) {
		Pageable pageable = PageRequest.of(Math.max(0, page), clampSize(size),
				Sort.by(Sort.Direction.DESC, "createdAt"));

		Page<User> userPage = (search == null || search.isBlank())
				? userRepository.findAll(pageable)
				: userRepository.findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(
						search.trim(), search.trim(), pageable);

		// 페이지 내 유저들의 게시물 수를 한 번의 group-by 쿼리로 조회 (N+1 방지)
		List<Long> userIds = userPage.getContent().stream().map(User::getId).toList();
		Map<Long, Long> postCounts = new HashMap<>();
		if (!userIds.isEmpty()) {
			for (Object[] row : postRepository.countGroupByUserIds(userIds)) {
				postCounts.put((Long) row[0], (Long) row[1]);
			}
		}

		List<AdminUserItemResponse> items = userPage.getContent().stream()
				.map(u -> AdminUserItemResponse.from(u, postCounts.getOrDefault(u.getId(), 0L)))
				.toList();

		return AdminPageResponse.of(items, userPage.getNumber(), userPage.getSize(),
				userPage.getTotalElements());
	}

	public AdminPageResponse<AdminPaymentItemResponse> getPayments(int page, int size) {
		Pageable pageable = PageRequest.of(Math.max(0, page), clampSize(size));
		Page<Payment> paymentPage = paymentRepository.findAllByOrderByCreatedAtDesc(pageable);

		// 결제자 이메일 표시용 — 페이지 내 userId만 모아 일괄 조회
		List<Long> userIds = paymentPage.getContent().stream()
				.map(Payment::getUserId).distinct().toList();
		Map<Long, String> emails = new HashMap<>();
		if (!userIds.isEmpty()) {
			userRepository.findAllById(userIds)
					.forEach(u -> emails.put(u.getId(), u.getEmail()));
		}

		List<AdminPaymentItemResponse> items = paymentPage.getContent().stream()
				.map(p -> AdminPaymentItemResponse.from(p, emails.getOrDefault(p.getUserId(), "(탈퇴)")))
				.toList();

		return AdminPageResponse.of(items, paymentPage.getNumber(), paymentPage.getSize(),
				paymentPage.getTotalElements());
	}

	private int clampSize(int size) {
		if (size < 1) return 20;
		return Math.min(size, MAX_PAGE_SIZE);
	}
}
