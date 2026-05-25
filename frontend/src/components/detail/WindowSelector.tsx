import type { ObservationWindow } from '../../lib/types';
import { cn } from '../../lib/utils';

interface WindowSelectorProps {
  value: ObservationWindow;
  onChange: (value: ObservationWindow) => void;
}

const windows: ObservationWindow[] = ['1h', '24h', '7d', '30d'];

export function WindowSelector({ value, onChange }: WindowSelectorProps) {
  return (
    <div className="inline-flex rounded-md border border-slate-800 bg-slate-950 p-1">
      {windows.map((window) => (
        <button
          key={window}
          type="button"
          className={cn(
            'h-8 min-w-14 rounded px-3 font-mono text-sm transition-colors',
            value === window ? 'bg-amber-500 text-slate-950' : 'text-slate-400 hover:bg-slate-800 hover:text-slate-100'
          )}
          onClick={() => onChange(window)}
        >
          {window}
        </button>
      ))}
    </div>
  );
}
