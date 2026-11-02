import { create } from 'zustand';
import { TRADING_SYMBOLS, type TradingSymbol } from '../../../shared/config/symbols';

interface SymbolState {
  symbol: TradingSymbol;
  setSymbol: (symbol: TradingSymbol) => void;
}

export const useSymbolStore = create<SymbolState>((set) => ({
  symbol: TRADING_SYMBOLS[0],
  setSymbol: (symbol) => set({ symbol }),
}));
