import { useState } from 'react';
import { EmptyState } from '../shared/ui';
import { useStompTopic } from '../shared/ws/useStompTopic';
import type { OrderBookUpdate } from '../shared/api/types';

export function OrderBook({ symbol }: { symbol: string }) {
  const [data, setData] = useState<OrderBookUpdate | null>(null);
  const [status, setStatus] = useState<'loading' | 'live' | 'empty'>('loading');

  useStompTopic<OrderBookUpdate>(`/topic/orderbook/${symbol}`, (update) => {
    setData(update);
    const empty = update.bids.length === 0 && update.asks.length === 0;
    setStatus(empty ? 'empty' : 'live');
  });

  if (status === 'loading' && !data) {
    return <EmptyState title="Connecting to order book…" description="Waiting for the first snapshot." />;
  }

  if (!data || status === 'empty') {
    return (
      <EmptyState
        title="Order book is empty"
        description={`No open bids or asks for ${symbol} yet.`}
      />
    );
  }

  const bestAsk = data.asks[0]?.price;
  const bestBid = data.bids[0]?.price;
  const spread =
    bestAsk != null && bestBid != null ? (bestAsk - bestBid).toFixed(2) : '—';

  return (
    <div className="flex h-full flex-col text-sm">
      <div className="mb-2 grid grid-cols-2 border-b pb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
        <span>Price</span>
        <span className="text-right">Quantity</span>
      </div>

      <div className="flex-1 space-y-1 overflow-y-auto">
        <div className="flex flex-col-reverse">
          {data.asks.map((ask) => (
            <div key={`ask-${ask.price}`} className="grid grid-cols-2 text-destructive">
              <span className="font-mono">{ask.price.toFixed(2)}</span>
              <span className="text-right font-mono">{ask.quantity.toFixed(4)}</span>
            </div>
          ))}
        </div>

        <div className="my-2 border-y py-2 text-center text-xs font-semibold">
          Spread: <span className="font-mono">{spread}</span>
        </div>

        <div>
          {data.bids.map((bid) => (
            <div key={`bid-${bid.price}`} className="grid grid-cols-2 text-emerald-700 dark:text-emerald-400">
              <span className="font-mono">{bid.price.toFixed(2)}</span>
              <span className="text-right font-mono">{bid.quantity.toFixed(4)}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
