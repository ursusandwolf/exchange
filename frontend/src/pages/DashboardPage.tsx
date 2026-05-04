import React from 'react';
import { Chart, OrderBook, TradingForm, Portfolio, RecentTrades } from '../widgets';

export const DashboardPage: React.FC = () => {
  const symbol = "BTC/USDT";

  return (
    <div className="grid grid-cols-12 gap-6 h-full">
      {/* Chart Section */}
      <div className="col-span-12 lg:col-span-8 space-y-6">
        <div className="bg-card border rounded-lg p-4 h-[500px]">
          <Chart symbol={symbol} />
        </div>
        
        <div className="grid grid-cols-2 gap-6">
          <div className="bg-card border rounded-lg p-4 min-h-[350px]">
            <h3 className="font-bold mb-4 border-b pb-2">Trading</h3>
            <TradingForm symbol={symbol} />
          </div>
          <div className="bg-card border rounded-lg p-4 min-h-[350px] flex flex-col">
            <h3 className="font-bold mb-4 border-b pb-2">Recent Trades</h3>
            <RecentTrades symbol={symbol} />
          </div>
        </div>
      </div>

      {/* Sidebar Section */}
      <div className="col-span-12 lg:col-span-4 space-y-6">
        <div className="bg-card border rounded-lg p-4 h-[450px] flex flex-col">
          <h3 className="font-bold mb-4 border-b pb-2">Order Book</h3>
          <OrderBook symbol={symbol} />
        </div>
        <div className="bg-card border rounded-lg p-4 min-h-[250px]">
          <h3 className="font-bold mb-4 border-b pb-2">Portfolio</h3>
          <Portfolio />
        </div>
      </div>
    </div>
  );
};
