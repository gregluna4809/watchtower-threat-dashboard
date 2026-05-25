import { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ConnectionTable } from '../components/ConnectionTable';
import { CountryFlag } from '../components/CountryFlag';
import { EntityHeader } from '../components/detail/EntityHeader';
import { MetadataCard } from '../components/detail/MetadataCard';
import { ObservationTimelineChart } from '../components/detail/ObservationTimelineChart';
import { ThreatScoreCard } from '../components/detail/ThreatScoreCard';
import { WindowSelector } from '../components/detail/WindowSelector';
import { Alert } from '../components/ui/alert';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Skeleton } from '../components/ui/skeleton';
import { ApiError } from '../lib/apiClient';
import type { EndpointSummary, ObservationWindow } from '../lib/types';
import { useProcess } from '../hooks/useProcess';
import { useProcessConnections } from '../hooks/useProcessConnections';
import { useProcessObservations } from '../hooks/useProcessObservations';

function parseId(value: string | undefined) {
  if (!value) {
    return undefined;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
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
          to="/processes"
          className="inline-flex h-10 items-center justify-center rounded-md border border-slate-700 bg-slate-950 px-4 text-sm font-medium text-slate-200 hover:bg-slate-800"
        >
          Back to Processes
        </Link>
      </CardContent>
    </Card>
  );
}

export function ProcessDetailPage() {
  const id = parseId(useParams().id);
  const [window, setWindow] = useState<ObservationWindow>('24h');
  const process = useProcess(id);
  const connections = useProcessConnections(id, { limit: 100, offset: 0 });
  const observations = useProcessObservations(id, window);

  const endpoints = useMemo(() => {
    const seen = new Set<number>();
    return (connections.data?.items ?? [])
      .map((connection) => connection.endpoint)
      .filter((endpoint): endpoint is EndpointSummary => {
        if (endpoint === null || seen.has(endpoint.id)) {
          return false;
        }
        seen.add(endpoint.id);
        return true;
      })
      .slice(0, 12);
  }, [connections.data?.items]);

  if (isNotFound(process.error)) {
    return <NotFoundCard />;
  }

  if (process.error) {
    return <Alert variant="destructive">{process.error.message}</Alert>;
  }

  if (process.isLoading || !process.data) {
    return <Skeleton className="h-[520px] w-full" />;
  }

  const item = process.data;

  return (
    <div className="space-y-6">
      <EntityHeader
        backTo="/processes"
        backLabel="Back to Processes"
        title={item.name ?? 'Unknown process'}
        subtitle={`PID ${item.pid ?? '--'}`}
        score={item.maxScore}
      />

      <div className="grid gap-6 xl:grid-cols-[minmax(0,2fr)_minmax(320px,1fr)]">
        <MetadataCard
          title="Process Metadata"
          items={[
            { label: 'PID', value: item.pid },
            { label: 'Name', value: item.name },
            { label: 'Path', value: item.path },
            { label: 'Signed', value: item.signed === null ? 'UNKNOWN' : item.signed ? 'YES' : 'NO' },
            { label: 'Signer', value: item.signer },
            { label: 'Connections', value: item.connectionCount },
            { label: 'First Seen', value: formatDate(item.firstSeen) },
            { label: 'Last Seen', value: formatDate(item.lastSeen) }
          ]}
        />
        <ThreatScoreCard score={item.maxScore} />
      </div>

      <div className="flex justify-end">
        <WindowSelector value={window} onChange={setWindow} />
      </div>
      <ObservationTimelineChart buckets={observations.data} isLoading={observations.isLoading} />

      <Card>
        <CardHeader>
          <CardTitle>Connections</CardTitle>
        </CardHeader>
        <CardContent>
          <ConnectionTable
            connections={connections.data?.items}
            isLoading={connections.isLoading}
            error={connections.error}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Endpoints Contacted</CardTitle>
        </CardHeader>
        <CardContent>
          {endpoints.length === 0 ? (
            <div className="text-sm text-slate-400">No endpoints recorded for this process.</div>
          ) : (
            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
              {endpoints.map((endpoint) => (
                <Link
                  key={endpoint.id}
                  to={`/endpoints/${endpoint.id}`}
                  className="rounded-md border border-slate-800 bg-slate-950 p-3 transition-colors hover:bg-slate-800/60"
                >
                  <div className="font-mono text-sm text-slate-100">{endpoint.ip}</div>
                  <div className="mt-2 flex items-center gap-2 text-sm text-slate-400">
                    <CountryFlag iso={endpoint.countryIso} />
                    <span>{endpoint.countryName ?? endpoint.countryIso ?? '--'}</span>
                  </div>
                  <div className="mt-2 truncate text-xs text-slate-500">{endpoint.asnOrg ?? 'ASN unavailable'}</div>
                </Link>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
