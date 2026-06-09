import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { Layout } from './Layout';
import { DashboardPage } from '../pages/DashboardPage';
import { LoginPage } from '../pages/LoginPage';
import { RegisterPage } from '../pages/RegisterPage';
import { ChangePasswordPage } from '../pages/ChangePasswordPage';
import { AdminPage } from '../pages/AdminPage';
import { ResetPasswordPage } from '../pages/ResetPasswordPage';
import { useAuthStore } from '../entities/user/model/authStore';
import { setupResponseInterceptors } from '../shared/api/base';
import { queryClient } from '../shared/api/queryClient';

import { Toaster } from 'sonner';

// Initialize API interceptors with logout callback to avoid circular dependency
setupResponseInterceptors(() => {
  queryClient.clear();
  useAuthStore.getState().logout();
});

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <Toaster position="top-right" richColors />
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/change-password" element={<ChangePasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/admin" element={<AdminPage />} />
          <Route path="/" element={<Layout />}>
            <Route index element={<DashboardPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
};
