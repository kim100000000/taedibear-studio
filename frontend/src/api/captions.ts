import apiClient from './client';
import type { ApiResponse } from '../types';

// 개선백로그 🟡: 캡션 보관함
export interface SavedCaption {
  id: number;
  caption: string;
  hashtags: string[];
  created_at: string;
}

export const saveCaption = (caption: string, hashtags: string[]) =>
  apiClient.post<ApiResponse<SavedCaption>>('/api/captions', { caption, hashtags });

export const listSavedCaptions = () =>
  apiClient.get<ApiResponse<SavedCaption[]>>('/api/captions');

export const deleteSavedCaption = (id: number) =>
  apiClient.delete<ApiResponse<null>>(`/api/captions/${id}`);
