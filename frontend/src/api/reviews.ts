import apiClient from './client';
import type { ApiResponse } from '../types';

// Phase 4-3: 리뷰(댓글) 자동 답글
export interface ReviewComment {
  id: number;
  media_id: string;
  comment_text: string;
  username: string | null;
  suggested_reply: string | null;
  status: 'pending' | 'replied' | 'skipped';
  created_at: string;
}

// GET /api/instagram/accounts/:id/comments
export const listComments = (accountId: number) =>
  apiClient.get<ApiResponse<ReviewComment[]>>(`/api/instagram/accounts/${accountId}/comments`);

// POST /api/instagram/comments/:id/reply
export const replyToComment = (commentId: number, message: string) =>
  apiClient.post<ApiResponse<{ success: boolean }>>(`/api/instagram/comments/${commentId}/reply`, { message });

// POST /api/instagram/comments/:id/skip
export const skipComment = (commentId: number) =>
  apiClient.post<ApiResponse<{ success: boolean }>>(`/api/instagram/comments/${commentId}/skip`);
