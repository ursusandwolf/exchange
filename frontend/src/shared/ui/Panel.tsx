import type { ReactNode } from 'react';
import { cn } from '../lib/cn';

interface PanelProps {
  title?: string;
  children: ReactNode;
  className?: string;
  bodyClassName?: string;
  action?: ReactNode;
}

export function Panel({ title, children, className, bodyClassName, action }: PanelProps) {
  return (
    <section className={cn('flex flex-col rounded-xl border border-border/80 bg-card shadow-sm', className)}>
      {title ? (
        <header className="flex items-center justify-between gap-3 border-b px-4 py-3">
          <h2 className="text-sm font-semibold tracking-wide text-foreground">{title}</h2>
          {action}
        </header>
      ) : null}
      <div className={cn('flex-1 p-4', bodyClassName)}>{children}</div>
    </section>
  );
}
