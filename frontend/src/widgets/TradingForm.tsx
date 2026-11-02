import { Link } from 'react-router-dom';
import { useAuthStore } from '../entities/user';
import { PlaceOrderForm } from '../features/place-order';
import { EmptyState } from '../shared/ui';

export function TradingForm({ symbol }: { symbol: string }) {
  const authenticated = useAuthStore((state) => state.authenticated);

  if (!authenticated) {
    return (
      <EmptyState
        title="Login to trade"
        description="Place limit, market, stop and OCO orders after signing in."
        className="min-h-[240px]"
        action={
          <div className="mt-2 flex gap-2">
            <Link
              to="/login"
              className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
            >
              Login
            </Link>
            <Link to="/register" className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-accent">
              Register
            </Link>
          </div>
        }
      />
    );
  }

  return <PlaceOrderForm symbol={symbol} />;
}
