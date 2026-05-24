import { formatDistanceToNowStrict } from 'date-fns';
import type { ProcessDto } from '../lib/types';
import { ScoreBadge } from './ScoreBadge';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';

interface ProcessCardProps {
  process: ProcessDto;
}

export function ProcessCard({ process }: ProcessCardProps) {
  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <CardTitle className="truncate">{process.name ?? 'Unknown process'}</CardTitle>
            <div className="mt-1 font-mono text-sm text-slate-500">PID {process.pid ?? '--'}</div>
          </div>
          <ScoreBadge score={process.maxScore} />
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="min-h-10 break-all font-mono text-sm text-slate-400">{process.path ?? 'Path unresolved'}</div>
        <div className="grid grid-cols-2 gap-4 text-sm">
          <div>
            <div className="text-slate-500">Connections</div>
            <div className="font-mono tabular-nums text-slate-100">{process.connectionCount}</div>
          </div>
          <div>
            <div className="text-slate-500">Signed</div>
            <div className="font-mono text-slate-100">
              {process.signed === null ? 'UNKNOWN' : process.signed ? 'YES' : 'NO'}
            </div>
          </div>
        </div>
        <div className="text-sm text-slate-400">{process.signer ?? 'Signer unavailable'}</div>
        <div className="text-right font-mono text-sm text-slate-500">
          {formatDistanceToNowStrict(new Date(process.lastSeen), { addSuffix: true })}
        </div>
      </CardContent>
    </Card>
  );
}
