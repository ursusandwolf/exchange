import type { InputHTMLAttributes } from 'react';
import { cn } from '../lib/cn';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

export function Input({ id, label, error, className, ...props }: InputProps) {
  const inputId = id ?? props.name;
  return (
    <div className="space-y-1.5">
      <label htmlFor={inputId} className="text-sm font-medium text-foreground">
        {label}
      </label>
      <input
        id={inputId}
        className={cn(
          'w-full rounded-md border bg-background px-3 py-2 text-sm outline-none transition-shadow focus-visible:ring-2 focus-visible:ring-ring',
          error ? 'border-destructive' : 'border-input',
          className
        )}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${inputId}-error` : undefined}
        {...props}
      />
      {error ? (
        <p id={`${inputId}-error`} className="text-xs text-destructive" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  );
}
