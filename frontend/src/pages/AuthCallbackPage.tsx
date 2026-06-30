import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// GET /api/auth/google/callback 처리 후 백엔드가 리다이렉트하는 /auth?token=<JWT> 를 받는 페이지
export default function AuthCallbackPage() {
  const [searchParams] = useSearchParams();
  const { loginWithToken } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState('');

  useEffect(() => {
    const token = searchParams.get('token');
    if (!token) {
      setError('로그인 정보를 받지 못했어요.');
      return;
    }

    loginWithToken(token)
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
