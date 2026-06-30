import apiClient from './client';
import type { ApiResponse, User } from '../types';

export interface LoginResult {
  token: string;
  user: User;
}

export const login = (email: string, password: string) =>
  apiClient.post<ApiResponse<LoginResult>>('/api/auth/login', { email, password });

export const register = (name: string, email: string, password: string) =>
  apiClient.post<ApiResponse<LoginResult>>('/api/auth/register', { name, email, password });

// docs/05_API명세서.md: 내 프로필 조회는 /api/users/me
export const getMe = () => apiClient.get<ApiResponse<User>>('/api/users/me');
