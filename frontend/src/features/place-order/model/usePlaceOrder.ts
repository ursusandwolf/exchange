import { useMutation, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { toast } from 'sonner';
import { api } from '../../../shared/api';
import { splitSymbol } from '../../../shared/config/symbols';

export type OrderSide = 'BUY' | 'SELL';
export type OrderType = 'LIMIT' | 'MARKET' | 'STOP_LOSS' | 'TAKE_PROFIT';
export type OrderMode = 'STANDARD' | 'OCO';

export interface PlaceOrderValues {
  quantity: number;
  price?: number | null;
  triggerPrice?: number | null;
  takeProfitPrice?: number | null;
  stopLossTriggerPrice?: number | null;
  stopLossPrice?: number | null;
}

interface StandardOrderRequest {
  baseAsset: string;
  quoteAsset: string;
  side: OrderSide;
  type: OrderType;
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

function errorMessage(error: unknown): string {
  if (error instanceof AxiosError) {
    const data = error.response?.data;
    if (typeof data === 'string') return data;
    if (data && typeof data === 'object' && 'message' in data && typeof data.message === 'string') {
      return data.message;
    }
  }
  return 'Failed to place order';
}

export function usePlaceOrder(symbol: string) {
  const queryClient = useQueryClient();
  const { base, quote } = splitSymbol(symbol);

  const mutation = useMutation({
    mutationFn: (payload: MutationPayload) => api.post(payload.endpoint, payload.body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['portfolio'] });
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      toast.success('Order placed');
    },
    onError: (error: unknown) => {
      toast.error(errorMessage(error));
    },
  });

  const submitStandard = async (side: OrderSide, type: OrderType, data: PlaceOrderValues) => {
    await mutation.mutateAsync({
      endpoint: '/orders/submit',
      body: {
        baseAsset: base,
        quoteAsset: quote,
        side,
        type,
        quantity: data.quantity,
        price: type === 'MARKET' ? null : data.price,
        triggerPrice: type === 'STOP_LOSS' || type === 'TAKE_PROFIT' ? data.triggerPrice : null,
      },
    });
  };

  const submitOco = async (data: PlaceOrderValues) => {
    if (data.takeProfitPrice == null || data.stopLossTriggerPrice == null) {
      toast.error('OCO requires take-profit and stop-loss trigger prices');
      return false;
    }
    await mutation.mutateAsync({
      endpoint: '/orders/oco',
      body: {
        baseAsset: base,
        quoteAsset: quote,
        side: 'SELL',
        quantity: data.quantity,
        takeProfitPrice: data.takeProfitPrice,
        stopLossTriggerPrice: data.stopLossTriggerPrice,
        stopLossPrice: data.stopLossPrice ?? null,
      },
    });
    return true;
  };

  return {
    baseAsset: base,
    quoteAsset: quote,
    isPending: mutation.isPending,
    submitStandard,
    submitOco,
  };
}
