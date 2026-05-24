import { useRules, useToggleRule } from '../hooks/useRules';
import { Alert } from '../components/ui/alert';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Skeleton } from '../components/ui/skeleton';
import { Switch } from '../components/ui/switch';

export function RulesPage() {
  const rules = useRules();
  const toggleRule = useToggleRule();

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-semibold text-slate-100">Rules</h2>
        <p className="mt-1 text-sm text-slate-400">Toggle scoring rules that affect future advisory scores.</p>
      </div>

      {rules.error ? <Alert variant="destructive">{rules.error.message}</Alert> : null}
      {toggleRule.error ? <Alert variant="destructive">{toggleRule.error.message}</Alert> : null}

      <Card>
        <CardHeader>
          <CardTitle>Rule Definitions</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {rules.isLoading
            ? Array.from({ length: 7 }).map((_, index) => <Skeleton key={index} className="h-24" />)
            : rules.data?.map((rule) => (
                <div
                  key={rule.code}
                  className="flex items-center justify-between gap-6 rounded-md border border-slate-800 bg-slate-950 p-4"
                >
                  <div className="min-w-0 space-y-1">
                    <div className="flex flex-wrap items-center gap-3">
                      <div className="font-medium text-slate-100">{rule.displayName}</div>
                      <div className="font-mono text-xs text-slate-500">{rule.code}</div>
                      <div className="font-mono text-xs text-amber-500">{rule.defaultPoints} pts</div>
                    </div>
                    <div className="text-sm text-slate-400">{rule.description}</div>
                  </div>
                  <Switch
                    checked={rule.enabled}
                    disabled={toggleRule.isPending}
                    aria-label={`Toggle ${rule.displayName}`}
                    onClick={() => toggleRule.mutate({ code: rule.code, enabled: !rule.enabled })}
                  />
                </div>
              ))}
        </CardContent>
      </Card>
    </div>
  );
}
