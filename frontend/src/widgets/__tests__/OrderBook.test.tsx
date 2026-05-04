import { describe, it, expect, vi } from 'vitest';
import { render } from '@testing-library/react';
import { OrderBook } from '../OrderBook';
import SockJS from 'sockjs-client';

// Mock SockJS
vi.mock('sockjs-client', () => {
  return {
    default: vi.fn().mockImplementation((url) => ({
      url,
      close: vi.fn(),
    })),
  };
});

// Mock @stomp/stompjs
vi.mock('@stomp/stompjs', () => {
  return {
    Client: vi.fn().mockImplementation(() => ({
      activate: vi.fn(),
      deactivate: vi.fn(),
      subscribe: vi.fn(),
    })),
  };
});

describe('OrderBook WebSocket Connection', () => {
  it('should use a relative URL for SockJS connection', () => {
    render(<OrderBook symbol="BTC/USDT" />);
    
    // Check if SockJS was called with the correct relative URL
    expect(SockJS).toHaveBeenCalledWith('/ws-exchange');
    // Ensure it's not the old absolute URL
    expect(SockJS).not.toHaveBeenCalledWith('http://localhost:8080/ws-exchange');
  });
});
