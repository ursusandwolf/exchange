import { Chart, OrderBook, TradingForm, Portfolio, RecentTrades, SymbolSwitcher } from '../widgets';
import { Panel } from '../shared/ui';
import { useSymbolStore } from '../entities/market';

export function DashboardPage() {
  const symbol = useSymbolStore((state) => state.symbol);

  return (
    <div className="flex h-full flex-col gap-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">Spot desk</p>
          <h1 className="text-2xl font-semibold tracking-tight text-foreground">{symbol}</h1>
        </div>
        <SymbolSwitcher />
      </div>

      <div className="grid grid-cols-12 gap-4 lg:gap-5">
        <div className="col-span-12 space-y-4 lg:col-span-8">
          <Panel title={`${symbol} · 1m`} className="min-h-[320px] lg:min-h-[460px]" bodyClassName="p-2 sm:p-3 h-[280px] sm:h-[420px]">
            <Chart symbol={symbol} />
          </Panel>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <Panel title="Trade">
              <TradingForm symbol={symbol} />
            </Panel>
            <Panel title="Recent trades" bodyClassName="flex min-h-[280px] flex-col p-4">
              <RecentTrades symbol={symbol} />
            </Panel>
          </div>
        </div>

        <div className="col-span-12 space-y-4 lg:col-span-4">
          <Panel title="Order book" bodyClassName="flex min-h-[320px] flex-col p-4 lg:min-h-[400px]">
            <OrderBook symbol={symbol} />
          </Panel>
          <Panel title="Portfolio" bodyClassName="min-h-[220px]">
            <Portfolio />
          </Panel>
        </div>
      </div>
    </div>
  );
}
