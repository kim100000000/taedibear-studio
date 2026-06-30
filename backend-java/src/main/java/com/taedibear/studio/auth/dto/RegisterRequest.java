package com.taedibear.studio.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank(message = "이름은 2자 이상이어야 해요.")
		@Size(min = 2, message = "이름은 2자 이상이어야 해요.")
		String name,

		@NotBlank(message = "올바른 이메일 형식이 아니에요.")
		@Email(message = "올바른 이메일 형식이 아니에요.")
		String email,

		@NotBlank(message = "비밀번호는 8자 이상이어야 해요.")
		@Size(min = 8, message = "비밀번호는 8자 이상이어야 해요.")
		String password
) {
}
