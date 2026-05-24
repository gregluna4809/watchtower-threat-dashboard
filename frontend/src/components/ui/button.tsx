import type { ButtonHTMLAttributes } from 'react';
import { cn } from '../../lib/utils';

type ButtonVariant = 'default' | 'ghost' | 'outline' | 'destructive';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
}

const variants: Record<ButtonVariant, string> = {
  default: 'bg-amber-500 text-slate-950 hover:bg-amber-400 focus-visible:ring-amber-500',
  ghost: 'text-slate-300 hover:bg-slate-800 hover:text-slate-100 focus-visible:ring-amber-500',
  outline:
    'border border-slate-700 bg-slate-950 text-slate-200 hover:bg-slate-800 focus-visible:ring-amber-500',
  destructive: 'bg-red-600 text-white hover:bg-red-500 focus-visible:ring-red-500'
};

export function Button({ className, variant = 'default', type = 'button', ...props }: ButtonProps) {
  return (
    <button
      type={type}
      className={cn(
        'inline-flex h-10 items-center justify-center rounded-md px-4 py-2 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 disabled:pointer-events-none disabled:opacity-50',
        variants[variant],
        className
      )}
      {...props}
    />
  );
}
