import { ChevronLeft } from 'lucide-react';
import { Link } from 'react-router-dom';
import { ScoreBadge } from '../ScoreBadge';

interface EntityHeaderProps {
  backTo: string;
  backLabel: string;
  title: string;
  subtitle?: string;
  score?: number | null;
}

export function EntityHeader({ backTo, backLabel, title, subtitle, score }: EntityHeaderProps) {
  return (
    <div className="space-y-4">
      <Link to={backTo} className="inline-flex items-center gap-2 text-sm text-slate-400 hover:text-slate-100">
        <ChevronLeft className="h-4 w-4" />
        {backLabel}
      </Link>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <h2 className="break-words text-2xl font-semibold text-slate-100">{title}</h2>
          {subtitle ? <p className="mt-1 break-words text-sm text-slate-400">{subtitle}</p> : null}
        </div>
        <ScoreBadge score={score} />
      </div>
    </div>
  );
}
