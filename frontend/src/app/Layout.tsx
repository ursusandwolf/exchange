import React from 'react';
import { useAuthStore } from '../entities/user/model/authStore';
import { Navigate, Outlet } from 'react-router-dom';

export const Layout: React.FC = () => {
  const token = useAuthStore((state) => state.token);
  const logout = useAuthStore((state) => state.logout);

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="min-h-screen bg-background flex flex-col">
      <header className="border-b px-6 py-4 flex justify-between items-center bg-card">
        <h1 className="text-xl font-bold text-primary">Gemini Exchange</h1>
        <div className="flex items-center gap-4">
          <span className="text-muted-foreground">{useAuthStore.getState().username}</span>
          <button
            onClick={logout}
            className="px-3 py-1 text-sm border rounded hover:bg-accent transition-colors"
          >
            Logout
          </button>
        </div>
      </header>
      <main className="flex-1 p-6">
        <Outlet />
      </main>
    </div>
  );
};
