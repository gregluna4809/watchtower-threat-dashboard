import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { EntityHeader } from '../components/detail/EntityHeader';
import { MetadataCard } from '../components/detail/MetadataCard';
import { ObservationTimelineChart } from '../components/detail/ObservationTimelineChart';
import { ThreatScoreCard } from '../components/detail/ThreatScoreCard';
import { WindowSelector } from '../components/detail/WindowSelector';
import { Alert } from '../components/ui/alert';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Skeleton } from '../components/ui/skeleton';
import { ApiError } from '../lib/apiClient';
import type { ObservationWindow } from '../lib/types';
import { useConnection } from '../hooks/useConnection';
import { useConnectionObservations } from '../hooks/useConnectionObservations';

function parseId(value: string | undefined) {
  if (!value) {
    return undefined;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function endpointLabel(remoteIp: string | null, remotePort: number | null) {
  if (!remoteIp) {
    return 'listening';
  }
  return remotePort ? `${remoteIp}:${remotePort}` : remoteIp;
}

function formatDate(value: string | null | undefined) {
  return value ? new Date(value).toLocaleString() : '--';
}

function isNotFound(error: Error | null) {
  return error instanceof ApiError && error.status === 404;
}

function NotFoundCard() {
  return (
    <Card className="mx-auto max-w-xl text-center">
      <CardContent className="space-y-4 p-6">
        <div className="text-sm font-medium text-slate-100">Not found</div>
        <Link
          to="/connections"
          className="inline-flex h-10 items-center justify-center rounded-md border border-slate-700 bg-slate-950 px-4 text-sm font-medium text-slate-200 hover:bg-slate-800"
        >
          Back to Connections
        </Link>
      </CardContent>
    </Card>
  );
}

export function ConnectionDetailPage() {
  const id = parseId(useParams().id);
  const [window, setWindow] = useState<ObservationWindow>('24h');
  const connection = useConnection(id);
  const observations = useConnectionObservations(id, window);

  if (isNotFound(connection.error)) {
    return <NotFoundCard />;
  }

  if (connection.error) {
    return <Alert variant="destructive">{connection.error.message}</Alert>;
  }

  if (connection.isLoading || !connection.data) {
    return <Skeleton className="h-[520px] w-full" />;
  }

  const item = connection.data;

  return (
    <div className="space-y-6">
      <EntityHeader
        backTo="/connections"
        backLabel="Back to Connections"
        title={`Connection ${item.id}`}
        subtitle={`${item.protocol} ${item.state ?? '--'} ${endpointLabel(item.remoteIp, item.remotePort)}`}
        score={item.latestScore}
      />

      <div className="grid gap-6 xl:grid-cols-3">
        <Card>
          <CardHeader>
            <CardTitle>Process</CardTitle>
          </CardHeader>
          <CardContent>
            {item.process ? (
              <Link to={`/processes/${item.process.id}`} className="block rounded-md bg-slate-950 p-3 hover:bg-slate-800/60">
                <div className="text-sm font-medium text-slate-100">{item.process.name ?? 'Unknown process'}</div>
                <div className="mt-1 font-mono text-sm text-slate-500">PID {item.process.pid ?? '--'}</div>
                <div className="mt-3 break-all font-mono text-xs text-slate-400">{item.process.path ?? 'Path unresolved'}</div>
              </Link>
            ) : (
              <div className="text-sm text-slate-400">Process unresolved</div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Endpoint</CardTitle>
          </CardHeader>
          <CardContent>
            {item.endpoint ? (
              <Link to={`/endpoints/${item.endpoint.id}`} className="block rounded-md bg-slate-950 p-3 hover:bg-slate-800/60">
                <div className="font-mono text-sm text-slate-100">{item.endpoint.ip}</div>
                <div className="mt-2 text-sm text-slate-400">{item.endpoint.countryName ?? item.endpoint.countryIso ?? '--'}</div>
                <div className="mt-2 truncate text-xs text-slate-500">{item.endpoint.asnOrg ?? 'ASN unavailable'}</div>
              </Link>
            ) : (
              <div className="text-sm text-slate-400">Endpoint unavailable for this row.</div>
            )}
          </CardContent>
        </Card>

        <ThreatScoreCard score={item.latestScore} />
      </div>

      <MetadataCard
        title="Connection Metadata"
        items={[
          { label: 'Protocol', value: item.protocol },
          { label: 'State', value: item.state },
          { label: 'Local', value: `${item.localIp}:${item.localPort}` },
          { label: 'Remote', value: endpointLabel(item.remoteIp, item.remotePort) },
          { label: 'Observations', value: item.observationCount },
          { label: 'First Seen', value: formatDate(item.firstSeen) },
          { label: 'Last Seen', value: formatDate(item.lastSeen) }
        ]}
      />

      <div className="flex justify-end">
        <WindowSelector value={window} onChange={setWindow} />
      </div>
      <ObservationTimelineChart buckets={observations.data} isLoading={observations.isLoading} />
    </div>
  );
}
