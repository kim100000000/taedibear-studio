import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { getPlan, setPlan, type PlanId } from '../utils/plan';
import type { ToastData } from '../types';

interface PlanSectionProps {
  onToast: (toast: ToastData) => void;
}

// 설정 페이지의 "플랜" 섹션 (5단계 부가 기능). 현재 플랜 뱃지 + 플랜 비교 카드.
// 실제 결제 연동은 이번 로드맵에 없으므로 업그레이드 버튼은 안내 토스트만 띄운다.
// (TODO: 결제 연동 시 서버의 plan 값으로 교체)
export default function PlanSection({ onToast }: PlanSectionProps) {
  const { t } = useTranslation();
  const [currentPlan, setCurrentPlan] = useState<PlanId>(getPlan);

  const PLANS = [
    {
      id: 'free' as PlanId,
      name: t('plan.free.name'),
      price: t('plan.free.price'),
      features: t('plan.free.features', { returnObjects: true }) as string[],
    },
    {
      id: 'pro' as PlanId,
      name: t('plan.pro.name'),
      price: t('plan.pro.price'),
      priceSuffix: t('plan.priceSuffix'),
      features: t('plan.pro.features', { returnObjects: true }) as string[],
    },
  ];

  const handleSelect = (planId: PlanId) => {
    if (planId === currentPlan) return;

    if (planId === 'pro') {
      onToast({ type: 'error', message: t('plan.upgradeComingSoon') });
      return;
    }

    setPlan(planId);
    setCurrentPlan(planId);
    onToast({ type: 'success', message: t('plan.downgraded') });
  };

  return (
    <section className="settings-section plan-section">
      <div className="plan-current">
        <h2>{t('plan.title')}</h2>
        <span className={`plan-badge plan-badge-${currentPlan}`}>
          {currentPlan === 'pro' ? t('plan.currentPro') : t('plan.currentFree')}
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
                {isActive ? t('plan.current') : plan.id === 'pro' ? t('plan.upgrade') : t('plan.downgrade')}
              </button>
            </div>
          );
        })}
      </div>
    </section>
  );
}
