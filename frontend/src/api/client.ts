import axios from 'axios';
import type { AxiosRequestConfig } from 'axios';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:4000',
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// C4: access token 만료(401) 시 refresh 쿠키로 새 토큰을 받아 원래 요청을 1회 재시도한다.
// 동시에 여러 요청이 401을 받아도 refresh는 한 번만 호출 (single-flight) —
// refresh token은 회전(일회성)이라 병렬 호출 시 두 번째가 실패하기 때문.
let refreshPromise: Promise<string> | null = null;

const requestNewAccessToken = (): Promise<string> => {
  if (!refreshPromise) {
    refreshPromise = apiClient
      .post('/api/auth/refresh', null, { withCredentials: true })
      .then((res) => {
        const token: string = res.data.data.token;
        localStorage.setItem('token', token);
        return token;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
};

const redirectToLogin = () => {
  localStorage.removeItem('token');
  if (window.location.pathname !== '/login') {
    // 로그인 페이지에서 만료 안내 배너를 표시하기 위해 플래그 저장
    sessionStorage.setItem('session_expired', '1');
    window.location.href = '/login';
  }
};

// 세션 만료(401) 공통 처리: 토큰을 들고 보낸 요청이 401을 받으면 refresh를 시도하고,
// refresh까지 실패하면 로그인 페이지로 보낸다. 로그인/회원가입처럼 토큰 없이 보낸
// 요청의 401(아이디/비번 오류 등)은 각 페이지의 에러 처리에 맡기고 건드리지 않는다.
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    // 네트워크 오류 (서버 응답 없음 — 오프라인 또는 서버 다운)
    if (!error.response) {
      error.isNetworkError = true;
      return Promise.reject(error);
    }

    const originalRequest: (AxiosRequestConfig & { _retry?: boolean }) | undefined = error.config;
    const hadToken = Boolean(originalRequest?.headers?.Authorization);
    const isRefreshCall = originalRequest?.url === '/api/auth/refresh';

    if (error.response?.status === 401 && hadToken && !isRefreshCall) {
      if (originalRequest && !originalRequest._retry) {
        originalRequest._retry = true;
        try {
          const token = await requestNewAccessToken();
          originalRequest.headers = { ...originalRequest.headers, Authorization: `Bearer ${token}` };
          return apiClient.request(originalRequest);
        } catch {
          redirectToLogin();
        }
      } else {
        redirectToLogin();
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
