import { AuditoriaResumo as TAuditoriaResumo } from '@/lib/types/entities';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function AuditoriaResumo({ data }: { data: TAuditoriaResumo }) {
  return (
    <div className="grid grid-cols-3 gap-4">
      <Card>
        <CardHeader className="pb-2"><CardTitle className="text-sm font-medium text-muted-foreground">Total de Transações</CardTitle></CardHeader>
        <CardContent><p className="text-2xl font-bold">{data.totalTransacoes}</p></CardContent>
      </Card>
      <Card className="border-red-200">
        <CardHeader className="pb-2"><CardTitle className="text-sm font-medium text-red-600">Cobrado a Mais</CardTitle></CardHeader>
        <CardContent><p className="text-2xl font-bold text-red-600">R$ {data.totalCobradoAMais?.toFixed(2)}</p></CardContent>
      </Card>
      <Card className="border-yellow-200">
        <CardHeader className="pb-2"><CardTitle className="text-sm font-medium text-yellow-600">Cobrado a Menos</CardTitle></CardHeader>
        <CardContent><p className="text-2xl font-bold text-yellow-600">R$ {data.totalCobradoAMenos?.toFixed(2)}</p></CardContent>
      </Card>
    </div>
  );
}
