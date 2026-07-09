import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { forgotPassword } from '../api/auth';

// Phase 6: 비밀번호 찾기 — 이메일 입력 → 재설정 링크 발송
export default function ForgotPasswordPage() {
  const { t } = useTranslation();
  const [email, setEmail] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!/\S+@\S+\.\S+/.test(email)) {
      setError(t('auth.validation.emailFormat'));
      return;
    }
    setSubmitting(true);
    try {
      await forgotPassword(email.trim());
      // 서버는 계정 존재 여부와 무관하게 성공을 반환 — 항상 "보냈어요" 안내
      setSent(true);
    } catch (err: any) {
      setError(err.isNetworkError ? t('error.network') : err.response?.data?.error || t('error.loadFailed'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <Link to="/" className="auth-logo">🐻 Taedibear Studio</Link>
      <h1>{t('auth.forgot.title')}</h1>

      {sent ? (
        <>
          <div className="banner banner-info">{t('auth.forgot.sent', { email })}</div>
          <p className="auth-switch">
            <Link to="/login">{t('auth.forgot.backToLogin')}</Link>
          </p>
        </>
      ) : (
        <>
          <p className="muted">{t('auth.forgot.description')}</p>
          <form onSubmit={handleSubmit}>
            <input
              type="email"
              placeholder={t('auth.field.email')}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            <button type="submit" className="btn-primary" disabled={submitting}>
              {submitting ? t('auth.forgot.submitting') : t('auth.forgot.submit')}
            </button>
          </form>
          {error && <p className="error">{error}</p>}
          <p className="auth-switch">
            <Link to="/login">{t('auth.forgot.backToLogin')}</Link>
          </p>
        </>
      )}
    </div>
  );
}
