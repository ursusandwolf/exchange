import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../shared/api/base';
import { useAuthStore } from '../entities/user/model/authStore';

export const LoginPage: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const setAuth = useAuthStore((state) => state.setAuth);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const response = await api.post('/auth/login', { username, password });
      setAuth(response.data.token, response.data.username, response.data.userId);
      navigate('/');
    } catch (err: any) {
      const data = err.response?.data;
      if (typeof data === 'string') {
        setError(data);
      } else if (typeof data === 'object' && data !== null) {
        setError(Object.values(data).join(', '));
      } else {
        setError('Login failed');
      }
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-muted/50">
      <div className="w-full max-w-md p-8 bg-card rounded-lg border shadow-sm">
        <h2 className="text-2xl font-bold text-center mb-6">Login to Exchange</h2>
        {error && <div className="p-3 mb-4 text-sm text-destructive bg-destructive/10 rounded">{error}</div>}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <label className="text-sm font-medium">Username</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full p-2 border rounded bg-background"
              required
            />
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full p-2 border rounded bg-background"
              required
            />
          </div>
          <button
            type="submit"
            className="w-full py-2 bg-primary text-primary-foreground rounded font-medium hover:opacity-90 transition-opacity"
          >
            Sign In
          </button>
        </form>
        <p className="mt-4 text-center text-sm text-muted-foreground">
          Don't have an account? <span onClick={() => navigate('/register')} className="text-primary cursor-pointer hover:underline">Register</span>
        </p>
      </div>
    </div>
  );
};
