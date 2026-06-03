import React, { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { toast } from 'sonner';
import { api } from '../../../shared/api';

const orderSchema = z.object({
  quantity: z.preprocess((val) => parseFloat(val as string), z.number().positive('Количество должно быть больше 0')),
  price: z.preprocess((val) => val === '' ? null : parseFloat(val as string), z.number().positive('Цена должна быть больше 0').nullable()).optional(),
  triggerPrice: z.preprocess((val) => val === '' ? null : parseFloat(val as string), z.number().positive('Цена триггера должна быть больше 0').nullable()).optional(),
});

type OrderFormData = z.infer<typeof orderSchema>;

interface OrderRequest {
  baseAsset: string;
  quoteAsset: string;
  side: 'BUY' | 'SELL';
  type: 'LIMIT' | 'MARKET' | 'STOP_LOSS' | 'TAKE_PROFIT';
  quantity: number;
  price?: number | null;
  triggerPrice?: number | null;
}

export const PlaceOrderForm: React.FC<{ symbol: string }> = ({ symbol }) => {
  const [side, setSide] = useState<'BUY' | 'SELL'>('BUY');
  const [type, setType] = useState<'LIMIT' | 'MARKET' | 'STOP_LOSS' | 'TAKE_PROFIT'>('LIMIT');
  const queryClient = useQueryClient();

  const [baseAsset, quoteAsset] = symbol.split('/');

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<OrderFormData>({
    resolver: zodResolver(orderSchema),
    defaultValues: {
      quantity: 0,
      price: null,
      triggerPrice: null,
    },
  });

  const mutation = useMutation({
    mutationFn: (newOrder: OrderRequest) => api.post('/orders/submit', newOrder),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['portfolio'] });
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      reset();
      toast.success('Ордер успешно размещен');
    },
    onError: (error: AxiosError<any>) => {
      const message = error.response?.data?.message || error.response?.data || 'Ошибка при размещении ордера';
      toast.error(message);
    }
  });

  const onSubmit = (data: OrderFormData) => {
    mutation.mutate({
      ...data,
      baseAsset,
      quoteAsset,
      side,
      type,
      price: type === 'MARKET' ? null : data.price,
      triggerPrice: (type === 'STOP_LOSS' || type === 'TAKE_PROFIT') ? data.triggerPrice : null,
    } as OrderRequest);
  };

  return (
    <div className="space-y-4">
      <div className="flex gap-1 overflow-x-auto pb-1">
        <button
          onClick={() => setSide('BUY')}
          className={`px-4 py-1 rounded text-xs font-bold transition-colors ${side === 'BUY' ? 'bg-primary text-primary-foreground' : 'bg-muted'}`}
        >
          BUY
        </button>
        <button
          onClick={() => setSide('SELL')}
          className={`px-4 py-1 rounded text-xs font-bold transition-colors ${side === 'SELL' ? 'bg-destructive text-destructive-foreground' : 'bg-muted'}`}
        >
          SELL
        </button>
      </div>

      <div className="flex gap-1 text-[10px]">
        {['LIMIT', 'MARKET', 'STOP_LOSS', 'TAKE_PROFIT'].map((t) => (
          <button
            key={t}
            onClick={() => setType(t as any)}
            className={`px-2 py-1 rounded transition-colors ${type === t ? 'bg-accent' : 'hover:bg-accent/50'}`}
          >
            {t.replace('_', ' ')}
          </button>
        ))}
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-3">
        <div className="space-y-1">
          <label className="text-[10px] text-muted-foreground uppercase font-bold">Quantity ({baseAsset})</label>
          <input
            {...register('quantity')}
            type="number"
            step="0.0001"
            className={`w-full p-2 border rounded bg-background text-sm ${errors.quantity ? 'border-destructive' : ''}`}
            placeholder="0.00"
          />
          {errors.quantity && <p className="text-[10px] text-destructive">{errors.quantity.message}</p>}
        </div>

        {(type === 'STOP_LOSS' || type === 'TAKE_PROFIT') && (
          <div className="space-y-1">
            <label className="text-[10px] text-muted-foreground uppercase font-bold">Trigger Price ({quoteAsset})</label>
            <input
              {...register('triggerPrice')}
              type="number"
              step="0.01"
              className={`w-full p-2 border rounded bg-background text-sm ${errors.triggerPrice ? 'border-destructive' : ''}`}
              placeholder="0.00"
            />
            {errors.triggerPrice && <p className="text-[10px] text-destructive">{errors.triggerPrice.message}</p>}
          </div>
        )}

        {type !== 'MARKET' && (
          <div className="space-y-1">
            <label className="text-[10px] text-muted-foreground uppercase font-bold">
              { (type === 'STOP_LOSS' || type === 'TAKE_PROFIT') ? 'Limit Price (Optional)' : `Price (${quoteAsset})` }
            </label>
            <input
              {...register('price')}
              type="number"
              step="0.01"
              className={`w-full p-2 border rounded bg-background text-sm ${errors.price ? 'border-destructive' : ''}`}
              placeholder="0.00"
            />
            {errors.price && <p className="text-[10px] text-destructive">{errors.price.message}</p>}
          </div>
        )}

        <button
          type="submit"
          disabled={mutation.isPending}
          className={`w-full py-2 rounded font-bold transition-all text-sm ${side === 'BUY' ? 'bg-primary text-primary-foreground' : 'bg-destructive text-destructive-foreground'} ${mutation.isPending ? 'opacity-50 cursor-not-allowed' : 'hover:opacity-90 active:scale-[0.98]'}`}
        >
          {mutation.isPending ? 'Sending...' : `${side} ${baseAsset}`}
        </button>
      </form>
    </div>
  );
};
