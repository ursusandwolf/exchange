export const TRADING_SYMBOLS = ['BTC/USDT', 'ETH/USDT'] as const;

export type TradingSymbol = (typeof TRADING_SYMBOLS)[number];

export function splitSymbol(symbol: string): { base: string; quote: string } {
  const [base, quote] = symbol.split('/');
  return { base: base ?? '', quote: quote ?? '' };
}
