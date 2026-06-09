import React, { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { AxiosError } from 'axios';
import { toast } from 'sonner';
import { api } from '../shared/api';
import { useAuthStore } from '../entities/user';

export const ChangePasswordPage: React.FC = () => {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const token = useAuthStore((state) => state.token);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
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
        if (typeof data === 'string') {
          setError(data);
        } else if (typeof data === 'object' && data !== null) {
          setError(Object.values(data).join(', '));
        } else {
          setError('Password update failed');
        }
      } else {
        setError('An unexpected error occurred');
      }
    }
  };

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-muted/50">
      <div className="w-full max-w-md p-8 bg-card rounded-lg border shadow-sm">
        <h2 className="text-2xl font-bold text-center mb-6">Change Password</h2>
        {error && <div className="p-3 mb-4 text-sm text-destructive bg-destructive/10 rounded">{error}</div>}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <label className="text-sm font-medium">Current password</label>
            <input
              type="password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              className="w-full p-2 border rounded bg-background"
              required
            />
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium">New password</label>
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              className="w-full p-2 border rounded bg-background"
              required
              minLength={6}
            />
          </div>
          <button
            type="submit"
            className="w-full py-2 bg-primary text-primary-foreground rounded font-medium hover:opacity-90 transition-opacity"
          >
            Update password
          </button>
        </form>
      </div>
    </div>
  );
};
