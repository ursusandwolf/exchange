import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../entities/user';

export function RequireAuth({ children }: { children: React.ReactNode }) {
  const authenticated = useAuthStore((state) => state.authenticated);
  const location = useLocation();

  if (!authenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return children;
}
