import { formatDistanceToNowStrict } from 'date-fns';
import { Link, useNavigate } from 'react-router-dom';
import type { ConnectionDto } from '../lib/types';
import { CountryFlag } from './CountryFlag';
import { ScoreBadge } from './ScoreBadge';
import { Alert } from './ui/alert';
import { Card, CardContent } from './ui/card';
import { ScrollArea } from './ui/scroll-area';
import { Skeleton } from './ui/skeleton';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from './ui/table';

interface ConnectionTableProps {
  connections: ConnectionDto[] | undefined;
  isLoading: boolean;
  error: Error | null;
}

function endpointLabel(connection: ConnectionDto) {
  if (!connection.remoteIp) {
    return 'listening';
  }

  return connection.remotePort ? `${connection.remoteIp}:${connection.remotePort}` : connection.remoteIp;
}

export function ConnectionTable({ connections, isLoading, error }: ConnectionTableProps) {
  const navigate = useNavigate();

  if (error) {
    return <Alert variant="destructive">{error.message}</Alert>;
  }

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 8 }).map((_, index) => (
          <Skeleton key={index} className="h-12 w-full" />
        ))}
      </div>
    );
  }

  if (!connections || connections.length === 0) {
    return (
      <Card className="mx-auto max-w-xl text-center">
        <CardContent className="p-6">
          <div className="text-sm font-medium text-slate-100">No connections match the current filters.</div>
          <div className="mt-2 text-sm text-slate-400">Let the backend run for a few polls or clear the filters.</div>
        </CardContent>
      </Card>
    );
  }

  const sorted = [...connections].sort((a, b) => (b.latestScore ?? 0) - (a.latestScore ?? 0));

  return (
    <ScrollArea className="max-h-[640px] rounded-md border border-slate-800">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-24 text-right">Score</TableHead>
            <TableHead>Process</TableHead>
            <TableHead>Protocol</TableHead>
            <TableHead className="text-right">Local</TableHead>
            <TableHead className="text-right">Remote</TableHead>
            <TableHead>Country</TableHead>
            <TableHead className="text-right">ASN</TableHead>
            <TableHead>State</TableHead>
            <TableHead className="text-right">Last Seen</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {sorted.map((connection) => (
            <TableRow
              key={connection.id}
              className="cursor-pointer"
              onClick={() => navigate(`/connections/${connection.id}`)}
            >
              <TableCell className="text-right">
                <ScoreBadge score={connection.latestScore} />
              </TableCell>
              <TableCell>
                <div className="font-medium text-slate-100">{connection.process?.name ?? 'Unknown'}</div>
                {connection.process ? (
                  <Link
                    to={`/processes/${connection.process.id}`}
                    className="font-mono text-xs text-slate-500 hover:text-slate-200"
                    onClick={(event) => event.stopPropagation()}
                  >
                    PID {connection.process.pid ?? '--'}
                  </Link>
                ) : (
                  <div className="font-mono text-xs text-slate-500">PID --</div>
                )}
              </TableCell>
              <TableCell className="font-mono text-sm">{connection.protocol}</TableCell>
              <TableCell className="text-right font-mono text-sm">
                {connection.localIp}:{connection.localPort}
              </TableCell>
              <TableCell className="text-right font-mono text-sm">
                {connection.endpoint ? (
                  <Link
                    to={`/endpoints/${connection.endpoint.id}`}
                    className="hover:text-amber-200"
                    onClick={(event) => event.stopPropagation()}
                  >
                    {endpointLabel(connection)}
                  </Link>
                ) : (
                  endpointLabel(connection)
                )}
              </TableCell>
              <TableCell>
                <div className="flex items-center gap-2">
                  <CountryFlag iso={connection.endpoint?.countryIso} />
                  <span className="text-sm text-slate-300">{connection.endpoint?.countryIso ?? '--'}</span>
                </div>
              </TableCell>
              <TableCell className="text-right font-mono text-sm">{connection.endpoint?.asnOrg ?? '--'}</TableCell>
              <TableCell className="font-mono text-sm">{connection.state ?? '--'}</TableCell>
              <TableCell className="text-right font-mono text-sm">
                {formatDistanceToNowStrict(new Date(connection.lastSeen), { addSuffix: true })}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </ScrollArea>
  );
}
