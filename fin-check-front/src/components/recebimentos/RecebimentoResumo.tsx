import { RecebimentoResumo as TRecebimentoResumo } from '@/lib/types/entities';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function RecebimentoResumo({ data }: { data: TRecebimentoResumo }) {
  return (
    <div className="grid grid-cols-2 gap-4">
      <Card>
        <CardHeader className="pb-2"><CardTitle className="text-sm font-medium text-muted-foreground">Total Recebido</CardTitle></CardHeader>
        <CardContent><p className="text-2xl font-bold text-green-600">R$ {data.totalRecebido?.toFixed(2)}</p></CardContent>
      </Card>
      <Card className="border-orange-200">
        <CardHeader className="pb-2"><CardTitle className="text-sm font-medium text-orange-600">Total Descontado em Taxas</CardTitle></CardHeader>
        <CardContent><p className="text-2xl font-bold text-orange-600">R$ {data.totalDescontado?.toFixed(2)}</p></CardContent>
      </Card>
    </div>
  );
}
