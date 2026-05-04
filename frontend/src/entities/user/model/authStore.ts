import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  token: string | null;
  username: string | null;
  userId: string | null;
  setAuth: (token: string, username: string, userId: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      username: null,
      userId: null,
      setAuth: (token, username, userId) => {
        set({ token, username, userId });
      },
      logout: () => {
        set({ token: null, username: null, userId: null });
      },
    }),
    {
      name: 'auth-storage',
    }
  )
);
