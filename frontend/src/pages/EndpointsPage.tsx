import { formatDistanceToNowStrict } from 'date-fns';
import { CountryFlag } from '../components/CountryFlag';
import { EndpointCard } from '../components/EndpointCard';
import { ScoreBadge } from '../components/ScoreBadge';
import { useEndpoints } from '../hooks/useEndpoints';
import { Alert } from '../components/ui/alert';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { ScrollArea } from '../components/ui/scroll-area';
import { Skeleton } from '../components/ui/skeleton';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../components/ui/table';

export function EndpointsPage() {
  const endpoints = useEndpoints();
  const sorted = [...(endpoints.data?.items ?? [])].sort((a, b) => (b.latestScore ?? 0) - (a.latestScore ?? 0));

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-semibold text-slate-100">Endpoints</h2>
        <p className="mt-1 text-sm text-slate-400">Remote IPs observed by local network connections.</p>
      </div>

      {endpoints.error ? <Alert variant="destructive">{endpoints.error.message}</Alert> : null}

      <div className="grid gap-4 xl:grid-cols-3">
        {endpoints.isLoading
          ? Array.from({ length: 3 }).map((_, index) => <Skeleton key={index} className="h-64" />)
          : sorted.slice(0, 3).map((endpoint) => <EndpointCard key={endpoint.id} endpoint={endpoint} />)}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>All Endpoints</CardTitle>
        </CardHeader>
        <CardContent>
          {endpoints.isLoading ? (
            <div className="space-y-3">
              {Array.from({ length: 10 }).map((_, index) => (
                <Skeleton key={index} className="h-12" />
              ))}
            </div>
          ) : sorted.length === 0 ? (
            <Card className="mx-auto max-w-xl text-center">
              <CardContent className="p-6">
                <div className="text-sm font-medium text-slate-100">No endpoints have been observed.</div>
                <div className="mt-2 text-sm text-slate-400">Remote IPs appear here after connection ingest.</div>
              </CardContent>
            </Card>
          ) : (
            <ScrollArea className="max-h-[680px] rounded-md border border-slate-800">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-24 text-right">Score</TableHead>
                    <TableHead className="text-right">IP</TableHead>
                    <TableHead>Country</TableHead>
                    <TableHead className="text-right">ASN</TableHead>
                    <TableHead>Organization</TableHead>
                    <TableHead className="text-right">Connections</TableHead>
                    <TableHead className="text-right">Last Seen</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {sorted.map((endpoint) => (
                    <TableRow key={endpoint.id}>
                      <TableCell className="text-right">
                        <ScoreBadge score={endpoint.latestScore} />
                      </TableCell>
                      <TableCell className="text-right font-mono text-sm">{endpoint.ip}</TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <CountryFlag iso={endpoint.countryIso} />
                          <span>{endpoint.countryName ?? '--'}</span>
                        </div>
                      </TableCell>
                      <TableCell className="text-right font-mono text-sm">{endpoint.asn ?? '--'}</TableCell>
                      <TableCell>{endpoint.asnOrg ?? '--'}</TableCell>
                      <TableCell className="text-right font-mono text-sm tabular-nums">
                        {endpoint.connectionCount}
                      </TableCell>
                      <TableCell className="text-right font-mono text-sm">
                        {formatDistanceToNowStrict(new Date(endpoint.lastSeen), { addSuffix: true })}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </ScrollArea>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
