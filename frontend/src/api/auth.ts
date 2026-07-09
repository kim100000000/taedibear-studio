import apiClient from './client';
import type { ApiResponse, User } from '../types';

export interface LoginResult {
  token: string;
  user: User;
}

// C4: 응답의 refresh 쿠키(HttpOnly)를 브라우저가 저장하려면 withCredentials 필요
// (프론트 5173 ↔ 백엔드 4000은 크로스 오리진이라 기본값으로는 Set-Cookie가 무시됨)
export const login = (email: string, password: string) =>
  apiClient.post<ApiResponse<LoginResult>>('/api/auth/login', { email, password }, { withCredentials: true });

export const register = (name: string, email: string, password: string) =>
  apiClient.post<ApiResponse<LoginResult>>('/api/auth/register', { name, email, password }, { withCredentials: true });

// docs/05_API명세서.md: 내 프로필 조회는 /api/users/me
export const getMe = () => apiClient.get<ApiResponse<User>>('/api/users/me');

// C4: refresh token은 HttpOnly 쿠키(refresh_token)로 오가므로 withCredentials 필수.
// 성공 시 새 access token을 body로 받고, 서버가 refresh 쿠키를 회전(재발급)한다.
export const refreshAccessToken = () =>
  apiClient.post<ApiResponse<{ token: string }>>('/api/auth/refresh', null, { withCredentials: true });

// C4/M9: 서버측에서 refresh token을 폐기하고 쿠키를 삭제한다.
export const logoutServer = () =>
  apiClient.post<ApiResponse<null>>('/api/auth/logout', null, { withCredentials: true });

// ── Phase 6: 비밀번호 찾기 / 이메일 인증 ─────────────────────────────────────

// 계정 존재 여부와 무관하게 항상 성공 응답 (이메일 존재 노출 방지)
export const forgotPassword = (email: string) =>
  apiClient.post<ApiResponse<null>>('/api/auth/forgot-password', { email });

export const resetPassword = (token: string, password: string) =>
  apiClient.post<ApiResponse<null>>('/api/auth/reset-password', { token, password });

export const verifyEmail = (token: string) =>
  apiClient.post<ApiResponse<null>>('/api/auth/verify-email', { token });

// 로그인 상태에서 인증 메일 재발송
export const resendVerification = () =>
  apiClient.post<ApiResponse<null>>('/api/auth/resend-verification');
