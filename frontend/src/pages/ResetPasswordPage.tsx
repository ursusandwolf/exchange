import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate, Navigate } from 'react-router-dom';
import { AxiosError } from 'axios';
import { toast } from 'sonner';
import { api } from '../shared/api';
import { useAuthStore } from '../entities/user';

export const ResetPasswordPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [token, setToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((state) => !!state.token);

  const initialToken = searchParams.get('token') ?? '';

  useEffect(() => {
    if (initialToken) {
      setToken(initialToken);
    }
  }, [initialToken]);

  const handleRequest = async (e: React.FormEvent) => {
    e.preventDefault();
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
    }
  };

  const handleConfirm = async (e: React.FormEvent) => {
    e.preventDefault();
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
    }
  };

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-muted/50">
      <div className="w-full max-w-md p-8 bg-card rounded-lg border shadow-sm space-y-8">
        <div>
          <h2 className="text-2xl font-bold text-center mb-2">Reset Password</h2>
          <p className="text-sm text-muted-foreground text-center">
            Request a reset link, then confirm with the token from email.
          </p>
        </div>

        {message && <div className="p-3 text-sm text-foreground bg-accent/20 rounded">{message}</div>}
        {error && <div className="p-3 text-sm text-destructive bg-destructive/10 rounded">{error}</div>}

        <form onSubmit={handleRequest} className="space-y-3">
          <h3 className="font-medium">1. Request link</h3>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="Email"
            className="w-full p-2 border rounded bg-background"
            required
          />
          <button type="submit" className="w-full py-2 bg-primary text-primary-foreground rounded font-medium">
            Send reset email
          </button>
        </form>

        <form onSubmit={handleConfirm} className="space-y-3">
          <h3 className="font-medium">2. Confirm reset</h3>
          <input
            type="text"
            value={token}
            onChange={(e) => setToken(e.target.value)}
            placeholder="Reset token"
            className="w-full p-2 border rounded bg-background"
            required
          />
          <input
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            placeholder="New password"
            minLength={6}
            className="w-full p-2 border rounded bg-background"
            required
          />
          <button type="submit" className="w-full py-2 border rounded font-medium hover:bg-accent transition-colors">
            Update password
          </button>
        </form>
      </div>
    </div>
  );
};
