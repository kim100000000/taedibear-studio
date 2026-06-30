package com.taedibear.studio.schedule;

import com.taedibear.studio.common.ApiResponse;
import com.taedibear.studio.schedule.dto.*;
import com.taedibear.studio.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// docs/05_API명세서.md 예약 업로드 API (/api/scheduled)
@RestController
@RequestMapping("/api/scheduled")
@RequiredArgsConstructor
public class ScheduledController {

	private final ScheduledPostService scheduledPostService;

	@PostMapping
	public ApiResponse<ScheduleResponse> create(@AuthenticationPrincipal UserPrincipal principal,
			@RequestBody CreateScheduleRequest request) {
		return ApiResponse.ok(scheduledPostService.createSchedule(principal.getId(), request));
	}

	@GetMapping
	public ApiResponse<List<ScheduleListItemResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
		return ApiResponse.ok(scheduledPostService.listSchedules(principal.getId()));
	}

	@PutMapping("/{id}")
	public ApiResponse<ScheduleResponse> update(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long id, @RequestBody UpdateScheduleRequest request) {
		return ApiResponse.ok(scheduledPostService.updateSchedule(id, principal.getId(), request));
	}

	@DeleteMapping("/{id}")
	public ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
		scheduledPostService.deleteSchedule(id, principal.getId());
		return ApiResponse.ok();
	}
}
