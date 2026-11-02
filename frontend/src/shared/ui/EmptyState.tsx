import type { ReactNode } from 'react';
import { cn } from '../lib/cn';

interface EmptyStateProps {
  title: string;
  description?: string;
  className?: string;
  action?: ReactNode;
}

export function EmptyState({ title, description, className, action }: EmptyStateProps) {
  return (
    <div
      className={cn(
        'flex h-full min-h-[120px] flex-col items-center justify-center gap-2 rounded-lg border border-dashed px-4 py-6 text-center',
        className
      )}
      role="status"
    >
      <p className="text-sm font-medium text-foreground">{title}</p>
      {description ? <p className="max-w-sm text-xs text-muted-foreground">{description}</p> : null}
      {action}
    </div>
  );
}
