import { useSearchParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

// 토스페이먼츠 결제 실패/취소 리다이렉트 (Phase 2-1)
export default function PaymentFailPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { t } = useTranslation();

  const message = searchParams.get('message') || t('plan.paymentFailed');

  return (
    <div className="auth-page">
      <h2>⚠️ {t('plan.paymentFailed')}</h2>
      <p className="muted">{message}</p>
      <button type="button" className="btn-primary" onClick={() => navigate('/settings')}>
        {t('settings.title')}으로 돌아가기
      </button>
    </div>
  );
}
