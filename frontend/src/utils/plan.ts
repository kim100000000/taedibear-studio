// 플랜 UI (5단계 부가 기능).
// 실제 결제/구독 연동은 이번 로드맵에 없는 단계라 백엔드에 plan 필드가 없다.
// 그래서 우선 localStorage로 "현재 플랜"을 흉내만 내고, 업그레이드 버튼은
// 결제 연동 전까지 안내 토스트만 띄운다. (TODO: 결제 연동 시 서버의 plan 값으로 교체)
export type PlanId = 'free' | 'pro';

const PLAN_STORAGE_KEY = 'plan';

export interface PlanDefinition {
  id: PlanId;
  name: string;
  price: string;
  priceSuffix?: string;
  features: string[];
}

export const PLANS: PlanDefinition[] = [
  {
    id: 'free',
    name: 'Free',
    price: '₩0',
    features: ['월 10건 업로드', '인스타그램 1개 계정 연동', 'AI 캡션 생성 (기본)'],
  },
  {
    id: 'pro',
    name: 'Pro',
    price: '₩9,900',
    priceSuffix: '/월',
    features: ['업로드 무제한', '인스타그램 다중 계정 연동', 'AI 캡션 생성 (고급)', '예약 업로드 우선 처리'],
  },
];

export function getPlan(): PlanId {
  const saved = localStorage.getItem(PLAN_STORAGE_KEY);
  return saved === 'pro' ? 'pro' : 'free';
}

export function setPlan(plan: PlanId): void {
  localStorage.setItem(PLAN_STORAGE_KEY, plan);
}
