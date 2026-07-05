import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// 소셜 로그인 콜백 처리 후 백엔드가 리다이렉트하는 /auth 페이지.
// C6: JWT를 URL로 받지 않는다 — 백엔드가 심은 refresh 쿠키(HttpOnly)로 access token을 교환한다.
export default function AuthCallbackPage() {
  const { loginFromCallback } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState('');

  useEffect(() => {
    loginFromCallback()
      .then(() => navigate('/dashboard'))
      .catch(() => setError('로그인에 실패했어요. 다시 시도해주세요.'));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="auth-page">
      {error ? <p className="error">{error}</p> : <p>로그인 처리 중이에요...</p>}
    </div>
  );
}
