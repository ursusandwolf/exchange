import React from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../shared/api/base';

export const Portfolio: React.FC = () => {
  const { data, isLoading } = useQuery({
    queryKey: ['portfolio'],
    queryFn: () => api.get('/user/portfolio').then(res => res.data),
  });

  if (isLoading) return <div className="text-sm italic">Loading assets...</div>;

  const balances = data?.balances || {};
  const reserved = data?.reserved || {};
  const assets = Array.from(new Set([...Object.keys(balances), ...Object.keys(reserved)]));

  return (
    <div className="space-y-4 text-sm">
      <div className="grid grid-cols-3 font-bold border-b pb-2">
        <span>Asset</span>
        <span className="text-right">Available</span>
        <span className="text-right">Reserved</span>
      </div>
      <div className="space-y-2">
        {assets.length === 0 && <div className="text-center text-muted-foreground py-4">No assets found</div>}
        {assets.map(asset => (
          <div key={asset} className="grid grid-cols-3">
            <span className="font-medium">{asset}</span>
            <span className="text-right">{(balances[asset] || 0).toFixed(4)}</span>
            <span className="text-right text-muted-foreground">{(reserved[asset] || 0).toFixed(4)}</span>
          </div>
        ))}
      </div>
    </div>
  );
};
