import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AxiosError } from 'axios';
import { api } from '../shared/api';
import type { AuthResponse } from '../shared/api/types';
import { useAuthStore } from '../entities/user';
import { Button, Input } from '../shared/ui';

export function RegisterPage() {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);
  const navigate = useNavigate();
  const setAuth = useAuthStore((state) => state.setAuth);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setPending(true);
    setError('');
    try {
      const response = await api.post<AuthResponse>('/auth/register', { username, email, password });
      setAuth(response.data.token, response.data.username, response.data.userId, response.data.admin);
      navigate('/');
    } catch (err: unknown) {
      if (err instanceof AxiosError) {
        const data = err.response?.data;
        if (typeof data === 'string') setError(data);
        else if (typeof data === 'object' && data !== null) setError(Object.values(data).join(', '));
        else setError('Registration failed');
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
        <h1 className="mb-2 text-center text-2xl font-semibold tracking-tight">Create account</h1>
        <p className="mb-6 text-center text-sm text-muted-foreground">Start trading on the simulation desk</p>
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
            label="Email"
            name="email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <Input
            label="Password"
            name="password"
            type="password"
            autoComplete="new-password"
            minLength={6}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          <Button type="submit" className="w-full" isLoading={pending}>
            Register
          </Button>
        </form>
        <p className="mt-4 text-center text-sm text-muted-foreground">
          Already registered?{' '}
          <Link to="/login" className="font-medium text-primary hover:underline">
            Login
          </Link>
        </p>
      </div>
    </div>
  );
}
