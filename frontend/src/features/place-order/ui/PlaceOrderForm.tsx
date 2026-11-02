import { useState } from 'react';
import { useForm, type Resolver } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button, Input } from '../../../shared/ui';
import { cn } from '../../../shared/lib/cn';
import {
  usePlaceOrder,
  type OrderMode,
  type OrderSide,
  type OrderType,
  type PlaceOrderValues,
} from '../model/usePlaceOrder';

const optionalPositive = z.preprocess(
  (val) => (val === '' || val === null || val === undefined ? null : Number(val)),
  z.number().positive('Must be greater than 0').nullable().optional()
);

const orderSchema = z.object({
  quantity: z.preprocess(
    (val) => Number(val),
    z.number().positive('Quantity must be greater than 0')
  ),
  price: optionalPositive,
  triggerPrice: optionalPositive,
  takeProfitPrice: optionalPositive,
  stopLossTriggerPrice: optionalPositive,
  stopLossPrice: optionalPositive,
});

type OrderFormData = z.output<typeof orderSchema>;

const ORDER_TYPES: OrderType[] = ['LIMIT', 'MARKET', 'STOP_LOSS', 'TAKE_PROFIT'];

export function PlaceOrderForm({ symbol }: { symbol: string }) {
  const [side, setSide] = useState<OrderSide>('BUY');
  const [type, setType] = useState<OrderType>('LIMIT');
  const [mode, setMode] = useState<OrderMode>('STANDARD');
  const { baseAsset, quoteAsset, isPending, submitStandard, submitOco } = usePlaceOrder(symbol);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<OrderFormData>({
    resolver: zodResolver(orderSchema) as Resolver<OrderFormData>,
    defaultValues: {
      quantity: undefined,
      price: null,
      triggerPrice: null,
      takeProfitPrice: null,
      stopLossTriggerPrice: null,
      stopLossPrice: null,
    },
  });

  const onSubmit = async (data: OrderFormData) => {
    const values = data as PlaceOrderValues;
    try {
      if (mode === 'OCO') {
        const ok = await submitOco(values);
        if (ok) reset();
        return;
      }
      await submitStandard(side, type, values);
      reset();
    } catch {
      // toast handled in hook
    }
  };

  const handleModeChange = (nextMode: OrderMode) => {
    setMode(nextMode);
    if (nextMode === 'OCO') {
      setSide('SELL');
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex gap-1" role="tablist" aria-label="Order mode">
        {(['STANDARD', 'OCO'] as const).map((item) => (
          <button
            key={item}
            type="button"
            role="tab"
            aria-selected={mode === item}
            onClick={() => handleModeChange(item)}
            className={cn(
              'rounded-md px-3 py-1.5 text-xs font-semibold transition-colors',
              mode === item ? 'bg-primary text-primary-foreground' : 'bg-muted hover:bg-accent'
            )}
          >
            {item}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-2 gap-1" role="group" aria-label="Order side">
        <button
          type="button"
          onClick={() => setSide('BUY')}
          disabled={mode === 'OCO'}
          className={cn(
            'rounded-md py-2 text-xs font-bold transition-colors',
            side === 'BUY' ? 'bg-emerald-600 text-white' : 'bg-muted',
            mode === 'OCO' && 'cursor-not-allowed opacity-50'
          )}
        >
          BUY
        </button>
        <button
          type="button"
          onClick={() => setSide('SELL')}
          className={cn(
            'rounded-md py-2 text-xs font-bold transition-colors',
            side === 'SELL' ? 'bg-destructive text-destructive-foreground' : 'bg-muted'
          )}
        >
          SELL
        </button>
      </div>

      {mode === 'STANDARD' ? (
        <div className="flex flex-wrap gap-1" role="group" aria-label="Order type">
          {ORDER_TYPES.map((item) => (
            <button
              key={item}
              type="button"
              onClick={() => setType(item)}
              className={cn(
                'rounded px-2 py-1 text-[10px] font-medium uppercase tracking-wide',
                type === item ? 'bg-accent' : 'hover:bg-accent/50'
              )}
            >
              {item.replaceAll('_', ' ')}
            </button>
          ))}
        </div>
      ) : (
        <p className="text-xs text-muted-foreground">
          OCO places linked SELL take-profit and stop-loss orders. When one triggers, the other cancels.
        </p>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-3">
        <Input
          label={`Quantity (${baseAsset})`}
          type="number"
          step="0.0001"
          placeholder="0.00"
          error={errors.quantity?.message}
          {...register('quantity')}
        />

        {mode === 'STANDARD' && (type === 'STOP_LOSS' || type === 'TAKE_PROFIT') && (
          <Input
            label={`Trigger price (${quoteAsset})`}
            type="number"
            step="0.01"
            placeholder="0.00"
            error={errors.triggerPrice?.message}
            {...register('triggerPrice')}
          />
        )}

        {mode === 'OCO' && (
          <>
            <Input
              label={`Take-profit (${quoteAsset})`}
              type="number"
              step="0.01"
              placeholder="0.00"
              error={errors.takeProfitPrice?.message}
              {...register('takeProfitPrice')}
            />
            <Input
              label={`Stop-loss trigger (${quoteAsset})`}
              type="number"
              step="0.01"
              placeholder="0.00"
              error={errors.stopLossTriggerPrice?.message}
              {...register('stopLossTriggerPrice')}
            />
            <Input
              label={`Stop-loss limit (optional, ${quoteAsset})`}
              type="number"
              step="0.01"
              placeholder="0.00"
              error={errors.stopLossPrice?.message}
              {...register('stopLossPrice')}
            />
          </>
        )}

        {mode === 'STANDARD' && type !== 'MARKET' && (
          <Input
            label={
              type === 'STOP_LOSS' || type === 'TAKE_PROFIT'
                ? 'Limit price (optional)'
                : `Price (${quoteAsset})`
            }
            type="number"
            step="0.01"
            placeholder="0.00"
            error={errors.price?.message}
            {...register('price')}
          />
        )}

        <Button
          type="submit"
          variant={side === 'BUY' ? 'primary' : 'danger'}
          isLoading={isPending}
          className="w-full"
        >
          {mode === 'OCO' ? `Place OCO ${baseAsset}` : `${side} ${baseAsset}`}
        </Button>
      </form>
    </div>
  );
}
