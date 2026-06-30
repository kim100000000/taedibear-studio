import apiClient from './client';
import type { ApiResponse, User } from '../types';

// PUT /api/users/me
export const updateMe = (name: string) =>
  apiClient.put<ApiResponse<User>>('/api/users/me', { name });
