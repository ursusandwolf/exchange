import { useState } from 'react';
import { EmptyState } from '../shared/ui';
import { useStompTopic } from '../shared/ws/useStompTopic';
import type { TradeUpdate } from '../shared/api/types';

export function RecentTrades({ symbol }: { symbol: string }) {
  const [trades, setTrades] = useState<TradeUpdate[]>([]);

  useStompTopic<TradeUpdate>(`/topic/trades/${symbol}`, (trade) => {
    setTrades((prev) => [trade, ...prev].slice(0, 50));
  });

  if (trades.length === 0) {
    return (
      <EmptyState
        title="No recent trades"
        description={`Trades for ${symbol} will appear here in real time.`}
      />
    );
  }

  return (
    <div className="flex h-full flex-col overflow-hidden text-xs">
      <div className="mb-2 grid grid-cols-3 border-b pb-2 text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
        <span>Price</span>
        <span className="text-right">Qty</span>
        <span className="text-right">Time</span>
      </div>

      <div className="flex-1 space-y-1 overflow-y-auto">
        {trades.map((trade) => (
          <div key={trade.id} className="grid grid-cols-3">
            <span className="font-mono font-medium">{trade.price.toFixed(2)}</span>
            <span className="text-right font-mono">{trade.quantity.toFixed(4)}</span>
            <span className="text-right text-muted-foreground">
              {new Date(trade.timestamp).toLocaleTimeString([], {
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
              })}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
