import { useQuery } from '@tanstack/react-query';
import { Activity } from 'lucide-react';
import { apiClient } from '../lib/apiClient';
import { cn } from '../lib/utils';
import { useWebSocketStatus } from '../hooks/useLiveStats';

export function HealthPill() {
  const wsStatus = useWebSocketStatus();
  const { data, isError } = useQuery({
    queryKey: ['health'],
    queryFn: apiClient.health,
    refetchInterval: 10000,
    retry: 1
  });

  const up = data?.status === 'UP' && !isError;

  return (
    <div className="inline-flex items-center gap-3 rounded-md border border-slate-800 bg-slate-900 px-3 py-2 text-sm text-slate-300">
      <div className="inline-flex items-center gap-2">
        <span className={cn('h-2 w-2 rounded-full', up ? 'bg-emerald-400' : 'bg-red-500')} />
        <Activity className="h-4 w-4 text-slate-500" />
        <span className="font-mono text-xs">{up ? `REST ${data?.version ?? 'dev'}` : 'REST DOWN'}</span>
      </div>
      <div className="h-4 w-px bg-slate-800" />
      <div className="inline-flex items-center gap-2">
        <span className={cn('h-2 w-2 rounded-full', wsStatus === 'connected' ? 'bg-emerald-400' : 'bg-slate-600')} />
        <span className="font-mono text-xs">WS {wsStatus === 'connected' ? 'UP' : 'RETRY'}</span>
      </div>
    </div>
  );
}
