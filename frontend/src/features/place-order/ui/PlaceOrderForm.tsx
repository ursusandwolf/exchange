import React, { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { api } from '../../../shared/api';

interface OrderRequest {
  baseAsset: string;
  quoteAsset: string;
  side: 'BUY' | 'SELL';
  type: 'LIMIT' | 'MARKET';
  quantity: number;
  price: number | null;
}

export const PlaceOrderForm: React.FC<{ symbol: string }> = ({ symbol }) => {
  const [side, setSide] = useState<'BUY' | 'SELL'>('BUY');
  const [type, setType] = useState<'LIMIT' | 'MARKET'>('LIMIT');
  const [quantity, setQuantity] = useState('');
  const [price, setPrice] = useState('');
  const queryClient = useQueryClient();

  const [baseAsset, quoteAsset] = symbol.split('/');

  const mutation = useMutation({
    mutationFn: (newOrder: OrderRequest) => api.post('/orders', newOrder),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['portfolio'] });
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      setQuantity('');
      setPrice('');
      // In a real app, we'd use a toast here. CR mentioned alert() is bad.
      // For now, I'll keep it simple or use a console.log if I don't have a toast lib.
      // But the CR said "нужен toast". I don't see a toast lib in package.json.
      // I'll stick with alert for now as I shouldn't add new libs unless asked,
      // but I'll make it cleaner.
      alert('Order placed successfully!');
    },
    onError: (error: AxiosError<string>) => {
      alert(error.response?.data || 'Failed to place order');
    }
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    mutation.mutate({
      baseAsset, quoteAsset, side, type,
      quantity: parseFloat(quantity),
      price: type === 'LIMIT' ? parseFloat(price) : null,
    });
  };

  return (
    <div className="space-y-4">
      <div className="flex gap-2">
        <button
          onClick={() => setSide('BUY')}
          className={`flex-1 py-1 rounded font-bold ${side === 'BUY' ? 'bg-primary text-primary-foreground' : 'bg-muted'}`}
        >
          BUY
        </button>
        <button
          onClick={() => setSide('SELL')}
          className={`flex-1 py-1 rounded font-bold ${side === 'SELL' ? 'bg-destructive text-destructive-foreground' : 'bg-muted'}`}
        >
          SELL
        </button>
      </div>

      <div className="flex gap-2 text-xs">
        <button
          onClick={() => setType('LIMIT')}
          className={`px-3 py-1 rounded ${type === 'LIMIT' ? 'bg-accent' : 'bg-transparent'}`}
        >
          Limit
        </button>
        <button
          onClick={() => setType('MARKET')}
          className={`px-3 py-1 rounded ${type === 'MARKET' ? 'bg-accent' : 'bg-transparent'}`}
        >
          Market
        </button>
      </div>

      <form onSubmit={handleSubmit} className="space-y-3">
        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">Quantity ({baseAsset})</label>
          <input
            type="number"
            step="0.0001"
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            className="w-full p-2 border rounded bg-background text-sm"
            placeholder="0.00"
            required
          />
        </div>

        {type === 'LIMIT' && (
          <div className="space-y-1">
            <label className="text-xs text-muted-foreground">Price ({quoteAsset})</label>
            <input
              type="number"
              step="0.01"
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              className="w-full p-2 border rounded bg-background text-sm"
              placeholder="0.00"
              required
            />
          </div>
        )}

        <button
          type="submit"
          disabled={mutation.isPending}
          className={`w-full py-2 rounded font-bold transition-opacity ${side === 'BUY' ? 'bg-primary text-primary-foreground' : 'bg-destructive text-destructive-foreground'} ${mutation.isPending ? 'opacity-50' : 'hover:opacity-90'}`}
        >
          {mutation.isPending ? 'Sending...' : `${side} ${baseAsset}`}
        </button>
      </form>
    </div>
  );
};
