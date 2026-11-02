import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { OrderBook } from '../OrderBook';

const subscribe = vi.fn(() => () => undefined);

vi.mock('../../shared/ws/stompBroker', () => ({
  getStompBroker: () => ({ subscribe }),
}));

describe('OrderBook', () => {
  it('subscribes to symbol topic via shared broker', () => {
    render(<OrderBook symbol="BTC/USDT" />);
    expect(subscribe).toHaveBeenCalledWith('/topic/orderbook/BTC/USDT', expect.any(Function));
    expect(screen.getByText(/Connecting to order book/i)).toBeTruthy();
  });
});
