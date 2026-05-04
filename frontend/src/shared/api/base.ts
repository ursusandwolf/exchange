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
      const { state } = JSON.parse(authData);
      if (state.token) {
        config.headers.Authorization = `Bearer ${state.token}`;
      }
    } catch {
      // silent fail
    }
  }
  return config;
});

export const setupResponseInterceptors = (onUnauthorized: () => void) => {
  api.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error.response?.status === 401 || error.response?.status === 403) {
        onUnauthorized();
      }
      return Promise.reject(error);
    }
  );
};

export default api;
