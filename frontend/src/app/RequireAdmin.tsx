import { Navigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../shared/api';
import { EmptyState } from '../shared/ui';
import { RequireAuth } from './RequireAuth';

export function RequireAdmin({ children }: { children: React.ReactNode }) {
  return (
    <RequireAuth>
      <AdminGate>{children}</AdminGate>
    </RequireAuth>
  );
}

function AdminGate({ children }: { children: React.ReactNode }) {
  const gateQuery = useQuery({
    queryKey: ['admin-gate'],
    queryFn: async () => (await api.get('/admin/stats')).data,
    retry: false,
  });

  if (gateQuery.isLoading) {
    return <EmptyState title="Checking admin access…" />;
  }

  if (gateQuery.isError) {
    return <Navigate to="/" replace />;
  }

  return children;
}
