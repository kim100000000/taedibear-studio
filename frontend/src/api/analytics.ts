import apiClient from './client';
import type { ApiResponse } from '../types';

export interface MonthlyUpload {
  month: string;  // "YYYY-MM"
  count: number;
}

export interface DailyPattern {
  day: string;  // "Sun" ~ "Sat"
  count: number;
}

// Phase 4-2: 팔로워 추이 스냅샷
export interface FollowerPoint {
  date: string;  // "YYYY-MM-DD"
  followers: number;
}

export interface AnalyticsSummary {
  monthly_uploads: MonthlyUpload[];
  success_rate: number;       // 0 ~ 100 (%)
  scheduled_ratio: number;    // 0 ~ 100 (%)
  daily_pattern: DailyPattern[];
  this_month_used: number;
  follower_trend: FollowerPoint[];
}

// GET /api/analytics/summary — Phase 4-1: instagram_account_id로 계정별 통계 조회 (없으면 전체 합산)
export const getAnalyticsSummary = (instagramAccountId?: number) =>
  apiClient.get<ApiResponse<AnalyticsSummary>>('/api/analytics/summary', {
    params: instagramAccountId ? { instagram_account_id: instagramAccountId } : undefined,
  });
