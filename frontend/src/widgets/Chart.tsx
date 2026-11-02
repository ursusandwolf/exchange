import { useEffect, useRef, useState } from 'react';
import { createChart, CandlestickSeries, type IChartApi, type ISeriesApi, type UTCTimestamp } from 'lightweight-charts';
import { api } from '../shared/api';
import { EmptyState } from '../shared/ui';
import { useStompTopic } from '../shared/ws/useStompTopic';
import type { CandleUpdate } from '../shared/api/types';

interface HistoryResponse {
  s: string;
  t: number[];
  o: number[];
  h: number[];
  l: number[];
  c: number[];
}

export function Chart({ symbol }: { symbol: string }) {
  const chartContainerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const seriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null);
  const [status, setStatus] = useState<'loading' | 'ready' | 'empty' | 'error'>('loading');

  useStompTopic<CandleUpdate>(`/topic/candles/${symbol}/1m`, (candle) => {
    seriesRef.current?.update({
      time: candle.openTime as UTCTimestamp,
      open: candle.open,
      high: candle.high,
      low: candle.low,
      close: candle.close,
    });
  });

  useEffect(() => {
    if (!chartContainerRef.current) return;

    const chart = createChart(chartContainerRef.current, {
      width: chartContainerRef.current.clientWidth,
      height: chartContainerRef.current.clientHeight,
      layout: {
        background: { color: 'transparent' },
        textColor: '#64748b',
      },
      grid: {
        vertLines: { color: 'rgba(148, 163, 184, 0.2)' },
        horzLines: { color: 'rgba(148, 163, 184, 0.2)' },
      },
      timeScale: {
        timeVisible: true,
        secondsVisible: false,
      },
    });

    const series = chart.addSeries(CandlestickSeries, {
      upColor: '#059669',
      downColor: '#dc2626',
      borderVisible: false,
      wickUpColor: '#059669',
      wickDownColor: '#dc2626',
    });

    chartRef.current = chart;
    seriesRef.current = series;

    const resizeObserver = new ResizeObserver((entries) => {
      const entry = entries[0];
      if (!entry || !chartContainerRef.current) return;
      const { width, height } = entry.contentRect;
      chart.applyOptions({ width, height });
    });
    resizeObserver.observe(chartContainerRef.current);

    let cancelled = false;
    const fetchHistory = async () => {
      setStatus('loading');
      try {
        const to = Math.floor(Date.now() / 1000);
        const from = to - 24 * 60 * 60;
        const response = await api.get<HistoryResponse>('/tradingview/history', {
          params: { symbol, resolution: '1', from, to },
        });

        if (cancelled) return;

        if (response.data.s === 'ok' && response.data.t.length > 0) {
          series.setData(
            response.data.t.map((t, i) => ({
              time: t as UTCTimestamp,
              open: response.data.o[i],
              high: response.data.h[i],
              low: response.data.l[i],
              close: response.data.c[i],
            }))
          );
          setStatus('ready');
        } else {
          series.setData([]);
          setStatus('empty');
        }
      } catch {
        if (!cancelled) setStatus('error');
      }
    };

    void fetchHistory();

    return () => {
      cancelled = true;
      resizeObserver.disconnect();
      chart.remove();
      chartRef.current = null;
      seriesRef.current = null;
    };
  }, [symbol]);

  return (
    <div className="relative h-full min-h-[280px] w-full">
      <div ref={chartContainerRef} className="absolute inset-0" />
      {status === 'loading' ? (
        <div className="absolute inset-0 flex items-center justify-center bg-card/60 text-sm text-muted-foreground">
          Loading chart…
        </div>
      ) : null}
      {status === 'empty' ? (
        <div className="absolute inset-0 flex items-center justify-center bg-card/80 p-4">
          <EmptyState title="No candles yet" description={`History for ${symbol} is empty.`} />
        </div>
      ) : null}
      {status === 'error' ? (
        <div className="absolute inset-0 flex items-center justify-center bg-card/80 p-4">
          <EmptyState title="Chart unavailable" description="Could not load market history. Retry by switching symbol." />
        </div>
      ) : null}
    </div>
  );
}
