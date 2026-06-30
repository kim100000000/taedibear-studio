package com.taedibear.studio.auth.dto;

import com.taedibear.studio.domain.User;

public record UserDto(Long id, String name, String email, String plan) {
	public static UserDto from(User user) {
		return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getPlan().name());
	}
}
