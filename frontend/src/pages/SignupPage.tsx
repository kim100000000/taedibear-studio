import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { register } from '../api/auth';
import { useAuth } from '../context/AuthContext';
import type { FieldErrors } from '../types';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:4000';

function isValidEmail(value: string): boolean {
  return /\S+@\S+\.\S+/.test(value);
}

interface PasswordStrength {
  label: string;
  className: string;
}

// 약함 / 보통 / 강함 — 길이 + 문자 종류 조합으로 간단히 판단
function getPasswordStrength(password: string): PasswordStrength | null {
  if (!password) return null;
  let score = 0;
  if (password.length >= 8) score += 1;
  if (password.length >= 12) score += 1;
  if (/[A-Z]/.test(password) && /[a-z]/.test(password)) score += 1;
  if (/[0-9]/.test(password)) score += 1;
  if (/[^A-Za-z0-9]/.test(password)) score += 1;

  if (score <= 1) return { label: '약함', className: 'weak' };
  if (score <= 3) return { label: '보통', className: 'medium' };
  return { label: '강함', className: 'strong' };
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

  const strength = getPasswordStrength(password);

  const validate = (): boolean => {
    const errors: FieldErrors = {};
    if (!name || name.trim().length < 2) errors.name = '이름은 2자 이상이어야 해요.';
    if (!email) errors.email = '이메일을 입력해주세요.';
    else if (!isValidEmail(email)) errors.email = '이메일 형식이 올바르지 않아요.';

    if (!password) errors.password = '비밀번호를 입력해주세요.';
    else if (password.length < 8) errors.password = '비밀번호는 8자 이상이어야 해요.';

    if (passwordConfirm !== password) errors.passwordConfirm = '비밀번호가 일치하지 않아요.';
    if (!agreed) errors.agreed = '이용약관에 동의해주세요.';

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
      setError(err.response?.data?.error || '회원가입에 실패했어요.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-logo">🐻 Taedibear Studio</div>
      <h1>회원가입</h1>

      <form onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="이름"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        {fieldErrors.name && <p className="field-error">{fieldErrors.name}</p>}

        <input
          type="email"
          placeholder="이메일"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        {fieldErrors.email && <p className="field-error">{fieldErrors.email}</p>}

        <input
          type="password"
          placeholder="비밀번호 (8자 이상)"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        {strength && <p className={`password-strength ${strength.className}`}>강도: {strength.label}</p>}
        {fieldErrors.password && <p className="field-error">{fieldErrors.password}</p>}

        <input
          type="password"
          placeholder="비밀번호 확인"
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
          <span>이용약관 및 개인정보처리방침에 동의합니다.</span>
        </label>
        {fieldErrors.agreed && <p className="field-error">{fieldErrors.agreed}</p>}

        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? '가입 중...' : '회원가입'}
        </button>
      </form>

      {error && <p className="error">{error}</p>}

      <div className="divider">또는</div>

      <a className="btn-social btn-google" href={`${API_URL}/api/auth/google`}>
        구글 가입하기
      </a>
      <a className="btn-social btn-kakao" href={`${API_URL}/api/auth/kakao`}>
        카카오 가입하기
      </a>
      <a className="btn-social btn-naver" href={`${API_URL}/api/auth/naver`}>
        네이버 가입하기
      </a>

      <p className="auth-switch">
        이미 계정이 있으신가요? <Link to="/login">로그인</Link>
      </p>
    </div>
  );
}
