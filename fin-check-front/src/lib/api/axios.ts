import axios from 'axios';
import { useAuthStore } from '@/lib/stores/authStore';

// Sem baseURL: o browser usa URL relativa (/api/*) e o servidor Next.js
// encaminha via proxy server-side (next.config.mjs rewrites) para http://api:8080.
const api = axios.create({
  headers: { 'Content-Type': 'application/json' },
});

// Injeta o Bearer token em todas as requisições
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Se receber 401, limpa o estado e redireciona para login
api.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().clearAuth();
      if (typeof window !== 'undefined') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
