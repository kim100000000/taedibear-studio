import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { register } from '../api/auth';
import { useAuth } from '../context/AuthContext';
import type { FieldErrors } from '../types';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:4000';

function isValidEmail(value: string): boolean {
  return /\S+@\S+\.\S+/.test(value);
}

// P-03 회원가입 (docs/03_화면설계서.md)
export default function SignupPage() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [agreed, setAgreed] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const { loginUser } = useAuth();
  const navigate = useNavigate();
  const { t } = useTranslation();

  // 약함 / 보통 / 강함 — 길이 + 문자 종류 조합으로 간단히 판단
  const getStrengthLabel = (pw: string): { label: string; className: string } | null => {
    if (!pw) return null;
    let score = 0;
    if (pw.length >= 8) score += 1;
    if (pw.length >= 12) score += 1;
    if (/[A-Z]/.test(pw) && /[a-z]/.test(pw)) score += 1;
    if (/[0-9]/.test(pw)) score += 1;
    if (/[^A-Za-z0-9]/.test(pw)) score += 1;

    if (score <= 1) return { label: t('auth.signup.weak'), className: 'weak' };
    if (score <= 3) return { label: t('auth.signup.medium'), className: 'medium' };
    return { label: t('auth.signup.strong'), className: 'strong' };
  };

  const strength = getStrengthLabel(password);

  const validate = (): boolean => {
    const errors: FieldErrors = {};
    if (!name || name.trim().length < 2) errors.name = t('auth.validation.nameMin2');
    if (!email) errors.email = t('auth.validation.emailRequired');
    else if (!isValidEmail(email)) errors.email = t('auth.validation.emailFormat');

    if (!password) errors.password = t('auth.validation.passwordRequired');
    else if (password.length < 8) errors.password = t('auth.validation.passwordMin8');

    if (passwordConfirm !== password) errors.passwordConfirm = t('auth.validation.passwordMatch');
    if (!agreed) errors.agreed = t('auth.validation.agreeRequired');

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
      const { data } = await register(name, email, password);
      loginUser(data.data.token, data.data.user);
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.response?.data?.error || t('auth.signup.failed'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-logo">🐻 Taedibear Studio</div>
      <h1>{t('auth.signup.title')}</h1>

      <form onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder={t('auth.field.name')}
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        {fieldErrors.name && <p className="field-error">{fieldErrors.name}</p>}

        <input
          type="email"
          placeholder={t('auth.field.email')}
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        {fieldErrors.email && <p className="field-error">{fieldErrors.email}</p>}

        <input
          type="password"
          placeholder={t('auth.field.passwordHint')}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        {strength && (
          <p className={`password-strength ${strength.className}`}>
            {t('auth.signup.strengthLabel', { level: strength.label })}
          </p>
        )}
        {fieldErrors.password && <p className="field-error">{fieldErrors.password}</p>}

        <input
          type="password"
          placeholder={t('auth.field.passwordConfirm')}
          value={passwordConfirm}
          onChange={(e) => setPasswordConfirm(e.target.value)}
        />
        {fieldErrors.passwordConfirm && <p className="field-error">{fieldErrors.passwordConfirm}</p>}

        <label className="checkbox-field">
          <input
            type="checkbox"
            checked={agreed}
            onChange={(e) => setAgreed(e.target.checked)}
          />
          <span>{t('auth.signup.agreeTerms')}</span>
        </label>
        {fieldErrors.agreed && <p className="field-error">{fieldErrors.agreed}</p>}

        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? t('auth.signup.submitting') : t('auth.signup.submit')}
        </button>
      </form>

      {error && <p className="error">{error}</p>}

      <div className="divider">{t('common.or')}</div>

      <a className="btn-social btn-google" href={`${API_URL}/api/auth/google`}>
        {t('auth.signup.google')}
      </a>
      <a className="btn-social btn-kakao" href={`${API_URL}/api/auth/kakao`}>
        {t('auth.signup.kakao')}
      </a>
      <a className="btn-social btn-naver" href={`${API_URL}/api/auth/naver`}>
        {t('auth.signup.naver')}
      </a>

      <p className="auth-switch">
        {t('auth.signup.hasAccount')} <Link to="/login">{t('auth.signup.loginLink')}</Link>
      </p>
    </div>
  );
}
