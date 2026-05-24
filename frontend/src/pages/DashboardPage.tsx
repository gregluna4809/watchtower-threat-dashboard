import { Link } from 'react-router-dom';
import { ArrowUpRight } from 'lucide-react';
import { format } from 'date-fns';
import { Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { useConnections } from '../hooks/useConnections';
import { useLiveConnections } from '../hooks/useLiveConnections';
import { useLiveStats } from '../hooks/useLiveStats';
import { useScoreTimeline } from '../hooks/useScoreTimeline';
import { useSummary } from '../hooks/useSummary';
import { ConnectionTable } from '../components/ConnectionTable';
import { ScoreBadge } from '../components/ScoreBadge';
import { Alert } from '../components/ui/alert';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Skeleton } from '../components/ui/skeleton';

function StatCard({ label, value }: { label: string; value: string | number }) {
  return (
    <Card>
      <CardContent className="p-6">
        <div className="text-sm text-slate-400">{label}</div>
        <div className="mt-3 text-3xl font-semibold tabular-nums text-slate-100">{value}</div>
      </CardContent>
    </Card>
  );
}

export function DashboardPage() {
  useLiveConnections();
  useLiveStats();
  const summary = useSummary();
  const connections = useConnections({ limit: 100, offset: 0 });
  const timeline = useScoreTimeline('1h');
  const topConnections = [...(connections.data?.items ?? [])]
    .sort((a, b) => (b.latestScore ?? 0) - (a.latestScore ?? 0))
    .slice(0, 5);
  const timelineData = (timeline.data ?? []).map((point) => ({
    time: format(new Date(point.minute), 'HH:mm'),
    score: point.maxScore
  }));

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-semibold text-slate-100">Dashboard</h2>
        <p className="mt-1 text-sm text-slate-400">Current local network observations and advisory scoring.</p>
      </div>

      {summary.error ? <Alert variant="destructive">{summary.error.message}</Alert> : null}

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {summary.isLoading ? (
          Array.from({ length: 4 }).map((_, index) => <Skeleton key={index} className="h-32" />)
        ) : (
          <>
            <StatCard label="Total connections" value={summary.data?.totalConnections ?? 0} />
            <StatCard label="Processes" value={summary.data?.distinctProcesses ?? 0} />
            <StatCard label="Endpoints" value={summary.data?.distinctEndpoints ?? 0} />
            <StatCard label="Mean score" value={(summary.data?.meanScore ?? 0).toFixed(1)} />
          </>
        )}
      </div>

      <div className="grid gap-6 xl:grid-cols-[1fr_380px]">
        <Card>
          <CardHeader>
            <CardTitle>Score History</CardTitle>
          </CardHeader>
          <CardContent>
            {timeline.error ? <Alert variant="destructive">{timeline.error.message}</Alert> : null}
            {timeline.isLoading ? (
              <Skeleton className="h-72 w-full" />
            ) : timelineData.length === 0 ? (
              <div className="flex h-72 items-center justify-center rounded-md border border-dashed border-slate-800 bg-slate-950 text-sm text-slate-400">
                Score timeline will populate as connection scores are computed.
              </div>
            ) : (
              <div className="h-72">
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={timelineData} margin={{ left: 8, right: 16, top: 16, bottom: 8 }}>
                    <XAxis
                      dataKey="time"
                      tick={{ fill: '#94a3b8', fontSize: 12, fontFamily: 'JetBrains Mono' }}
                      axisLine={{ stroke: '#334155' }}
                      tickLine={{ stroke: '#334155' }}
                    />
                    <YAxis
                      domain={[0, 100]}
                      tick={{ fill: '#94a3b8', fontSize: 12, fontFamily: 'JetBrains Mono' }}
                      axisLine={{ stroke: '#334155' }}
                      tickLine={{ stroke: '#334155' }}
                    />
                    <Tooltip
                      contentStyle={{
                        background: '#020617',
                        border: '1px solid #1e293b',
                        borderRadius: 8,
                        color: '#f1f5f9'
                      }}
                      labelStyle={{ color: '#f59e0b' }}
                    />
                    <Line type="monotone" dataKey="score" stroke="#f59e0b" strokeWidth={2} dot={false} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle>Highest Scoring Connections</CardTitle>
            <Link className="inline-flex items-center gap-1 text-sm text-amber-500 hover:text-amber-400" to="/connections">
              View all
              <ArrowUpRight className="h-4 w-4" />
            </Link>
          </CardHeader>
          <CardContent className="space-y-3">
            {connections.isLoading ? (
              Array.from({ length: 5 }).map((_, index) => <Skeleton key={index} className="h-14" />)
            ) : topConnections.length === 0 ? (
              <div className="rounded-md border border-slate-800 bg-slate-950 p-4 text-sm text-slate-400">
                No scored connections yet.
              </div>
            ) : (
              topConnections.map((connection) => (
                <div key={connection.id} className="flex items-center justify-between gap-4 rounded-md bg-slate-950 p-3">
                  <div className="min-w-0">
                    <div className="truncate text-sm font-medium text-slate-100">
                      {connection.process?.name ?? 'Unknown process'}
                    </div>
                    <div className="truncate font-mono text-sm text-slate-500">
                      {connection.remoteIp ?? 'listening'}
                      {connection.remotePort ? `:${connection.remotePort}` : ''}
                    </div>
                  </div>
                  <ScoreBadge score={connection.latestScore} />
                </div>
              ))
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Recent Connections</CardTitle>
        </CardHeader>
        <CardContent>
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
