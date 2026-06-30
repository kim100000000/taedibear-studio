import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { getMe } from '../api/auth';
import type { User } from '../types';

interface AuthContextValue {
  user: User | null;
  loading: boolean;
  loginUser: (token: string, userData: User) => void;
  loginWithToken: (token: string) => Promise<void>;
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

  // 구글 로그인 콜백(/auth?token=) 처럼 토큰만 받고 사용자 정보는 /api/users/me로 따로 가져와야 할 때 사용
  const loginWithToken = async (token: string) => {
    localStorage.setItem('token', token);
    const res = await getMe();
    setUser(res.data.data);
  };

  const logout = () => {
    localStorage.removeItem('token');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, loginUser, loginWithToken, logout }}>
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
