import apiClient from './client';
import type { ApiResponse } from '../types';

// 개선백로그 🟡: 공지사항
export interface Notice {
  id: number;
  title: string;
  content: string;
  created_at: string;
}

export const listNotices = () =>
  apiClient.get<ApiResponse<Notice[]>>('/api/notices');

// 관리자 전용
export const createNotice = (title: string, content: string) =>
  apiClient.post<ApiResponse<Notice>>('/api/admin/notices', { title, content });

export const deleteNotice = (id: number) =>
  apiClient.delete<ApiResponse<null>>(`/api/admin/notices/${id}`);
