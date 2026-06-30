import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
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
  const { loginUser } = useAuth();
  const navigate = useNavigate();

  const validate = (): boolean => {
    const errors: FieldErrors = {};
    if (!email) errors.email = '이메일을 입력해주세요.';
    else if (!isValidEmail(email)) errors.email = '이메일 형식이 올바르지 않아요.';

    if (!password) errors.password = '비밀번호를 입력해주세요.';
    else if (password.length < 6) errors.password = '비밀번호는 6자 이상이어야 해요.';

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
      setError(err.response?.data?.error || '로그인에 실패했어요.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-logo">🐻 Taedibear Studio</div>
      <h1>로그인</h1>

      <form onSubmit={handleSubmit}>
        <input
          type="email"
          placeholder="이메일"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        {fieldErrors.email && <p className="field-error">{fieldErrors.email}</p>}

        <div className="password-field">
          <input
            type={showPassword ? 'text' : 'password'}
            placeholder="비밀번호"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <button
            type="button"
            className="toggle-password"
            onClick={() => setShowPassword((v) => !v)}
          >
            {showPassword ? '숨기기' : '보기'}
          </button>
        </div>
        {fieldErrors.password && <p className="field-error">{fieldErrors.password}</p>}

        <Link to="/forgot-password" className="link-muted">
          비밀번호를 잊으셨나요?
        </Link>

        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? '로그인 중...' : '로그인'}
        </button>
      </form>

      {error && <p className="error">{error}</p>}

      <div className="divider">또는</div>

      <a className="btn-social btn-google" href={`${API_URL}/api/auth/google`}>
        구글 로그인
      </a>
      <a className="btn-social btn-kakao" href={`${API_URL}/api/auth/kakao`}>
        카카오 로그인
      </a>
      <a className="btn-social btn-naver" href={`${API_URL}/api/auth/naver`}>
        네이버 로그인
      </a>

      <p className="auth-switch">
        아직 계정이 없으신가요? <Link to="/signup">회원가입</Link>
      </p>
    </div>
  );
}
