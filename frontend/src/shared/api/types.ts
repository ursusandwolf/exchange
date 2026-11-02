export interface AuthResponse {
  token: string;
  username: string;
  userId: string;
  admin: boolean;
}

export interface PortfolioResponse {
  userId: string;
  balances: Record<string, number>;
  reserved: Record<string, number>;
}

export interface TransactionRecordResponse {
  id: string;
  type: string;
  asset: string;
  amount: number;
  description?: string;
  timestamp: string;
}

export interface OrderBookLevel {
  price: number;
  quantity: number;
}

export interface OrderBookUpdate {
  symbol: string;
  bids: OrderBookLevel[];
  asks: OrderBookLevel[];
}

export interface TradeUpdate {
  id: string;
  price: number;
  quantity: number;
  timestamp: string;
}

export interface CandleUpdate {
  openTime: number;
  open: number;
  high: number;
  low: number;
  close: number;
}
