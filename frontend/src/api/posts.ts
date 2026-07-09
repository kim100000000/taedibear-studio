import apiClient from './client';
import type { ApiResponse, Post, PostListResponse } from '../types';

export interface UploadImageResult {
  image_url: string;
}

export interface GenerateCaptionResult {
  caption: string;
  hashtags: string[];
}

export interface CreatePostPayload {
  instagram_account_id: number;
  image_url: string;
  caption: string;
  hashtags: string[];
}

export interface UpdatePostPayload {
  caption?: string;
  hashtags?: string[];
}

export interface ListPostsParams {
  status?: string;
  // Phase 4-1: 계정별 히스토리 필터링
  instagram_account_id?: number;
  page?: number;
  limit?: number;
}

// Phase 4-2: GET /api/posts/:id/insights 응답
export interface PostInsights {
  engagement: number;
  impressions: number;
  reach: number;
  like_count: number;
  comments_count: number;
}

// POST /api/posts/upload (multipart/form-data)
// 개선백로그 🟠: onProgress 콜백(0~100)으로 업로드 진행률 표시 지원
export const uploadImage = (file: File, onProgress?: (percent: number) => void) => {
  const formData = new FormData();
  formData.append('image', file);
  return apiClient.post<ApiResponse<UploadImageResult>>('/api/posts/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e) => {
      if (onProgress && e.total) onProgress(Math.round((e.loaded / e.total) * 100));
    },
  });
};

// Phase 2-3: 선택 입력 필드 추가
export interface GenerateCaptionPayload {
  image_url: string;
  business_type: string;
  mood: string;
  special_menu?: string;
  event_promotion?: string;
  keywords?: string;
}

// POST /api/posts/caption
export const generateCaption = (payload: GenerateCaptionPayload) =>
  apiClient.post<ApiResponse<GenerateCaptionResult>>('/api/posts/caption', payload);

// POST /api/posts — draft 저장
export const createPost = (payload: CreatePostPayload) =>
  apiClient.post<ApiResponse<Post>>('/api/posts', payload);

// GET /api/posts?status=&page=&limit=
export const listPosts = (params?: ListPostsParams) =>
  apiClient.get<ApiResponse<PostListResponse>>('/api/posts', { params });

// GET /api/posts/:id
export const getPost = (id: number) => apiClient.get<ApiResponse<Post>>(`/api/posts/${id}`);

// PUT /api/posts/:id
export const updatePost = (id: number, payload: UpdatePostPayload) =>
  apiClient.put<ApiResponse<Post>>(`/api/posts/${id}`, payload);

// DELETE /api/posts/:id
export const deletePost = (id: number) => apiClient.delete<ApiResponse<null>>(`/api/posts/${id}`);

// POST /api/posts/:id/publish — 즉시 업로드
export const publishPost = (id: number) =>
  apiClient.post<ApiResponse<Post>>(`/api/posts/${id}/publish`);

// GET /api/posts/:id/insights — Phase 4-2: 게시물 조회수/좋아요/저장 등 인사이트
export const getPostInsights = (id: number) =>
  apiClient.get<ApiResponse<PostInsights>>(`/api/posts/${id}/insights`);
