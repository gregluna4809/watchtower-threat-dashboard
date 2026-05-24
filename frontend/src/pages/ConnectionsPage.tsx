import { useSearchParams } from 'react-router-dom';
import { ConnectionTable } from '../components/ConnectionTable';
import { useConnections } from '../hooks/useConnections';
import { useLiveConnections } from '../hooks/useLiveConnections';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Select } from '../components/ui/select';

const states = [
  '',
  'ESTABLISHED',
  'LISTENING',
  'TIME_WAIT',
  'CLOSE_WAIT',
  'SYN_SENT',
  'SYN_RECV',
  'FIN_WAIT_1',
  'FIN_WAIT_2',
  'LAST_ACK',
  'CLOSED',
  'UNKNOWN'
];

export function ConnectionsPage() {
  useLiveConnections();
  const [searchParams, setSearchParams] = useSearchParams();
  const limit = Number(searchParams.get('limit') ?? 100);
  const offset = Number(searchParams.get('offset') ?? 0);
  const state = searchParams.get('state') ?? '';
  const country = searchParams.get('country') ?? '';
  const processName = searchParams.get('processName') ?? '';
  const minScoreText = searchParams.get('minScore') ?? '';
  const minScore = minScoreText === '' ? undefined : Number(minScoreText);

  const connections = useConnections({
    limit,
    offset,
    state: state || undefined,
    country: country || undefined,
    processName: processName || undefined,
    minScore
  });

  function updateParam(key: string, value: string) {
    const next = new URLSearchParams(searchParams);
    if (value) {
      next.set(key, value);
    } else {
      next.delete(key);
    }
    next.set('offset', '0');
    setSearchParams(next);
  }

  function page(delta: number) {
    const next = new URLSearchParams(searchParams);
    next.set('limit', String(limit));
    next.set('offset', String(Math.max(0, offset + delta * limit)));
    setSearchParams(next);
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-semibold text-slate-100">Connections</h2>
        <p className="mt-1 text-sm text-slate-400">Live connection rows with latest advisory score.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Filters</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
          <Select value={state} onChange={(event) => updateParam('state', event.target.value)}>
            {states.map((option) => (
              <option key={option} value={option}>
                {option || 'Any state'}
              </option>
            ))}
          </Select>
          <Input
            value={country}
            maxLength={2}
            placeholder="Country"
            onChange={(event) => updateParam('country', event.target.value.toUpperCase())}
          />
          <Input
            value={minScoreText}
            min={0}
            max={100}
            type="number"
            placeholder="Minimum score"
            onChange={(event) => updateParam('minScore', event.target.value)}
          />
          <Input
            value={processName}
            placeholder="Process name"
            onChange={(event) => updateParam('processName', event.target.value)}
          />
          <Button
            variant="outline"
            onClick={() => {
              setSearchParams(new URLSearchParams());
            }}
          >
            Clear Filters
          </Button>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="p-6">
          <ConnectionTable
            connections={connections.data?.items}
            isLoading={connections.isLoading}
            error={connections.error}
          />
          <div className="mt-4 flex items-center justify-between gap-4">
            <div className="font-mono text-sm text-slate-500">
              {connections.data ? `${connections.data.offset + 1}-${connections.data.offset + connections.data.items.length} / ${connections.data.total}` : '--'}
            </div>
            <div className="flex gap-2">
              <Button variant="outline" disabled={offset === 0} onClick={() => page(-1)}>
                Previous
              </Button>
              <Button
                variant="outline"
                disabled={!connections.data || offset + limit >= connections.data.total}
                onClick={() => page(1)}
              >
                Next
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
