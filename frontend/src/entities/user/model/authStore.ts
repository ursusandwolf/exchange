import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  /** Session marker only — JWT lives in httpOnly cookie, not here. */
  authenticated: boolean;
  username: string | null;
  userId: string | null;
  admin: boolean;
  setAuth: (username: string, userId: string, admin: boolean) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      authenticated: false,
      username: null,
      userId: null,
      admin: false,
      setAuth: (username, userId, admin) => {
        set({ authenticated: true, username, userId, admin });
      },
      logout: () => {
        set({ authenticated: false, username: null, userId: null, admin: false });
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        authenticated: state.authenticated,
        username: state.username,
        userId: state.userId,
        admin: state.admin,
      }),
    }
  )
);
