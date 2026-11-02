import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { AxiosError } from 'axios';
import { toast } from 'sonner';
import { api } from '../shared/api';
import { useAuthStore } from '../entities/user';
import { Button, Input } from '../shared/ui';

export function ChangePasswordPage() {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);
  const navigate = useNavigate();
  const authenticated = useAuthStore((state) => state.authenticated);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setPending(true);
    setError('');

    try {
      await api.put('/user/password', { currentPassword, newPassword });
      toast.success('Password updated');
      setCurrentPassword('');
      setNewPassword('');
      navigate('/');
    } catch (err: unknown) {
      if (err instanceof AxiosError) {
        const data = err.response?.data;
        if (typeof data === 'string') setError(data);
        else if (typeof data === 'object' && data !== null) setError(Object.values(data).join(', '));
        else setError('Password update failed');
      } else {
        setError('An unexpected error occurred');
      }
    } finally {
      setPending(false);
    }
  };

  if (!authenticated) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-[radial-gradient(circle_at_top,_rgba(37,99,235,0.12),_transparent_40%),hsl(var(--muted)/0.45)] p-4">
      <div className="w-full max-w-md rounded-2xl border bg-card p-8 shadow-sm">
        <h1 className="mb-2 text-center text-2xl font-semibold tracking-tight">Change password</h1>
        <p className="mb-6 text-center text-sm text-muted-foreground">Update credentials for your exchange account</p>
        {error ? (
          <div className="mb-4 rounded-md bg-destructive/10 p-3 text-sm text-destructive" role="alert">
            {error}
          </div>
        ) : null}
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Current password"
            name="currentPassword"
            type="password"
            autoComplete="current-password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            required
          />
          <Input
            label="New password"
            name="newPassword"
            type="password"
            autoComplete="new-password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            required
            minLength={6}
          />
          <Button type="submit" className="w-full" isLoading={pending}>
            Update password
          </Button>
        </form>
        <p className="mt-4 text-center text-sm text-muted-foreground">
          <Link to="/" className="font-medium text-primary hover:underline">
            Back to desk
          </Link>
        </p>
      </div>
    </div>
  );
}
