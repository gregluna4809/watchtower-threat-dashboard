import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';

export interface MetadataItem {
  label: string;
  value: string | number | null | undefined;
}

interface MetadataCardProps {
  title: string;
  items: MetadataItem[];
}

export function MetadataCard({ title, items }: MetadataCardProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {items.map((item) => (
            <div key={item.label} className="min-w-0">
              <div className="text-xs uppercase tracking-normal text-slate-500">{item.label}</div>
              <div className="mt-1 break-words font-mono text-sm text-slate-100">{item.value ?? '--'}</div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
