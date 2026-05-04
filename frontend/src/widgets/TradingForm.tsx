import React from 'react';
import { Link } from 'react-router-dom';
import { useAuthStore } from '../entities/user';
import { PlaceOrderForm } from '../features/place-order';

export const TradingForm: React.FC<{ symbol: string }> = ({ symbol }) => {
  const token = useAuthStore(state => state.token);

  if (!token) {
    return (
      <div className="flex flex-col items-center justify-center h-[280px] text-center space-y-4 border-2 border-dashed rounded-lg p-4">
        <p className="text-muted-foreground">Please login to start trading</p>
        <div className="flex gap-4">
          <Link to="/login" className="px-4 py-2 bg-primary text-primary-foreground rounded hover:opacity-90 font-medium">
            Login
          </Link>
          <Link to="/register" className="px-4 py-2 border rounded hover:bg-accent font-medium">
            Register
          </Link>
        </div>
      </div>
    );
  }

  return <PlaceOrderForm symbol={symbol} />;
};
