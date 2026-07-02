import apiClient from './client';
import type { ApiResponse, User } from '../types';
import type { UsageInfo } from './payments';

// GET /api/users/me
export const getMe = () =>
  apiClient.get<ApiResponse<User>>('/api/users/me');

// PUT /api/users/me
export const updateMe = (name: string) =>
  apiClient.put<ApiResponse<User>>('/api/users/me', { name });

// GET /api/users/me/usage — 플랜 + 이번 달 사용량 (Phase 2-1)
export const getUsage = () =>
  apiClient.get<ApiResponse<UsageInfo>>('/api/users/me/usage');

// POST /api/users/me/onboarding-complete — 온보딩 완료 +2 크레딧 (Phase 2-1)
export const completeOnboarding = () =>
  apiClient.post<ApiResponse<{ credits: number }>>('/api/users/me/onboarding-complete');

// POST /api/users/me/credits/ad-watch — 광고 시청 +1 크레딧 (Phase 2-1)
export const watchAd = () =>
  apiClient.post<ApiResponse<{ credits: number }>>('/api/users/me/credits/ad-watch');
