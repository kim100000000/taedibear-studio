import apiClient from './client';
import type { ApiResponse, ScheduledPost } from '../types';

export interface CreateSchedulePayload {
  post_id: number;
  scheduled_at: string;
}

export interface UpdateSchedulePayload {
  scheduled_at: string;
}

// POST /api/scheduled
export const createSchedule = (payload: CreateSchedulePayload) =>
  apiClient.post<ApiResponse<ScheduledPost>>('/api/scheduled', payload);

// GET /api/scheduled
export const listSchedules = () =>
  apiClient.get<ApiResponse<ScheduledPost[]>>('/api/scheduled');

// PUT /api/scheduled/:id
export const updateSchedule = (id: number, payload: UpdateSchedulePayload) =>
  apiClient.put<ApiResponse<ScheduledPost>>(`/api/scheduled/${id}`, payload);

// DELETE /api/scheduled/:id
export const deleteSchedule = (id: number) =>
  apiClient.delete<ApiResponse<null>>(`/api/scheduled/${id}`);
