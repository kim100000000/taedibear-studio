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

export interface AnalyticsSummary {
  monthly_uploads: MonthlyUpload[];
  success_rate: number;       // 0 ~ 100 (%)
  scheduled_ratio: number;    // 0 ~ 100 (%)
  daily_pattern: DailyPattern[];
  this_month_used: number;
}

// GET /api/analytics/summary
export const getAnalyticsSummary = () =>
  apiClient.get<ApiResponse<AnalyticsSummary>>('/api/analytics/summary');
