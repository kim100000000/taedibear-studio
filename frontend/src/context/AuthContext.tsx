import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { getMe, refreshAccessToken, logoutServer } from '../api/auth';
import type { User } from '../types';

interface AuthContextValue {
  user: User | null;
  loading: boolean;
  loginUser: (token: string, userData: User) => void;
  loginFromCallback: () => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      setLoading(false);
      return;
    }
    getMe()
      // docs/05_API명세서.md 공통 응답 구조: { success, data: {...} }
      .then((res) => setUser(res.data.data))
      .catch(() => localStorage.removeItem('token'))
      .finally(() => setLoading(false));
  }, []);

  const loginUser = (token: string, userData: User) => {
    localStorage.setItem('token', token);
    setUser(userData);
  };

  // C6: 소셜 로그인 콜백(/auth) — 백엔드가 심어준 refresh 쿠키(HttpOnly)로
  // access token을 교환한 뒤 사용자 정보를 가져온다 (URL에 토큰이 실리지 않음).
  const loginFromCallback = async () => {
    const refreshRes = await refreshAccessToken();
    localStorage.setItem('token', refreshRes.data.data.token);
    const res = await getMe();
    setUser(res.data.data);
  };

  // C4/M9: 서버측 refresh token도 폐기 (실패해도 로컬 로그아웃은 진행)
  const logout = () => {
    logoutServer().catch(() => {});
    localStorage.removeItem('token');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, loginUser, loginFromCallback, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = (): AuthContextValue => {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth는 AuthProvider 안에서만 사용할 수 있어요.');
  }
  return ctx;
};
