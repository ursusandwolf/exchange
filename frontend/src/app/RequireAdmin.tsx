import { Navigate } from 'react-router-dom';
import { useAuthStore } from '../entities/user';
import { RequireAuth } from './RequireAuth';

export function RequireAdmin({ children }: { children: React.ReactNode }) {
  const admin = useAuthStore((state) => state.admin);

  return (
    <RequireAuth>
      {admin ? children : <Navigate to="/" replace />}
    </RequireAuth>
  );
}
