package com.taedibear.studio.notice;

import com.taedibear.studio.common.ApiResponse;
import com.taedibear.studio.domain.Notice;
import com.taedibear.studio.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

// 개선백로그 🟡: 공지사항 조회 (전체 유저). 작성/삭제는 관리자 전용 — AdminController.
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

	private final NoticeRepository noticeRepository;

	public record NoticeResponse(Long id, String title, String content, LocalDateTime created_at) {
		public static NoticeResponse from(Notice n) {
			return new NoticeResponse(n.getId(), n.getTitle(), n.getContent(), n.getCreatedAt());
		}
	}

	@GetMapping
	public ApiResponse<List<NoticeResponse>> list() {
		return ApiResponse.ok(noticeRepository.findAllByOrderByCreatedAtDesc()
				.stream().map(NoticeResponse::from).toList());
	}
}
