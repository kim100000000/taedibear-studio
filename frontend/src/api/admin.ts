import apiClient from './client';
import type { ApiResponse } from '../types';

// Phase 5-1: 관리자 대시보드 API (관리자 계정만 호출 가능 — 403 시 접근 불가 처리)

export interface AdminSummary {
  users: { total: number; new_7d: number; new_30d: number; free: number; pro: number };
  posts: { total: number; posted: number; failed: number; new_7d: number };
  schedules: { pending: number; failed: number };
  revenue: { month_revenue: number; active_pro: number };
}

export interface AdminUserItem {
  id: number;
  name: string;
  email: string;
  plan: 'free' | 'pro';
  credits: number;
  post_count: number;
  created_at: string;
}

export interface AdminPaymentItem {
  id: number;
  user_id: number;
  user_email: string;
  order_id: string;
  amount: number;
  status: 'PAID' | 'CANCELED' | 'FAILED';
  valid_until: string | null;
  created_at: string;
}

export interface AdminPage<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
  total_pages: number;
}

export const getAdminSummary = () =>
  apiClient.get<ApiResponse<AdminSummary>>('/api/admin/summary');

export const getAdminUsers = (page: number, search?: string) =>
  apiClient.get<ApiResponse<AdminPage<AdminUserItem>>>('/api/admin/users', {
    params: { page, size: 20, ...(search ? { search } : {}) },
  });

export const getAdminPayments = (page: number) =>
  apiClient.get<ApiResponse<AdminPage<AdminPaymentItem>>>('/api/admin/payments', {
    params: { page, size: 20 },
  });
