import { create } from 'zustand';

interface AuthUser {
  login: string;
  role: string;
}

interface AuthState {
  token: string | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  setAuth: (token: string, user: AuthUser) => void;
  clearAuth: () => void;
  initAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  user: null,
  isAuthenticated: false,

  setAuth: (token, user) => {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
    // Cookie lido pelo middleware (server-side) para proteger rotas
    document.cookie = `token=${token}; path=/; SameSite=Lax`;
    set({ token, user, isAuthenticated: true });
  },

  clearAuth: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    // Remove o cookie apagando sua data de expiração
    document.cookie = 'token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
    set({ token: null, user: null, isAuthenticated: false });
  },

  // Lê o token salvo no localStorage ao iniciar a aplicação
  initAuth: () => {
    if (typeof window === 'undefined') return;
    const token = localStorage.getItem('token');
    const userRaw = localStorage.getItem('user');
    if (token && userRaw) {
      try {
        const user = JSON.parse(userRaw) as AuthUser;
        // Garante que o cookie está sincronizado mesmo após reload da página
        document.cookie = `token=${token}; path=/; SameSite=Lax`;
        set({ token, user, isAuthenticated: true });
      } catch {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
      }
    }
  },
}));
