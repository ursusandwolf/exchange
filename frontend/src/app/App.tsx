import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'sonner';
import { Layout } from './Layout';
import { RequireAuth } from './RequireAuth';
import { RequireAdmin } from './RequireAdmin';
import { DashboardPage } from '../pages/DashboardPage';
import { LoginPage } from '../pages/LoginPage';
import { RegisterPage } from '../pages/RegisterPage';
import { ChangePasswordPage } from '../pages/ChangePasswordPage';
import { AdminPage } from '../pages/AdminPage';
import { ResetPasswordPage } from '../pages/ResetPasswordPage';
import { useAuthStore } from '../entities/user/model/authStore';
import api, { setupResponseInterceptors } from '../shared/api/base';
import { queryClient } from '../shared/api/queryClient';

setupResponseInterceptors(() => {
  queryClient.clear();
  useAuthStore.getState().logout();
  void api.post('/auth/logout').catch(() => undefined);
});

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Toaster position="top-right" richColors />
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route
            path="/change-password"
            element={
              <RequireAuth>
                <ChangePasswordPage />
              </RequireAuth>
            }
          />
          <Route
            path="/admin"
            element={
              <RequireAdmin>
                <AdminPage />
              </RequireAdmin>
            }
          />
          <Route path="/" element={<Layout />}>
            <Route index element={<DashboardPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
