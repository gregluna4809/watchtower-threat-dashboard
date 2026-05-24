import { formatDistanceToNowStrict } from 'date-fns';
import type { EndpointDto } from '../lib/types';
import { CountryFlag } from './CountryFlag';
import { ScoreBadge } from './ScoreBadge';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';

interface EndpointCardProps {
  endpoint: EndpointDto;
}

export function EndpointCard({ endpoint }: EndpointCardProps) {
  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between gap-4">
          <div>
            <CardTitle className="font-mono text-sm">{endpoint.ip}</CardTitle>
            <div className="mt-2 flex items-center gap-2 text-sm text-slate-400">
              <CountryFlag iso={endpoint.countryIso} />
              <span>{endpoint.countryName ?? 'Unresolved country'}</span>
            </div>
          </div>
          <ScoreBadge score={endpoint.latestScore} />
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid grid-cols-2 gap-4 text-sm">
          <div>
            <div className="text-slate-500">ASN</div>
            <div className="font-mono text-slate-100">{endpoint.asn ?? '--'}</div>
          </div>
          <div>
            <div className="text-slate-500">Connections</div>
            <div className="font-mono tabular-nums text-slate-100">{endpoint.connectionCount}</div>
          </div>
        </div>
        <div className="text-sm text-slate-400">{endpoint.asnOrg ?? 'ASN organization unavailable'}</div>
        <div className="text-sm text-slate-400">{endpoint.city ?? 'City unavailable'}</div>
        <div className="text-right font-mono text-sm text-slate-500">
          {formatDistanceToNowStrict(new Date(endpoint.lastSeen), { addSuffix: true })}
        </div>
      </CardContent>
    </Card>
  );
}
