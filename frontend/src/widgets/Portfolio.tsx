import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuthStore } from '../entities/user';
import { api } from '../shared/api';
import type { PortfolioResponse, TransactionRecordResponse } from '../shared/api/types';
import { EmptyState } from '../shared/ui';
import { cn } from '../shared/lib/cn';

export function Portfolio() {
  const [activeTab, setActiveTab] = useState<'assets' | 'history'>('assets');
  const token = useAuthStore((state) => state.token);
  const userId = useAuthStore((state) => state.userId);

  const { data: portfolio, isLoading: isPortfolioLoading, isError: isPortfolioError } = useQuery({
    queryKey: ['portfolio', userId],
    queryFn: async () => (await api.get<PortfolioResponse>('/user/portfolio')).data,
    enabled: Boolean(token && userId),
    refetchInterval: 5000,
  });

  const { data: history, isLoading: isHistoryLoading, isError: isHistoryError } = useQuery({
    queryKey: ['wallet-history', userId],
    queryFn: async () => (await api.get<TransactionRecordResponse[]>('/user/history')).data,
    enabled: Boolean(token && userId && activeTab === 'history'),
  });

  if (!token) {
    return (
      <EmptyState
        title="Sign in to view portfolio"
        description="Balances and ledger appear after authentication."
        action={
          <Link
            to="/login"
            className="mt-1 inline-flex rounded-md bg-primary px-3 py-2 text-sm font-medium text-primary-foreground"
          >
            Login
          </Link>
        }
      />
    );
  }

  return (
    <div className="flex h-full flex-col">
      <div className="mb-4 flex border-b" role="tablist" aria-label="Portfolio sections">
        {(['assets', 'history'] as const).map((tab) => (
          <button
            key={tab}
            type="button"
            role="tab"
            aria-selected={activeTab === tab}
            onClick={() => setActiveTab(tab)}
            className={cn(
              'flex-1 border-b-2 py-2 text-sm font-medium capitalize transition-colors',
              activeTab === tab
                ? 'border-primary text-primary'
                : 'border-transparent text-muted-foreground hover:text-foreground'
            )}
          >
            {tab}
          </button>
        ))}
      </div>

      <div className="flex-1 overflow-y-auto">
        {activeTab === 'assets' ? (
          <AssetsView data={portfolio} isLoading={isPortfolioLoading} isError={isPortfolioError} />
        ) : (
          <HistoryView data={history} isLoading={isHistoryLoading} isError={isHistoryError} />
        )}
      </div>
    </div>
  );
}

function AssetsView({
  data,
  isLoading,
  isError,
}: {
  data?: PortfolioResponse;
  isLoading: boolean;
  isError: boolean;
}) {
  if (isLoading) return <p className="p-2 text-center text-sm text-muted-foreground">Loading assets…</p>;
  if (isError) return <EmptyState title="Could not load assets" description="Try again in a moment." />;

  const balances = data?.balances ?? {};
  const reserved = data?.reserved ?? {};
  const assets = Array.from(new Set([...Object.keys(balances), ...Object.keys(reserved)])).sort((a, b) =>
    a.localeCompare(b)
  );

  if (assets.length === 0) {
    return <EmptyState title="No assets" description="Funded balances will show up here." />;
  }

  return (
    <div className="space-y-2 text-sm">
      <div className="grid grid-cols-3 border-b pb-2 text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
        <span>Asset</span>
        <span className="text-right">Available</span>
        <span className="text-right">Reserved</span>
      </div>
      <div className="divide-y">
        {assets.map((asset) => (
          <div key={asset} className="grid grid-cols-3 rounded px-1 py-2 hover:bg-muted/50">
            <span className="font-semibold">{asset}</span>
            <span className="text-right font-mono">
              {(balances[asset] || 0).toLocaleString(undefined, {
                minimumFractionDigits: 2,
                maximumFractionDigits: 4,
              })}
            </span>
            <span className="text-right font-mono text-muted-foreground">
              {(reserved[asset] || 0).toLocaleString(undefined, {
                minimumFractionDigits: 2,
                maximumFractionDigits: 4,
              })}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

function HistoryView({
  data,
  isLoading,
  isError,
}: {
  data?: TransactionRecordResponse[];
  isLoading: boolean;
  isError: boolean;
}) {
  if (isLoading) return <p className="p-2 text-center text-sm text-muted-foreground">Loading history…</p>;
  if (isError) return <EmptyState title="Could not load history" />;
  if (!data || data.length === 0) {
    return <EmptyState title="No transactions" description="Wallet movements will be listed here." />;
  }

  return (
    <div className="space-y-2 text-[11px]">
      <div className="grid grid-cols-4 border-b pb-2 text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
        <span>Type</span>
        <span>Asset</span>
        <span className="text-right">Amount</span>
        <span className="text-right">Time</span>
      </div>
      <div className="divide-y">
        {data.map((record) => (
          <div key={record.id} className="grid grid-cols-4 rounded px-1 py-2 hover:bg-muted/50" title={record.description}>
            <span
              className={cn(
                'font-medium',
                record.amount > 0 ? 'text-emerald-700' : record.amount < 0 ? 'text-destructive' : ''
              )}
            >
              {record.type}
            </span>
            <span>{record.asset}</span>
            <span
              className={cn(
                'text-right font-mono',
                record.amount > 0 ? 'text-emerald-700' : record.amount < 0 ? 'text-destructive' : ''
              )}
            >
              {record.amount > 0 ? '+' : ''}
              {record.amount.toFixed(4)}
            </span>
            <span className="text-right text-muted-foreground">
              {record.timestamp
                ? new Date(record.timestamp).toLocaleTimeString([], {
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit',
                  })
                : '—'}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
