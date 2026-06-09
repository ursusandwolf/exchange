import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAuthStore } from '../entities/user';
import { api } from '../shared/api';

export const Portfolio: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'assets' | 'history'>('assets');
  const token = useAuthStore(state => state.token);
  const userId = useAuthStore(state => state.userId);

  const { data: portfolio, isLoading: isPortfolioLoading } = useQuery({
    queryKey: ['portfolio', userId],
    queryFn: () => api.get('/user/portfolio').then(res => res.data),
    enabled: !!token && !!userId,
    refetchInterval: 5000,
  });

  const { data: history, isLoading: isHistoryLoading } = useQuery({
    queryKey: ['wallet-history', userId],
    queryFn: () => api.get('/user/history').then(res => res.data),
    enabled: !!token && !!userId && activeTab === 'history',
  });

  if (!token) {
    return (
      <div className="flex flex-col items-center justify-center h-[150px] text-muted-foreground italic text-sm text-center px-4">
        Connect your wallet to see your assets and transaction history
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      <div className="flex border-b mb-4">
        <button
          onClick={() => setActiveTab('assets')}
          className={`flex-1 py-1 text-sm font-medium border-b-2 transition-colors ${activeTab === 'assets' ? 'border-primary text-primary' : 'border-transparent text-muted-foreground hover:text-foreground'}`}
        >
          Assets
        </button>
        <button
          onClick={() => setActiveTab('history')}
          className={`flex-1 py-1 text-sm font-medium border-b-2 transition-colors ${activeTab === 'history' ? 'border-primary text-primary' : 'border-transparent text-muted-foreground hover:text-foreground'}`}
        >
          History
        </button>
      </div>

      <div className="flex-1 overflow-y-auto">
        {activeTab === 'assets' ? (
          <AssetsView data={portfolio} isLoading={isPortfolioLoading} />
        ) : (
          <HistoryView data={history} isLoading={isHistoryLoading} />
        )}
      </div>
    </div>
  );
};

const AssetsView: React.FC<{ data: any; isLoading: boolean }> = ({ data, isLoading }) => {
  if (isLoading) return <div className="text-sm italic p-2 text-center">Loading assets...</div>;

  const balances = data?.balances || {};
  const reserved = data?.reserved || {};
  const assets = Array.from(new Set([...Object.keys(balances), ...Object.keys(reserved)]))
    .sort((a, b) => a.localeCompare(b));

  return (
    <div className="space-y-2 text-sm">
      <div className="grid grid-cols-3 font-bold border-b pb-2 text-xs uppercase text-muted-foreground">
        <span>Asset</span>
        <span className="text-right">Available</span>
        <span className="text-right">Reserved</span>
      </div>
      <div className="divide-y">
        {assets.length === 0 && <div className="text-center text-muted-foreground py-4 italic">No assets found</div>}
        {assets.map(asset => (
          <div key={asset} className="grid grid-cols-3 py-2 hover:bg-muted/50 transition-colors px-1 rounded">
            <span className="font-bold">{asset}</span>
            <span className="text-right">{(balances[asset] || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 4 })}</span>
            <span className="text-right text-muted-foreground">{(reserved[asset] || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 4 })}</span>
          </div>
        ))}
      </div>
    </div>
  );
};

const HistoryView: React.FC<{ data: any[]; isLoading: boolean }> = ({ data, isLoading }) => {
  if (isLoading) return <div className="text-sm italic p-2 text-center">Loading history...</div>;

  return (
    <div className="space-y-2 text-[11px]">
      <div className="grid grid-cols-4 font-bold border-b pb-2 text-[10px] uppercase text-muted-foreground">
        <span>Type</span>
        <span>Asset</span>
        <span className="text-right">Amount</span>
        <span className="text-right px-1">Date</span>
      </div>
      <div className="divide-y">
        {(!data || data.length === 0) && <div className="text-center text-muted-foreground py-4 italic">No transactions found</div>}
        {data?.map((record: any) => (
          <div key={record.id} className="grid grid-cols-4 py-2 hover:bg-muted/50 px-1 rounded" title={record.description}>
            <span className={`font-medium ${record.amount > 0 ? 'text-primary' : record.amount < 0 ? 'text-destructive' : ''}`}>
              {record.type}
            </span>
            <span>{record.asset}</span>
            <span className={`text-right font-mono ${record.amount > 0 ? 'text-primary' : record.amount < 0 ? 'text-destructive' : ''}`}>
              {record.amount > 0 ? '+' : ''}{record.amount.toFixed(4)}
            </span>
            <span className="text-right text-muted-foreground px-1">
               {record.timestamp ? new Date(record.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : 'N/A'}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};
