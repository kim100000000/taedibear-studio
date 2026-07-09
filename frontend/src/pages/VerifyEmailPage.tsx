import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import Spinner from '../components/Spinner';
import { verifyEmail } from '../api/auth';

// Phase 6: 이메일 인증 — 인증 메일 링크(/verify-email?token=...)로 진입, 자동 처리
export default function VerifyEmailPage() {
  const { t } = useTranslation();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') ?? '';

  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    if (!token) {
      setStatus('error');
      setErrorMessage(t('auth.verify.invalidLink'));
      return;
    }
    verifyEmail(token)
      .then(() => setStatus('success'))
      .catch((err: any) => {
        setStatus('error');
        setErrorMessage(
          err.isNetworkError ? t('error.network') : err.response?.data?.error || t('auth.verify.failed'),
        );
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  return (
    <div className="auth-page">
      <Link to="/" className="auth-logo">🐻 Taedibear Studio</Link>

      {status === 'loading' && <Spinner label={t('auth.verify.verifying')} />}

      {status === 'success' && (
        <>
          <h1>✅ {t('auth.verify.successTitle')}</h1>
          <p className="muted">{t('auth.verify.successBody')}</p>
          <Link to="/dashboard" className="btn-primary" style={{ textAlign: 'center' }}>
            {t('auth.verify.goDashboard')}
          </Link>
        </>
      )}

      {status === 'error' && (
        <>
          <div className="banner banner-warn">{errorMessage}</div>
          <p className="muted">{t('auth.verify.retryHint')}</p>
          <p className="auth-switch">
            <Link to="/dashboard">{t('auth.verify.goDashboard')}</Link>
          </p>
        </>
      )}
    </div>
  );
}
