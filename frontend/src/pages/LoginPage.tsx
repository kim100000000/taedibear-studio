import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { login } from '../api/auth';
import { useAuth } from '../context/AuthContext';
import type { FieldErrors } from '../types';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:4000';

function isValidEmail(value: string): boolean {
  return /\S+@\S+\.\S+/.test(value);
}

// P-02 로그인 (docs/03_화면설계서.md)
export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [sessionExpired, setSessionExpired] = useState(false);
  const [withdrawn, setWithdrawn] = useState(false);
  const [resetDone, setResetDone] = useState(false); // Phase 6: 비밀번호 재설정 완료 안내
  const { loginUser } = useAuth();
  const navigate = useNavigate();
  const { t } = useTranslation();

  // Phase 2-6: 세션 만료로 인한 자동 로그아웃 안내 + 회원 탈퇴 완료 안내
  useState(() => {
    if (sessionStorage.getItem('session_expired')) {
      setSessionExpired(true);
      sessionStorage.removeItem('session_expired');
    }
    if (new URLSearchParams(window.location.search).get('withdrawn') === 'true') {
      setWithdrawn(true);
    }
    if (sessionStorage.getItem('password_reset_done')) {
      setResetDone(true);
      sessionStorage.removeItem('password_reset_done');
    }
  });

  const validate = (): boolean => {
    const errors: FieldErrors = {};
    if (!email) errors.email = t('auth.validation.emailRequired');
    else if (!isValidEmail(email)) errors.email = t('auth.validation.emailFormat');

    if (!password) errors.password = t('auth.validation.passwordRequired');
    else if (password.length < 6) errors.password = t('auth.validation.passwordMin6');

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!validate()) return;

    setSubmitting(true);
    try {
      // docs/05_API명세서.md 공통 응답 구조: { success, data: { token, user } }
      const { data } = await login(email, password);
      loginUser(data.data.token, data.data.user);
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.response?.data?.error || t('auth.login.failed'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <Link to="/" className="auth-logo">🐻 Taedibear Studio</Link>
      {sessionExpired && (
        <div className="banner banner-warn">{t('error.sessionExpired')}</div>
      )}
      {withdrawn && (
        <div className="banner banner-info">{t('auth.login.withdrawn')}</div>
      )}
      {resetDone && (
        <div className="banner banner-info">{t('auth.reset.done')}</div>
      )}
      <h1>{t('auth.login.title')}</h1>

      <form onSubmit={handleSubmit}>
        <input
          type="email"
          placeholder={t('auth.field.email')}
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        {fieldErrors.email && <p className="field-error">{fieldErrors.email}</p>}

        <div className="password-field">
          <input
            type={showPassword ? 'text' : 'password'}
            placeholder={t('auth.field.password')}
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
        {fieldErrors.password && <p className="field-error">{fieldErrors.password}</p>}

        <Link to="/forgot-password" className="link-muted">
          {t('auth.login.forgotPassword')}
        </Link>

        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? t('auth.login.submitting') : t('auth.login.submit')}
        </button>
      </form>

      {error && <p className="error">{error}</p>}

      <div className="divider">{t('common.or')}</div>

      <a className="btn-social btn-google" href={`${API_URL}/api/auth/google`}>
        {t('auth.login.google')}
      </a>
      <a className="btn-social btn-kakao" href={`${API_URL}/api/auth/kakao`}>
        {t('auth.login.kakao')}
      </a>
      <a className="btn-social btn-naver" href={`${API_URL}/api/auth/naver`}>
        {t('auth.login.naver')}
      </a>

      <p className="auth-switch">
        {t('auth.login.noAccount')} <Link to="/signup">{t('auth.login.signupLink')}</Link>
      </p>
    </div>
  );
}
