import { useState } from 'react';
import { PLANS, getPlan, setPlan, type PlanId } from '../utils/plan';
import type { ToastData } from '../types';

interface PlanSectionProps {
  onToast: (toast: ToastData) => void;
}

// 설정 페이지의 "플랜" 섹션 (5단계 부가 기능). 현재 플랜 뱃지 + 플랜 비교 카드.
export default function PlanSection({ onToast }: PlanSectionProps) {
  const [currentPlan, setCurrentPlan] = useState<PlanId>(getPlan);

  const handleSelect = (planId: PlanId) => {
    if (planId === currentPlan) return;

    if (planId === 'pro') {
      // TODO: 결제 연동 전이라 실제 업그레이드 대신 안내만 띄운다.
      onToast({ type: 'error', message: '결제 연동 준비 중이에요. 곧 만나요!' });
      return;
    }

    setPlan(planId);
    setCurrentPlan(planId);
    onToast({ type: 'success', message: 'Free 플랜으로 변경했어요.' });
  };

  return (
    <section className="settings-section plan-section">
      <div className="plan-current">
        <h2>플랜</h2>
        <span className={`plan-badge plan-badge-${currentPlan}`}>
          {currentPlan === 'pro' ? 'Pro 이용 중' : 'Free 이용 중'}
        </span>
      </div>

      <div className="plan-cards">
        {PLANS.map((plan) => {
          const isActive = plan.id === currentPlan;
          return (
            <div key={plan.id} className={`plan-card ${isActive ? 'plan-card-active' : ''}`}>
              <h3>{plan.name}</h3>
              <div className="plan-price">
                {plan.price}
                {plan.priceSuffix && <span>{plan.priceSuffix}</span>}
              </div>
              <ul className="plan-feature-list">
                {plan.features.map((feature) => (
                  <li key={feature}>✓ {feature}</li>
                ))}
              </ul>
              <button
                type="button"
                className={isActive ? 'btn-outline' : 'btn-primary'}
                disabled={isActive}
                onClick={() => handleSelect(plan.id)}
              >
                {isActive ? '현재 플랜' : plan.id === 'pro' ? '업그레이드' : '다운그레이드'}
              </button>
            </div>
          );
        })}
      </div>
    </section>
  );
}
