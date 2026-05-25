import type { ThreatReason, ThreatScoreDto } from '../../lib/types';
import { cn } from '../../lib/utils';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { ScoreBadge } from '../ScoreBadge';

interface ThreatScoreCardProps {
  score?: number | null;
  latest?: ThreatScoreDto | null;
  reasons?: ThreatReason[];
}

function scoreClass(score: number | null | undefined) {
  const value = score ?? 0;
  if (value >= 75) {
    return 'text-red-200';
  }
  if (value >= 50) {
    return 'text-orange-200';
  }
  if (value >= 25) {
    return 'text-amber-200';
  }
  return 'text-emerald-200';
}

function reasonLabel(reason: ThreatReason) {
  const rule = typeof reason.rule === 'string' ? reason.rule : 'Rule';
  const points = typeof reason.points === 'number' ? `+${reason.points}` : null;
  return points ? `${rule} ${points}` : rule;
}

function reasonDetail(reason: ThreatReason) {
  if (typeof reason.detail === 'string' && reason.detail.length > 0) {
    return reason.detail;
  }
  return JSON.stringify(reason);
}

export function ThreatScoreCard({ score, latest, reasons }: ThreatScoreCardProps) {
  const displayScore = latest?.score ?? score ?? 0;
  const scoreReasons = latest?.reasons ?? reasons ?? [];

  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between gap-4">
          <CardTitle>Threat Score</CardTitle>
          <ScoreBadge score={displayScore} />
        </div>
      </CardHeader>
      <CardContent className="space-y-5">
        <div className={cn('font-mono text-5xl font-semibold tabular-nums', scoreClass(displayScore))}>
          {displayScore}
        </div>
        {scoreReasons.length === 0 ? (
          <div className="text-sm text-slate-400">No scoring reasons recorded.</div>
        ) : (
          <div className="space-y-3">
            {scoreReasons.map((reason, index) => (
              <div key={`${reasonLabel(reason)}-${index}`} className="rounded-md border border-slate-800 bg-slate-950 p-3">
                <div className="font-mono text-xs text-amber-200">{reasonLabel(reason)}</div>
                <div className="mt-2 text-sm text-slate-400">{reasonDetail(reason)}</div>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
