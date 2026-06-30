import apiClient from './client';
import type { ApiResponse, InstagramAccount } from '../types';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:4000';

// GET /api/instagram/accounts
export const listInstagramAccounts = () =>
  apiClient.get<ApiResponse<InstagramAccount[]>>('/api/instagram/accounts');

// DELETE /api/instagram/accounts/:id
export const disconnectInstagramAccount = (id: number) =>
  apiClient.delete<ApiResponse<null>>(`/api/instagram/accounts/${id}`);

// GET /api/instagram/connect — 전체 페이지 이동(리다이렉트)이라 axios가 아니라 location 이동으로 호출한다.
// 백엔드가 Authorization 헤더를 못 받기 때문에 JWT를 쿼리 파라미터로 그대로 전달한다.
export const getInstagramConnectUrl = (): string => {
  const token = localStorage.getItem('token');
  return `${API_URL}/api/instagram/connect?token=${token}`;
};
