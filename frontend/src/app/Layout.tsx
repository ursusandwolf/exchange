import { Link, Outlet } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '../entities/user';
import { Button } from '../shared/ui';

export function Layout() {
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
    <div className="flex min-h-screen flex-col bg-[radial-gradient(circle_at_top,_rgba(37,99,235,0.08),_transparent_42%),hsl(var(--background))]">
      <header className="sticky top-0 z-20 border-b border-border/70 bg-card/90 px-4 py-3 backdrop-blur sm:px-6">
        <div className="mx-auto flex max-w-[1400px] items-center justify-between gap-4">
          <Link to="/" className="text-lg font-semibold tracking-tight text-foreground sm:text-xl">
            Gemini Exchange
          </Link>
          <nav className="flex items-center gap-2 sm:gap-3" aria-label="Account">
            {token ? (
              <>
                <span className="hidden text-sm text-muted-foreground sm:inline">{username}</span>
                {admin ? (
                  <Link to="/admin" className="text-sm font-medium hover:text-primary">
                    Admin
                  </Link>
                ) : null}
                <Link to="/change-password" className="text-sm font-medium hover:text-primary">
                  Password
                </Link>
                <Button variant="secondary" onClick={handleLogout} className="px-3 py-1.5">
                  Logout
                </Button>
              </>
            ) : (
              <>
                <Link to="/login" className="text-sm font-medium hover:text-primary">
                  Login
                </Link>
                <Link
                  to="/register"
                  className="rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground"
                >
                  Register
                </Link>
              </>
            )}
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-[1400px] flex-1 p-4 sm:p-6">
        <Outlet />
      </main>
    </div>
  );
}
