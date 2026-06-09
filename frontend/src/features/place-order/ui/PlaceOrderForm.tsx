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
  takeProfitPrice: z.preprocess((val) => val === '' ? null : parseFloat(val as string), z.number().positive('Цена take-profit должна быть больше 0').nullable()).optional(),
  stopLossTriggerPrice: z.preprocess((val) => val === '' ? null : parseFloat(val as string), z.number().positive('Цена stop-loss триггера должна быть больше 0').nullable()).optional(),
  stopLossPrice: z.preprocess((val) => val === '' ? null : parseFloat(val as string), z.number().positive('Цена stop-loss должна быть больше 0').nullable()).optional(),
});

type OrderFormData = z.output<typeof orderSchema>;

interface StandardOrderRequest {
  baseAsset: string;
  quoteAsset: string;
  side: 'BUY' | 'SELL';
  type: 'LIMIT' | 'MARKET' | 'STOP_LOSS' | 'TAKE_PROFIT';
  quantity: number;
  price?: number | null;
  triggerPrice?: number | null;
}

interface OcoOrderRequest {
  baseAsset: string;
  quoteAsset: string;
  side: 'SELL';
  quantity: number;
  takeProfitPrice: number;
  stopLossTriggerPrice: number;
  stopLossPrice?: number | null;
}

type MutationPayload =
  | { endpoint: '/orders/submit'; body: StandardOrderRequest }
  | { endpoint: '/orders/oco'; body: OcoOrderRequest };

export const PlaceOrderForm: React.FC<{ symbol: string }> = ({ symbol }) => {
  const [side, setSide] = useState<'BUY' | 'SELL'>('BUY');
  const [type, setType] = useState<'LIMIT' | 'MARKET' | 'STOP_LOSS' | 'TAKE_PROFIT'>('LIMIT');
  const [mode, setMode] = useState<'STANDARD' | 'OCO'>('STANDARD');
  const queryClient = useQueryClient();

  const [baseAsset, quoteAsset] = symbol.split('/');

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<OrderFormData>({
    resolver: zodResolver(orderSchema) as any,
    defaultValues: {
      quantity: 0,
      price: null,
      triggerPrice: null,
      takeProfitPrice: null,
      stopLossTriggerPrice: null,
      stopLossPrice: null,
    },
  });

  const mutation = useMutation({
    mutationFn: (payload: MutationPayload) => api.post(payload.endpoint, payload.body),
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
    if (mode === 'OCO') {
      if (data.takeProfitPrice == null || data.stopLossTriggerPrice == null) {
        toast.error('Для OCO нужны take-profit и stop-loss trigger цены');
        return;
      }

      mutation.mutate({
        endpoint: '/orders/oco',
        body: {
          baseAsset,
          quoteAsset,
          side: 'SELL',
          quantity: data.quantity,
          takeProfitPrice: data.takeProfitPrice,
          stopLossTriggerPrice: data.stopLossTriggerPrice,
          stopLossPrice: data.stopLossPrice ?? null,
        },
      });
      return;
    }

    mutation.mutate({
      endpoint: '/orders/submit',
      body: {
        ...data,
        baseAsset,
        quoteAsset,
        side,
        type,
        price: type === 'MARKET' ? null : data.price,
        triggerPrice: (type === 'STOP_LOSS' || type === 'TAKE_PROFIT') ? data.triggerPrice : null,
      } as StandardOrderRequest,
    });
  };

  const handleModeChange = (nextMode: 'STANDARD' | 'OCO') => {
    setMode(nextMode);
    if (nextMode === 'OCO') {
      setSide('SELL');
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex gap-1 overflow-x-auto pb-1">
        <button
          onClick={() => handleModeChange('STANDARD')}
          className={`px-4 py-1 rounded text-xs font-bold transition-colors ${mode === 'STANDARD' ? 'bg-primary text-primary-foreground' : 'bg-muted'}`}
        >
          STANDARD
        </button>
        <button
          onClick={() => handleModeChange('OCO')}
          className={`px-4 py-1 rounded text-xs font-bold transition-colors ${mode === 'OCO' ? 'bg-primary text-primary-foreground' : 'bg-muted'}`}
        >
          OCO
        </button>
      </div>

      <div className="flex gap-1 overflow-x-auto pb-1">
        <button
          onClick={() => setSide('BUY')}
          disabled={mode === 'OCO'}
          className={`px-4 py-1 rounded text-xs font-bold transition-colors ${side === 'BUY' ? 'bg-primary text-primary-foreground' : 'bg-muted'} ${mode === 'OCO' ? 'opacity-50 cursor-not-allowed' : ''}`}
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

      {mode === 'STANDARD' ? (
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
      ) : (
        <p className="text-[10px] text-muted-foreground">
          OCO создаёт пару SELL-ордеров: take-profit и stop-loss. Когда один срабатывает, второй отменяется автоматически.
        </p>
      )}

      <form onSubmit={handleSubmit(onSubmit as any)} className="space-y-3">
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

        {mode === 'STANDARD' && (type === 'STOP_LOSS' || type === 'TAKE_PROFIT') && (
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

        {mode === 'OCO' && (
          <>
            <div className="space-y-1">
              <label className="text-[10px] text-muted-foreground uppercase font-bold">Take Profit Price ({quoteAsset})</label>
              <input
                {...register('takeProfitPrice')}
                type="number"
                step="0.01"
                className={`w-full p-2 border rounded bg-background text-sm ${errors.takeProfitPrice ? 'border-destructive' : ''}`}
                placeholder="0.00"
              />
              {errors.takeProfitPrice && <p className="text-[10px] text-destructive">{errors.takeProfitPrice.message}</p>}
            </div>

            <div className="space-y-1">
              <label className="text-[10px] text-muted-foreground uppercase font-bold">Stop Loss Trigger Price ({quoteAsset})</label>
              <input
                {...register('stopLossTriggerPrice')}
                type="number"
                step="0.01"
                className={`w-full p-2 border rounded bg-background text-sm ${errors.stopLossTriggerPrice ? 'border-destructive' : ''}`}
                placeholder="0.00"
              />
              {errors.stopLossTriggerPrice && <p className="text-[10px] text-destructive">{errors.stopLossTriggerPrice.message}</p>}
            </div>

            <div className="space-y-1">
              <label className="text-[10px] text-muted-foreground uppercase font-bold">Stop Loss Price (Optional, {quoteAsset})</label>
              <input
                {...register('stopLossPrice')}
                type="number"
                step="0.01"
                className={`w-full p-2 border rounded bg-background text-sm ${errors.stopLossPrice ? 'border-destructive' : ''}`}
                placeholder="0.00"
              />
              {errors.stopLossPrice && <p className="text-[10px] text-destructive">{errors.stopLossPrice.message}</p>}
            </div>
          </>
        )}

        {mode === 'STANDARD' && type !== 'MARKET' && (
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
          {mutation.isPending ? 'Sending...' : mode === 'OCO' ? `Place OCO ${baseAsset}` : `${side} ${baseAsset}`}
        </button>
      </form>
    </div>
  );
};
