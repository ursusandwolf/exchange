import React, { useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

interface OrderBookUpdate {
  symbol: string;
  bids: { price: number; quantity: number }[];
  asks: { price: number; quantity: number }[];
}

export const OrderBook: React.FC<{ symbol: string }> = ({ symbol }) => {
  const [data, setData] = useState<OrderBookUpdate | null>(null);

  useEffect(() => {
    // Используем абсолютный путь к бэкенду, чтобы избежать проблем с прокси Vite
    const socket = new SockJS('http://localhost:8080/ws-exchange');
    const client = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        console.log('Connected to OrderBook WebSocket');
        client.subscribe(`/topic/orderbook/${symbol}`, (message) => {
          setData(JSON.parse(message.body));
        });
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame);
      },
    });

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [symbol]);

  if (!data) return <div className="text-center text-muted-foreground italic">Loading order book...</div>;

  return (
    <div className="flex flex-col h-full text-sm">
      <div className="grid grid-cols-2 font-bold border-b pb-2 mb-2">
        <span>Price</span>
        <span className="text-right">Quantity</span>
      </div>
      
      <div className="flex-1 overflow-y-auto space-y-1">
        {/* Asks (Sell Orders) in reverse order */}
        <div className="flex flex-col-reverse">
          {data.asks.map((ask, i) => (
            <div key={i} className="grid grid-cols-2 text-destructive">
              <span>{ask.price.toFixed(2)}</span>
              <span className="text-right">{ask.quantity.toFixed(4)}</span>
            </div>
          ))}
        </div>

        <div className="py-2 border-y my-2 text-center font-bold">
          Spread: {(data.asks[0]?.price - data.bids[0]?.price || 0).toFixed(2)}
        </div>

        {/* Bids (Buy Orders) */}
        <div>
          {data.bids.map((bid, i) => (
            <div key={i} className="grid grid-cols-2 text-primary">
              <span>{bid.price.toFixed(2)}</span>
              <span className="text-right">{bid.quantity.toFixed(4)}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
