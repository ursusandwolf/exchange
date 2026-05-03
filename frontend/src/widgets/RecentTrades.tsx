import React, { useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

interface TradeUpdate {
  id: string;
  price: number;
  quantity: number;
  timestamp: string;
}

export const RecentTrades: React.FC<{ symbol: string }> = ({ symbol }) => {
  const [trades, setTrades] = useState<TradeUpdate[]>([]);

  useEffect(() => {
    // Используем абсолютный путь к бэкенду
    const socket = new SockJS('http://localhost:8080/ws-exchange');
    const client = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('Connected to Trades WebSocket');
        client.subscribe(`/topic/trades/${symbol}`, (message) => {
          const trade = JSON.parse(message.body);
          setTrades((prev) => [trade, ...prev].slice(0, 50));
        });
      },
    });

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [symbol]);

  return (
    <div className="flex flex-col h-full text-xs overflow-hidden">
      <div className="grid grid-cols-3 font-bold border-b pb-2 mb-2">
        <span>Price</span>
        <span className="text-right">Qty</span>
        <span className="text-right">Time</span>
      </div>
      
      <div className="flex-1 overflow-y-auto space-y-1">
        {trades.length === 0 && (
          <div className="text-center text-muted-foreground py-4 italic">No recent trades</div>
        )}
        {trades.map((trade) => (
          <div key={trade.id} className="grid grid-cols-3 animate-in fade-in slide-in-from-top-1">
            <span className="font-medium">{trade.price.toFixed(2)}</span>
            <span className="text-right">{trade.quantity.toFixed(4)}</span>
            <span className="text-right text-muted-foreground">
              {new Date(trade.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};
