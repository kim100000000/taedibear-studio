package com.taedibear.studio.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 컨트롤러/서비스 어디서든 던질 수 있는 비즈니스 예외.
 * GlobalExceptionHandler가 잡아서 docs/05_API명세서.md 형식의 에러 응답으로 변환한다.
 */
public class ApiException extends RuntimeException {

	private final HttpStatus status;

	public ApiException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public static ApiException badRequest(String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, message);
	}

	public static ApiException unauthorized(String message) {
		return new ApiException(HttpStatus.UNAUTHORIZED, message);
	}

	public static ApiException forbidden(String message) {
		return new ApiException(HttpStatus.FORBIDDEN, message);
	}

	public static ApiException notFound(String message) {
		return new ApiException(HttpStatus.NOT_FOUND, message);
	}

	public static ApiException tooManyRequests(String message) {
		return new ApiException(HttpStatus.TOO_MANY_REQUESTS, message);
	}

	public static ApiException internal(String message) {
		return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, message);
	}
}
