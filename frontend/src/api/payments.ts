import apiClient from './client';
import type { ApiResponse } from '../types';

export interface PaymentStatus {
  plan: 'free' | 'pro';
  paymentKey?: string;
  validUntil?: string;
  amount?: number;
}

export interface UsageInfo {
  plan: 'free' | 'pro';
  used: number;
  limit: number | null; // null = 무제한 (pro)
  reset_at: string;
}

// POST /api/payments/confirm — 토스 결제 승인
export const confirmPayment = (paymentKey: string, orderId: string, amount: number) =>
  apiClient.post<ApiResponse<PaymentStatus>>('/api/payments/confirm', {
    paymentKey,
    orderId,
    amount,
  });

// GET /api/payments/status — 현재 플랜 + 결제 상태
export const getPaymentStatus = () =>
  apiClient.get<ApiResponse<PaymentStatus>>('/api/payments/status');

// POST /api/payments/cancel — 구독 해지
export const cancelPayment = (paymentKey: string, cancelReason?: string) =>
  apiClient.post<ApiResponse<void>>('/api/payments/cancel', {
    paymentKey,
    cancelReason: cancelReason ?? '구독 해지',
  });
