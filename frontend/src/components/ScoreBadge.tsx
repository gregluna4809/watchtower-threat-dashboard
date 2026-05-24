import { Badge } from './ui/badge';
import { cn } from '../lib/utils';

interface ScoreBadgeProps {
  score: number | null | undefined;
}

export function ScoreBadge({ score }: ScoreBadgeProps) {
  const value = score ?? 0;
  const className =
    value >= 75
      ? 'border-red-800 bg-red-950 text-red-200'
      : value >= 50
        ? 'border-orange-800 bg-orange-950 text-orange-200'
        : value >= 25
          ? 'border-amber-800 bg-amber-950 text-amber-200'
          : 'border-emerald-800 bg-emerald-950 text-emerald-200';

  return <Badge className={cn('min-w-12 justify-center font-mono', className)}>{value}</Badge>;
}
