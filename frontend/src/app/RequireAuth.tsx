import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../entities/user';

export function RequireAuth({ children }: { children: React.ReactNode }) {
  const token = useAuthStore((state) => state.token);
  const location = useLocation();

  if (!token) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return children;
}
