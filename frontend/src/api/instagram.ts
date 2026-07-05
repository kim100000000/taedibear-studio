import apiClient from './client';
import type { ApiResponse, InstagramAccount } from '../types';

// GET /api/instagram/accounts
export const listInstagramAccounts = () =>
  apiClient.get<ApiResponse<InstagramAccount[]>>('/api/instagram/accounts');

// DELETE /api/instagram/accounts/:id
export const disconnectInstagramAccount = (id: number) =>
  apiClient.delete<ApiResponse<null>>(`/api/instagram/accounts/${id}`);

// C6: GET /api/instagram/connect-url — 인증된 API로 Meta 로그인 URL(랜덤 nonce state 포함)을 받아
// window.location으로 이동한다. (기존: JWT를 ?token= 쿼리로 노출하던 방식 제거)
export const getInstagramConnectUrl = () =>
  apiClient.get<ApiResponse<{ url: string }>>('/api/instagram/connect-url');
