package com.taedibear.studio.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 공통 응답 포맷 (docs/05_API명세서.md):
 * 성공: { "success": true, "data": {...} }
 * 실패: { "success": false, "error": "에러 메시지" }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, String error) {

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, data, null);
	}

	public static ApiResponse<Void> ok() {
		return new ApiResponse<>(true, null, null);
	}

	public static <T> ApiResponse<T> fail(String error) {
		return new ApiResponse<>(false, null, error);
	}
}
