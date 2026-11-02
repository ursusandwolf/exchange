import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { AxiosError } from 'axios';
import { api } from '../shared/api';
import type { AuthResponse } from '../shared/api/types';
import { useAuthStore } from '../entities/user';
import { Button, Input } from '../shared/ui';

export function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const setAuth = useAuthStore((state) => state.setAuth);
  const from = (location.state as { from?: string } | null)?.from ?? '/';

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setPending(true);
    setError('');
    try {
      const response = await api.post<AuthResponse>('/auth/login', { username, password });
      setAuth(response.data.username, response.data.userId, response.data.admin);
      navigate(from, { replace: true });
    } catch (err: unknown) {
      if (err instanceof AxiosError) {
        const data = err.response?.data;
        if (typeof data === 'string') setError(data);
        else if (typeof data === 'object' && data !== null) setError(Object.values(data).join(', '));
        else setError('Login failed');
      } else {
        setError('An unexpected error occurred');
      }
    } finally {
      setPending(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-[radial-gradient(circle_at_top,_rgba(37,99,235,0.12),_transparent_40%),hsl(var(--muted)/0.45)] p-4">
      <div className="w-full max-w-md rounded-2xl border bg-card p-8 shadow-sm">
        <h1 className="mb-2 text-center text-2xl font-semibold tracking-tight">Sign in</h1>
        <p className="mb-6 text-center text-sm text-muted-foreground">Access the Gemini Exchange spot desk</p>
        {error ? (
          <div className="mb-4 rounded-md bg-destructive/10 p-3 text-sm text-destructive" role="alert">
            {error}
          </div>
        ) : null}
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Username"
            name="username"
            autoComplete="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
          <Input
            label="Password"
            name="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          <Button type="submit" className="w-full" isLoading={pending}>
            Sign in
          </Button>
        </form>
        <p className="mt-4 text-center text-sm text-muted-foreground">
          No account?{' '}
          <Link to="/register" className="font-medium text-primary hover:underline">
            Register
          </Link>
        </p>
        <p className="mt-2 text-center text-sm text-muted-foreground">
          Forgot password?{' '}
          <Link to="/reset-password" className="font-medium text-primary hover:underline">
            Reset it
          </Link>
        </p>
      </div>
    </div>
  );
}
