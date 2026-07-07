package com.taedibear.studio.security;

import com.taedibear.studio.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * JWT 인증 필터가 SecurityContext에 채워 넣는 인증 주체.
 * 컨트롤러에서는 @AuthenticationPrincipal UserPrincipal principal로 꺼내 쓴다
 * (Node 버전의 req.user.id와 동일한 역할).
 */
@Getter
public class UserPrincipal implements UserDetails {

	private final Long id;
	private final String email;
	// Phase 5-1: 관리자 여부 — ADMIN_EMAIL 환경변수와 이메일이 일치하면 true
	private final boolean admin;

	public UserPrincipal(Long id, String email) {
		this(id, email, false);
	}

	public UserPrincipal(Long id, String email, boolean admin) {
		this.id = id;
		this.email = email;
		this.admin = admin;
	}

	public static UserPrincipal from(User user) {
		return new UserPrincipal(user.getId(), user.getEmail());
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		if (admin) {
			return List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"));
		}
		return List.of(new SimpleGrantedAuthority("ROLE_USER"));
	}

	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
