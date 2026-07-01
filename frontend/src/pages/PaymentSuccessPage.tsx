import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import Spinner from '../components/Spinner';
import { confirmPayment } from '../api/payments';

// 토스페이먼츠 결제 성공 리다이렉트 처리 페이지 (Phase 2-1)
// Toss → /payments/success?paymentKey=...&orderId=...&amount=...
export default function PaymentSuccessPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [error, setError] = useState('');

  useEffect(() => {
    const paymentKey = searchParams.get('paymentKey');
    const orderId = searchParams.get('orderId');
    const amount = Number(searchParams.get('amount'));

    if (!paymentKey || !orderId || !amount) {
      navigate('/settings', { replace: true });
      return;
    }

    confirmPayment(paymentKey, orderId, amount)
      .then(() => {
        navigate('/settings?upgraded=true', { replace: true });
      })
      .catch((err: any) => {
        setError(err.response?.data?.error || t('plan.paymentFailed'));
      });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (error) {
    return (
      <div className="auth-page">
        <p className="error">{error}</p>
        <button type="button" className="btn-primary" onClick={() => navigate('/settings')}>
          {t('settings.title')}으로 돌아가기
        </button>
      </div>
    );
  }

  return <Spinner label={t('plan.confirmingPayment')} />;
}
