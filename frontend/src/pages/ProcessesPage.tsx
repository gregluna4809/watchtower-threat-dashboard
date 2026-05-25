import { Link } from 'react-router-dom';
import { ProcessCard } from '../components/ProcessCard';
import { useProcesses } from '../hooks/useProcesses';
import { Alert } from '../components/ui/alert';
import { Card, CardContent } from '../components/ui/card';
import { Skeleton } from '../components/ui/skeleton';

export function ProcessesPage() {
  const processes = useProcesses();
  const sorted = [...(processes.data?.items ?? [])].sort((a, b) => (b.maxScore ?? 0) - (a.maxScore ?? 0));

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-semibold text-slate-100">Processes</h2>
        <p className="mt-1 text-sm text-slate-400">Observed processes with connection counts and maximum score.</p>
      </div>

      {processes.error ? <Alert variant="destructive">{processes.error.message}</Alert> : null}

      {processes.isLoading ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 9 }).map((_, index) => (
            <Skeleton key={index} className="h-72" />
          ))}
        </div>
      ) : sorted.length === 0 ? (
        <Card className="mx-auto max-w-xl text-center">
          <CardContent className="p-6">
            <div className="text-sm font-medium text-slate-100">No processes have been observed.</div>
            <div className="mt-2 text-sm text-slate-400">Start the backend and let the ingest loop run.</div>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {sorted.map((process) => (
            <Link key={process.id} to={`/processes/${process.id}`} className="block hover:opacity-95">
              <ProcessCard process={process} />
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
