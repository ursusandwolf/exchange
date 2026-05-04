import React, { useEffect, useRef } from 'react';
import { createChart, type ISeriesApi, type CandlestickData, CandlestickSeries } from 'lightweight-charts';
import { api } from '../shared/api';

interface ChartProps {
  symbol: string;
}

export const Chart: React.FC<ChartProps> = ({ symbol }) => {
  const chartContainerRef = useRef<HTMLDivElement>(null);
  const seriesRef = useRef<ISeriesApi<'Candlestick'>>();

  useEffect(() => {
    if (!chartContainerRef.current) return;

    const chart = createChart(chartContainerRef.current, {
      width: chartContainerRef.current.clientWidth,
      height: chartContainerRef.current.clientHeight,
      layout: {
        background: { color: 'transparent' },
        textColor: '#666',
      },
      grid: {
        vertLines: { color: '#f0f0f0' },
        horzLines: { color: '#f0f0f0' },
      },
      timeScale: {
        timeVisible: true,
        secondsVisible: false,
      },
    });

    const candlestickSeries = chart.addSeries(CandlestickSeries, {
      upColor: '#26a69a',
      downColor: '#ef5350',
      borderVisible: false,
      wickUpColor: '#26a69a',
      wickDownColor: '#ef5350',
    });

    seriesRef.current = candlestickSeries;

    const resizeObserver = new ResizeObserver(entries => {
      if (entries[0].contentRect && chartContainerRef.current) {
        const { width, height } = entries[0].contentRect;
        chart.applyOptions({ width, height });
      }
    });

    resizeObserver.observe(chartContainerRef.current);

    // Fetch initial data
    const fetchHistory = async () => {
      try {
        const to = Math.floor(Date.now() / 1000);
        const from = to - 24 * 60 * 60; // 24 hours ago
        const response = await api.get(`/tradingview/history`, {
          params: { symbol, resolution: '1', from, to }
        });
        
        if (response.data.s === 'ok') {
          const data: CandlestickData[] = response.data.t.map((t: number, i: number) => ({
            time: t,
            open: response.data.o[i],
            high: response.data.h[i],
            low: response.data.l[i],
            close: response.data.c[i],
          }));
          candlestickSeries.setData(data);
        }
      } catch (error) {
        console.error('Failed to fetch chart history', error);
      }
    };

    fetchHistory();

    return () => {
      resizeObserver.disconnect();
      chart.remove();
    };
  }, [symbol]);

  return (
    <div className="w-full h-full relative">
      <div ref={chartContainerRef} className="absolute inset-0" />
    </div>
  );
};
