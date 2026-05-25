import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ConnectionTable } from '../components/ConnectionTable';
import { EntityHeader } from '../components/detail/EntityHeader';
import { EndpointMap } from '../components/detail/EndpointMap';
import { MetadataCard } from '../components/detail/MetadataCard';
import { ObservationTimelineChart } from '../components/detail/ObservationTimelineChart';
import { ThreatScoreCard } from '../components/detail/ThreatScoreCard';
import { WindowSelector } from '../components/detail/WindowSelector';
import { Alert } from '../components/ui/alert';
import { Card, CardContent } from '../components/ui/card';
import { Skeleton } from '../components/ui/skeleton';
import { ApiError } from '../lib/apiClient';
import type { ObservationWindow } from '../lib/types';
import { useEndpoint } from '../hooks/useEndpoint';
import { useEndpointConnections } from '../hooks/useEndpointConnections';
import { useEndpointObservations } from '../hooks/useEndpointObservations';
import { useEndpointScores } from '../hooks/useEndpointScores';

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
          to="/endpoints"
          className="inline-flex h-10 items-center justify-center rounded-md border border-slate-700 bg-slate-950 px-4 text-sm font-medium text-slate-200 hover:bg-slate-800"
        >
          Back to Endpoints
        </Link>
      </CardContent>
    </Card>
  );
}

export function EndpointDetailPage() {
  const id = parseId(useParams().id);
  const [window, setWindow] = useState<ObservationWindow>('24h');
  const endpoint = useEndpoint(id);
  const connections = useEndpointConnections(id, { limit: 100, offset: 0 });
  const observations = useEndpointObservations(id, window);
  const scores = useEndpointScores(id, 20);

  if (isNotFound(endpoint.error)) {
    return <NotFoundCard />;
  }

  if (endpoint.error) {
    return <Alert variant="destructive">{endpoint.error.message}</Alert>;
  }

  if (endpoint.isLoading || !endpoint.data) {
    return <Skeleton className="h-[520px] w-full" />;
  }

  const item = endpoint.data;

  return (
    <div className="space-y-6">
      <EntityHeader
        backTo="/endpoints"
        backLabel="Back to Endpoints"
        title={item.ip}
        subtitle={item.reverseDns ?? item.countryName ?? 'Endpoint detail'}
        score={item.latestScore}
      />

      <div className="grid gap-6 xl:grid-cols-[minmax(0,2fr)_minmax(320px,1fr)]">
        <div className="space-y-6">
          <EndpointMap latitude={item.latitude} longitude={item.longitude} city={item.city} />
          <MetadataCard
            title="Endpoint Metadata"
            items={[
              { label: 'IP', value: item.ip },
              { label: 'Hostname', value: item.reverseDns },
              { label: 'Country', value: item.countryName ?? item.countryIso },
              { label: 'City', value: item.city },
              { label: 'ASN', value: item.asn },
              { label: 'Organization', value: item.asnOrg },
              { label: 'Connections', value: item.connectionCount },
              { label: 'First Seen', value: formatDate(item.firstSeen) },
              { label: 'Last Seen', value: formatDate(item.lastSeen) }
            ]}
          />
        </div>
        <ThreatScoreCard score={item.latestScore} latest={scores.data?.latest} />
      </div>

      <div className="flex justify-end">
        <WindowSelector value={window} onChange={setWindow} />
      </div>
      <ObservationTimelineChart buckets={observations.data} isLoading={observations.isLoading} />

      <Card>
        <CardContent className="p-6">
          <ConnectionTable
            connections={connections.data?.items}
            isLoading={connections.isLoading}
            error={connections.error}
          />
        </CardContent>
      </Card>
    </div>
  );
}
