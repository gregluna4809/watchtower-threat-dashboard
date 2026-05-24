import type { HTMLAttributes } from 'react';
import { cn } from '../../lib/utils';

interface AlertProps extends HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'destructive';
}

export function Alert({ className, variant = 'default', ...props }: AlertProps) {
  return (
    <div
      role="alert"
      className={cn(
        'rounded-md border p-4 text-sm',
        variant === 'destructive'
          ? 'border-red-900/70 bg-red-950/40 text-red-200'
          : 'border-slate-800 bg-slate-950 text-slate-300',
        className
      )}
      {...props}
    />
  );
}
