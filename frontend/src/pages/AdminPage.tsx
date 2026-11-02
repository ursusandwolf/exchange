import React, { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../shared/api';
import { useAuthStore } from '../entities/user';
import { Button, EmptyState, Input, Panel } from '../shared/ui';
import { toast } from 'sonner';

type AdminStatsResponse = {
  totalUsers: number;
  totalOrders: number;
  activeOrders: number;
  adminUsers: number;
  exchangeProfit: Record<string, number>;
  systemUserPresent: boolean;
};

type AdminUserResponse = {
  userId: string;
  username: string;
  admin: boolean;
  balances: Record<string, number>;
  reserved: Record<string, number>;
};

type UserFilter = 'all' | 'admins' | 'users';

export const AdminPage: React.FC = () => {
  const admin = useAuthStore((state) => state.admin);
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<UserFilter>('all');

  const statsQuery = useQuery({
    queryKey: ['admin-stats'],
    queryFn: async () => (await api.get<AdminStatsResponse>('/admin/stats')).data,
    enabled: admin,
  });

  const usersQuery = useQuery({
    queryKey: ['admin-users'],
    queryFn: async () => (await api.get<AdminUserResponse[]>('/admin/users')).data,
    enabled: admin,
  });

  const toggleAdminMutation = useMutation({
    mutationFn: async ({ userId, admin }: { userId: string; admin: boolean }) => {
      const response = await api.patch<AdminUserResponse>(`/admin/users/${userId}/admin`, { admin });
      return response.data;
    },
    onSuccess: async (_, variables) => {
      await queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      await queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
      toast.success(variables.admin ? 'User promoted to admin' : 'Admin role revoked');
    },
    onError: () => {
      toast.error('Failed to update user role');
    },
  });

  if (!admin) {
    return <Navigate to="/" replace />;
  }

  const stats = statsQuery.data;
  const users = usersQuery.data ?? [];
  const filteredUsers = users
    .filter((user) => {
      const matchesSearch =
        search.trim().length === 0 ||
        user.username.toLowerCase().includes(search.trim().toLowerCase()) ||
        user.userId.toLowerCase().includes(search.trim().toLowerCase());
      const matchesFilter =
        filter === 'all' ||
        (filter === 'admins' && user.admin) ||
        (filter === 'users' && !user.admin);
      return matchesSearch && matchesFilter;
    })
    .sort((left, right) => {
      if (left.admin !== right.admin) return left.admin ? -1 : 1;
      return left.username.localeCompare(right.username);
    });

  const totalReserved = Object.values(stats?.exchangeProfit ?? {}).reduce((sum, value) => sum + value, 0);

  return (
    <div className="space-y-6">
      <section className="relative overflow-hidden rounded-2xl border bg-gradient-to-br from-slate-950 via-slate-900 to-slate-800 p-6 text-slate-100 shadow-lg">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(56,189,248,0.18),transparent_35%),radial-gradient(circle_at_bottom_left,rgba(16,185,129,0.12),transparent_30%)]" />
        <div className="relative flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div className="space-y-3">
            <p className="text-xs uppercase tracking-[0.35em] text-sky-300/80">Admin control room</p>
            <div className="space-y-2">
              <h1 className="text-3xl font-black tracking-tight sm:text-4xl">User management dashboard</h1>
              <p className="max-w-2xl text-sm text-slate-300">
                Monitor exchange health, inspect wallet exposure, and promote or revoke admin access without leaving the
                panel.
              </p>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3 text-sm">
            <MiniStat label="Users" value={stats?.totalUsers ?? 0} />
            <MiniStat label="Admins" value={stats?.adminUsers ?? 0} />
            <MiniStat label="Orders" value={stats?.totalOrders ?? 0} />
            <MiniStat label="Active" value={stats?.activeOrders ?? 0} />
          </div>
        </div>
      </section>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Panel title="Exchange profit">
          <div className="space-y-3">
            {Object.entries(stats?.exchangeProfit ?? {}).length > 0 ? (
              Object.entries(stats?.exchangeProfit ?? {}).map(([asset, amount]) => (
                <div
                  key={asset}
                  className="flex items-center justify-between rounded-lg border bg-background/60 px-3 py-2 text-sm"
                >
                  <span className="font-medium">{asset}</span>
                  <span className="font-mono">{formatNumber(amount)}</span>
                </div>
              ))
            ) : (
              <EmptyState title="No fee wallet found" />
            )}
          </div>
        </Panel>

        <Panel title="System health">
          <div className="space-y-3">
            <InfoRow
              label="System fee user"
              value={stats?.systemUserPresent ? 'Present' : 'Missing'}
              accent={stats?.systemUserPresent ? 'emerald' : 'rose'}
            />
            <InfoRow label="Tracked fee assets" value={Object.keys(stats?.exchangeProfit ?? {}).length} />
            <InfoRow label="Fee total" value={formatNumber(totalReserved)} />
          </div>
        </Panel>

        <Panel title="Controls">
          <div className="space-y-3">
            <Input
              label="Search users"
              name="admin-search"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="username or user id"
            />

            <div className="flex flex-wrap gap-2">
              <FilterButton active={filter === 'all'} onClick={() => setFilter('all')}>
                All
              </FilterButton>
              <FilterButton active={filter === 'admins'} onClick={() => setFilter('admins')}>
                Admins
              </FilterButton>
              <FilterButton active={filter === 'users'} onClick={() => setFilter('users')}>
                Users
              </FilterButton>
            </div>

            <Button
              variant="secondary"
              className="w-full"
              onClick={() => {
                queryClient.invalidateQueries({ queryKey: ['admin-users'] });
                queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
              }}
            >
              Refresh data
            </Button>
          </div>
        </Panel>
      </div>

      <Panel title={`Users (${filteredUsers.length})`}>
        {usersQuery.isLoading ? (
          <EmptyState title="Loading admin data…" />
        ) : usersQuery.isError ? (
          <EmptyState title="Failed to load users" className="border-rose-200 bg-rose-50 text-rose-700" />
        ) : filteredUsers.length === 0 ? (
          <EmptyState title="No users match the current search and filter" />
        ) : (
          <div className="grid gap-4 xl:grid-cols-2">
            {filteredUsers.map((user) => (
              <article
                key={user.userId}
                className="rounded-xl border bg-card p-4 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md"
              >
                <div className="flex flex-col gap-4">
                  <div className="flex items-start justify-between gap-4">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <h3 className="text-lg font-bold">{user.username}</h3>
                        <RoleBadge admin={user.admin} />
                      </div>
                      <p className="break-all text-xs text-muted-foreground">{user.userId}</p>
                    </div>

                    <Button
                      variant="secondary"
                      className="px-3 py-1.5 text-xs"
                      onClick={() =>
                        navigator.clipboard.writeText(user.userId).then(() => toast.success('User ID copied'))
                      }
                    >
                      Copy ID
                    </Button>
                  </div>

                  <div className="grid gap-3 md:grid-cols-2">
                    <WalletBlock title="Balances" values={user.balances} />
                    <WalletBlock title="Reserved" values={user.reserved} />
                  </div>

                  <div className="flex items-center justify-between gap-3 border-t pt-3">
                    <div className="text-xs text-muted-foreground">
                      {user.admin ? 'Admin access enabled' : 'Regular user account'}
                    </div>
                    <Button
                      variant={user.admin ? 'danger' : 'primary'}
                      isLoading={toggleAdminMutation.isPending}
                      onClick={() => {
                        const nextAdmin = !user.admin;
                        const confirmed = window.confirm(
                          nextAdmin
                            ? `Grant admin role to ${user.username}?`
                            : `Revoke admin role from ${user.username}?`
                        );
                        if (!confirmed) return;
                        toggleAdminMutation.mutate({ userId: user.userId, admin: nextAdmin });
                      }}
                    >
                      {user.admin ? 'Revoke admin' : 'Make admin'}
                    </Button>
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}
      </Panel>
    </div>
  );
};

const MiniStat: React.FC<{ label: string; value: number }> = ({ label, value }) => (
  <div className="rounded-xl border border-white/10 bg-white/5 p-4 backdrop-blur">
    <div className="text-[11px] uppercase tracking-[0.3em] text-slate-300">{label}</div>
    <div className="mt-2 text-2xl font-black">{value}</div>
  </div>
);

const FilterButton: React.FC<React.PropsWithChildren<{ active: boolean; onClick: () => void }>> = ({
  active,
  onClick,
  children,
}) => (
  <button
    type="button"
    onClick={onClick}
    className={`rounded-full px-3 py-1.5 text-xs font-semibold transition ${
      active ? 'bg-primary text-primary-foreground' : 'border bg-background hover:bg-accent'
    }`}
  >
    {children}
  </button>
);

const RoleBadge: React.FC<{ admin: boolean }> = ({ admin }) => (
  <span
    className={`rounded-full px-2 py-1 text-[11px] font-bold uppercase tracking-wide ${
      admin ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-600'
    }`}
  >
    {admin ? 'Admin' : 'User'}
  </span>
);

const WalletBlock: React.FC<{ title: string; values: Record<string, number> }> = ({ title, values }) => {
  const entries = Object.entries(values);

  return (
    <div className="rounded-xl border bg-background/60 p-3">
      <div className="mb-3 text-xs font-semibold uppercase tracking-wide text-muted-foreground">{title}</div>
      {entries.length === 0 ? (
        <div className="text-sm text-muted-foreground">-</div>
      ) : (
        <div className="space-y-2">
          {entries.map(([asset, amount]) => (
            <div key={asset} className="flex items-center justify-between gap-3 text-sm">
              <span className="font-medium">{asset}</span>
              <span className="font-mono text-muted-foreground">{formatNumber(amount)}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

const InfoRow: React.FC<{ label: string; value: React.ReactNode; accent?: 'emerald' | 'rose' }> = ({
  label,
  value,
  accent,
}) => (
  <div className="flex items-center justify-between rounded-lg border bg-background/60 px-3 py-2 text-sm">
    <span className="text-muted-foreground">{label}</span>
    <span
      className={
        accent === 'emerald'
          ? 'font-semibold text-emerald-600'
          : accent === 'rose'
            ? 'font-semibold text-rose-600'
            : 'font-medium'
      }
    >
      {value}
    </span>
  </div>
);

const formatNumber = (value: number) =>
  new Intl.NumberFormat('en-US', {
    maximumFractionDigits: 8,
  }).format(value);
