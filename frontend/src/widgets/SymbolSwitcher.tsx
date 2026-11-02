import { TRADING_SYMBOLS, type TradingSymbol } from '../shared/config/symbols';
import { useSymbolStore } from '../entities/market';
import { cn } from '../shared/lib/cn';

export function SymbolSwitcher() {
  const symbol = useSymbolStore((state) => state.symbol);
  const setSymbol = useSymbolStore((state) => state.setSymbol);

  return (
    <div className="flex flex-wrap items-center gap-2" role="group" aria-label="Trading pair">
      {TRADING_SYMBOLS.map((item) => (
        <button
          key={item}
          type="button"
          onClick={() => setSymbol(item as TradingSymbol)}
          className={cn(
            'rounded-full border px-3 py-1 text-xs font-semibold tracking-wide transition-colors',
            symbol === item
              ? 'border-primary bg-primary text-primary-foreground'
              : 'border-border bg-card text-muted-foreground hover:text-foreground'
          )}
        >
          {item}
        </button>
      ))}
    </div>
  );
}
