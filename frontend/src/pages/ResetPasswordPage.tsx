import { useEffect, useState } from 'react';
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import { AxiosError } from 'axios';
import { toast } from 'sonner';
import { api } from '../shared/api';
import { useAuthStore } from '../entities/user';
import { Button, Input } from '../shared/ui';

function readTokenFromHash(): string {
  const hash = window.location.hash.startsWith('#')
    ? window.location.hash.slice(1)
    : window.location.hash;
  const params = new URLSearchParams(hash);
  return params.get('token') ?? '';
}

export function ResetPasswordPage() {
  const [email, setEmail] = useState('');
  const [token, setToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [requestPending, setRequestPending] = useState(false);
  const [confirmPending, setConfirmPending] = useState(false);
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((state) => state.authenticated);

  useEffect(() => {
    const fromHash = readTokenFromHash();
    const fromQuery = searchParams.get('token') ?? '';
    const initial = fromHash || fromQuery;
    if (initial) {
      setToken(initial);
    }
    if (fromHash || fromQuery) {
      // Drop token from address bar after reading (query legacy + fragment).
      window.history.replaceState(null, '', '/reset-password');
    }
  }, [searchParams]);

  const handleRequest = async (event: React.FormEvent) => {
    event.preventDefault();
    setRequestPending(true);
    setError('');
    setMessage('');

    try {
      await api.post('/auth/password-reset/request', { email });
      setMessage('If the account exists, a reset email has been sent.');
      toast.success('Reset request submitted');
    } catch (err: unknown) {
      if (err instanceof AxiosError) {
        setError(typeof err.response?.data === 'string' ? err.response.data : 'Reset request failed');
      } else {
        setError('An unexpected error occurred');
      }
    } finally {
      setRequestPending(false);
    }
  };

  const handleConfirm = async (event: React.FormEvent) => {
    event.preventDefault();
    setConfirmPending(true);
    setError('');
    setMessage('');

    try {
      await api.post('/auth/password-reset/confirm', {
        token,
        newPassword,
      });
      toast.success('Password updated');
      navigate('/login');
    } catch (err: unknown) {
      if (err instanceof AxiosError) {
        setError(typeof err.response?.data === 'string' ? err.response.data : 'Reset confirmation failed');
      } else {
        setError('An unexpected error occurred');
      }
    } finally {
      setConfirmPending(false);
    }
  };

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-[radial-gradient(circle_at_top,_rgba(37,99,235,0.12),_transparent_40%),hsl(var(--muted)/0.45)] p-4">
      <div className="w-full max-w-md space-y-8 rounded-2xl border bg-card p-8 shadow-sm">
        <div>
          <h1 className="mb-2 text-center text-2xl font-semibold tracking-tight">Reset password</h1>
          <p className="text-center text-sm text-muted-foreground">
            Request a reset link, then confirm with the token from email.
          </p>
        </div>

        {message ? (
          <div className="rounded-md bg-accent/20 p-3 text-sm text-foreground" role="status">
            {message}
          </div>
        ) : null}
        {error ? (
          <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive" role="alert">
            {error}
          </div>
        ) : null}

        <form onSubmit={handleRequest} className="space-y-3">
          <h2 className="text-sm font-semibold">1. Request link</h2>
          <Input
            label="Email"
            name="email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <Button type="submit" className="w-full" isLoading={requestPending}>
            Send reset email
          </Button>
        </form>

        <form onSubmit={handleConfirm} className="space-y-3">
          <h2 className="text-sm font-semibold">2. Confirm reset</h2>
          <Input
            label="Reset token"
            name="resetToken"
            value={token}
            onChange={(e) => setToken(e.target.value)}
            required
          />
          <Input
            label="New password"
            name="newPassword"
            type="password"
            autoComplete="new-password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            minLength={6}
            required
          />
          <Button type="submit" variant="secondary" className="w-full" isLoading={confirmPending}>
            Update password
          </Button>
        </form>

        <p className="text-center text-sm text-muted-foreground">
          <Link to="/login" className="font-medium text-primary hover:underline">
            Back to sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
