import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  token: string | null;
  username: string | null;
  userId: string | null;
  admin: boolean;
  setAuth: (token: string, username: string, userId: string, admin: boolean) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      username: null,
      userId: null,
      admin: false,
      setAuth: (token, username, userId, admin) => {
        set({ token, username, userId, admin });
      },
      logout: () => {
        set({ token: null, username: null, userId: null, admin: false });
      },
    }),
    {
      name: 'auth-storage',
    }
  )
);
