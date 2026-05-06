'use client';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';

interface Props {
  dataInicio: string;
  dataFim: string;
  onChange: (range: { dataInicio: string; dataFim: string }) => void;
}

export function DateRangePicker({ dataInicio, dataFim, onChange }: Props) {
  return (
    <div className="flex gap-4 items-end">
      <div className="space-y-1">
        <Label>De</Label>
        <Input
          type="date"
          value={dataInicio}
          onChange={(e) => onChange({ dataInicio: e.target.value, dataFim })}
        />
      </div>
      <div className="space-y-1">
        <Label>Até</Label>
        <Input
          type="date"
          value={dataFim}
          onChange={(e) => onChange({ dataInicio, dataFim: e.target.value })}
        />
      </div>
    </div>
  );
}
