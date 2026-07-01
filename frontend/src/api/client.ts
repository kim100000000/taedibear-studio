import axios from 'axios';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:4000',
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// 세션 만료(401) 공통 처리: 토큰을 들고 보낸 요청이 401을 받으면
// 토큰을 지우고 로그인 페이지로 보낸다. 로그인/회원가입처럼 토큰 없이 보낸
// 요청의 401(아이디/비번 오류 등)은 각 페이지의 에러 처리에 맡기고 건드리지 않는다.
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // 네트워크 오류 (서버 응답 없음 — 오프라인 또는 서버 다운)
    if (!error.response) {
      error.isNetworkError = true;
      return Promise.reject(error);
    }

    const hadToken = Boolean(error.config?.headers?.Authorization);
    if (error.response?.status === 401 && hadToken) {
      localStorage.removeItem('token');
      if (window.location.pathname !== '/login') {
        // 로그인 페이지에서 만료 안내 배너를 표시하기 위해 플래그 저장
        sessionStorage.setItem('session_expired', '1');
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
