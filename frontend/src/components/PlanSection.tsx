import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { getPaymentStatus, cancelPayment } from '../api/payments';
import type { PaymentStatus } from '../api/payments';
import type { ToastData } from '../types';

interface PlanSectionProps {
  onToast: (toast: ToastData) => void;
  /** settings 페이지에서 결제 완료 후 상태 갱신 트리거 */
  refreshTrigger?: number;
}

// 설정 페이지의 "플랜" 섹션 — Phase 2-1: 서버 plan + 토스페이먼츠 연동
// 토스 클라이언트 키: VITE_TOSS_CLIENT_KEY 환경변수
export default function PlanSection({ onToast, refreshTrigger }: PlanSectionProps) {
  const { t } = useTranslation();
  const [status, setStatus] = useState<PaymentStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [canceling, setCanceling] = useState(false);

  const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY as string | undefined;

  useEffect(() => {
    setLoading(true);
    getPaymentStatus()
      .then((res) => setStatus(res.data.data))
      .catch(() => setStatus({ plan: 'free' }))
      .finally(() => setLoading(false));
  }, [refreshTrigger]);

  const handleUpgrade = () => {
    if (!clientKey) {
      onToast({ type: 'error', message: t('plan.upgradeComingSoon') });
      return;
    }

    // 토스페이먼츠 결제 위젯 열기
    // @ts-expect-error: 토스 SDK는 index.html에서 스크립트로 로드
    const tossPayments = window.TossPayments(clientKey);
    const orderId = `order_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;

    tossPayments.requestPayment('카드', {
      amount: 29000,
      orderId,
      orderName: 'Taedibear Studio Pro 구독 (1개월)',
      customerName: '',
      successUrl: `${window.location.origin}/payments/success`,
      failUrl: `${window.location.origin}/payments/fail`,
    }).catch((err: any) => {
      if (err?.code !== 'USER_CANCEL') {
        onToast({ type: 'error', message: t('plan.paymentFailed') });
      }
    });
  };

  const handleCancel = async () => {
    if (!status?.paymentKey) return;
    if (!window.confirm(t('plan.cancelConfirm'))) return;
    setCanceling(true);
    try {
      await cancelPayment(status.paymentKey);
      setStatus({ plan: 'free' });
      onToast({ type: 'success', message: t('plan.downgraded') });
    } catch (err: any) {
      onToast({ type: 'error', message: err.response?.data?.error || t('plan.cancelFailed') });
    } finally {
      setCanceling(false);
    }
  };

  const PLANS = [
    {
      id: 'free' as const,
      name: t('plan.free.name'),
      price: t('plan.free.price'),
      priceSuffix: '',
      features: t('plan.free.features', { returnObjects: true }) as string[],
    },
    {
      id: 'pro' as const,
      name: t('plan.pro.name'),
      price: '₩29,000',
      priceSuffix: t('plan.priceSuffix'),
      features: t('plan.pro.features', { returnObjects: true }) as string[],
    },
  ];

  const currentPlan = status?.plan ?? 'free';

  return (
    <section className="settings-section plan-section">
      <div className="plan-current">
        <h2>{t('plan.title')}</h2>
        <span className={`plan-badge plan-badge-${currentPlan}`}>
          {currentPlan === 'pro' ? t('plan.currentPro') : t('plan.currentFree')}
        </span>
      </div>

      {currentPlan === 'pro' && status?.validUntil && (
        <p className="plan-valid-until">
          {t('plan.validUntil', {
            date: new Date(status.validUntil).toLocaleDateString('ko-KR'),
          })}
        </p>
      )}

      {loading ? null : (
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
                {isActive && plan.id === 'pro' ? (
                  <button
                    type="button"
                    className="btn-outline"
                    disabled={canceling}
                    onClick={handleCancel}
                  >
                    {canceling ? t('common.loading') : t('plan.cancelSubscription')}
                  </button>
                ) : isActive ? (
                  <button type="button" className="btn-outline" disabled>
                    {t('plan.current')}
                  </button>
                ) : (
                  <button type="button" className="btn-primary" onClick={handleUpgrade}>
                    {t('plan.upgrade')}
                  </button>
                )}
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}
