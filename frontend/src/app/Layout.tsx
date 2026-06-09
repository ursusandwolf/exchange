import React from 'react';
import { useAuthStore } from '../entities/user';
import { Outlet, Link } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';

export const Layout: React.FC = () => {
  const token = useAuthStore((state) => state.token);
  const username = useAuthStore((state) => state.username);
  const admin = useAuthStore((state) => state.admin);
  const logout = useAuthStore((state) => state.logout);
  const queryClient = useQueryClient();

  const handleLogout = () => {
    queryClient.clear();
    logout();
  };

  return (
    <div className="min-h-screen bg-background flex flex-col">
      <header className="border-b px-6 py-4 flex justify-between items-center bg-card">
        <Link to="/" className="text-xl font-bold text-primary">Gemini Exchange</Link>
        <div className="flex items-center gap-4">
          {token ? (
            <>
              <span className="text-muted-foreground">{username}</span>
              {admin && (
                <Link to="/admin" className="text-sm font-medium hover:text-primary">
                  Admin
                </Link>
              )}
              <Link to="/change-password" className="text-sm font-medium hover:text-primary">
                Change password
              </Link>
              <button
                onClick={handleLogout}
                className="px-3 py-1 text-sm border rounded hover:bg-accent transition-colors"
              >
                Logout
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="text-sm font-medium hover:text-primary">
                Login
              </Link>
              <Link
                to="/register"
                className="px-4 py-1.5 text-sm bg-primary text-primary-foreground rounded-md font-medium hover:opacity-90 transition-opacity"
              >
                Register
              </Link>
            </>
          )}
        </div>
      </header>
      <main className="flex-1 p-6">
        <Outlet />
      </main>
    </div>
  );
};
