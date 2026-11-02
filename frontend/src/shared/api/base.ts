import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const authData = localStorage.getItem('auth-storage');
  if (authData) {
    try {
      const { state } = JSON.parse(authData) as { state?: { token?: string } };
      if (state?.token) {
        config.headers.Authorization = `Bearer ${state.token}`;
      }
    } catch {
      // ignore malformed persisted auth
    }
  }
  return config;
});

export const setupResponseInterceptors = (onUnauthorized: () => void) => {
  api.interceptors.response.use(
    (response) => response,
    (error) => {
      // Only revoke session on auth failure; keep session on 403 Forbidden
      if (error.response?.status === 401) {
        onUnauthorized();
      }
      return Promise.reject(error);
    }
  );
};

export default api;
