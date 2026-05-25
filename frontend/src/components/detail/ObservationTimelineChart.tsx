import { format } from 'date-fns';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { ObservationBucket } from '../../lib/types';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Skeleton } from '../ui/skeleton';

interface ObservationTimelineChartProps {
  buckets: ObservationBucket[] | undefined;
  isLoading: boolean;
}

export function ObservationTimelineChart({ buckets, isLoading }: ObservationTimelineChartProps) {
  if (isLoading) {
    return <Skeleton className="h-[200px] w-full" />;
  }

  const data = (buckets ?? []).map((bucket) => ({
    ...bucket,
    label: format(new Date(bucket.bucketStart), 'MM-dd HH:mm')
  }));

  return (
    <Card>
      <CardHeader>
        <CardTitle>Observation Timeline</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="h-[200px]">
          {data.length === 0 ? (
            <div className="flex h-full items-center justify-center rounded-md border border-dashed border-slate-800 text-sm text-slate-400">
              No observations in this window.
            </div>
          ) : (
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={data}>
                <CartesianGrid stroke="#1e293b" vertical={false} />
                <XAxis dataKey="label" tick={{ fill: '#94a3b8', fontSize: 12 }} tickLine={false} axisLine={false} />
                <YAxis tick={{ fill: '#94a3b8', fontSize: 12 }} tickLine={false} axisLine={false} width={40} />
                <Tooltip
                  cursor={{ fill: '#334155', opacity: 0.25 }}
                  contentStyle={{ background: '#020617', border: '1px solid #1e293b', borderRadius: 6 }}
                  labelStyle={{ color: '#e2e8f0' }}
                />
                <Bar dataKey="count" fill="#f59e0b" radius={[3, 3, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
