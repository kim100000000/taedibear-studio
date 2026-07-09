import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { resetPassword } from '../api/auth';

// Phase 6: 비밀번호 재설정 — 메일 링크(/reset-password?token=...)로 진입
export default function ResetPasswordPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') ?? '';

  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (password.length < 8) {
      setError(t('auth.validation.passwordMin8'));
      return;
    }
    if (password !== confirm) {
      setError(t('auth.reset.mismatch'));
      return;
    }
    setSubmitting(true);
    try {
      await resetPassword(token, password);
      // 성공 → 로그인 페이지로 (재설정 완료 안내)
      sessionStorage.setItem('password_reset_done', '1');
      navigate('/login');
    } catch (err: any) {
      setError(err.isNetworkError ? t('error.network') : err.response?.data?.error || t('error.loadFailed'));
    } finally {
      setSubmitting(false);
    }
  };

  if (!token) {
    return (
      <div className="auth-page">
        <Link to="/" className="auth-logo">🐻 Taedibear Studio</Link>
        <div className="banner banner-warn">{t('auth.reset.invalidLink')}</div>
        <p className="auth-switch">
          <Link to="/forgot-password">{t('auth.reset.requestAgain')}</Link>
        </p>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <Link to="/" className="auth-logo">🐻 Taedibear Studio</Link>
      <h1>{t('auth.reset.title')}</h1>

      <form onSubmit={handleSubmit}>
        <div className="password-field">
          <input
            type={showPassword ? 'text' : 'password'}
            placeholder={t('auth.reset.newPassword')}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <button
            type="button"
            className="toggle-password"
            onClick={() => setShowPassword((v) => !v)}
          >
            {showPassword ? t('auth.field.hide') : t('auth.field.show')}
          </button>
        </div>
        <input
          type={showPassword ? 'text' : 'password'}
          placeholder={t('auth.reset.confirmPassword')}
          value={confirm}
          onChange={(e) => setConfirm(e.target.value)}
        />
        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? t('auth.reset.submitting') : t('auth.reset.submit')}
        </button>
      </form>
      {error && <p className="error">{error}</p>}
    </div>
  );
}
