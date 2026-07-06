// docs/04_DB설계서.md / docs/05_API명세서.md 기준 공통 타입.
// 백엔드(Node, Java 둘 다) 응답의 필드명(snake_case)을 그대로 따른다.

export type PostStatus = 'draft' | 'scheduled' | 'posted' | 'failed';
export type ScheduleStatus = 'pending' | 'done' | 'failed';

export interface User {
  id: number;
  name: string;
  email: string;
  plan?: 'free' | 'pro';
  // Phase 2-3: 업종/분위기 저장
  business_type?: string | null;
  mood?: string | null;
  // Phase 2-1: 크레딧
  credits?: number;
}

export interface InstagramAccount {
  id: number;
  username: string;
  instagram_user_id?: string;
  connected_at?: string;
  // Phase 4-3: 리뷰 자동 답글 사용 여부
  auto_reply_enabled?: boolean;
}

export interface Post {
  id: number;
  user_id?: number;
  instagram_account_id: number;
  image_url: string;
  caption: string | null;
  hashtags?: string[];
  status: PostStatus;
  instagram_post_id?: string | null;
  posted_at?: string | null;
  created_at?: string;
}

export interface PostListResponse {
  posts: Post[];
  total?: number;
  page?: number;
  limit?: number;
}

export interface ScheduledPost {
  id: number;
  post_id: number;
  scheduled_at: string;
  status: ScheduleStatus;
  post?: {
    image_url: string;
    caption: string | null;
  };
}

// 공통 응답 포맷 (docs/05_API명세서.md): { success, data, error }
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: string;
}

export interface ToastData {
  type?: 'success' | 'error';
  message: string;
}

export interface FieldErrors {
  [field: string]: string;
}
