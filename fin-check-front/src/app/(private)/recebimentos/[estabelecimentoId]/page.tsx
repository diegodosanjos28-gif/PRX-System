'use client';
import { useState } from 'react';
import { useParams } from 'next/navigation';
import { useRecebimentos } from '@/lib/hooks/useRecebimentos';
import { DateRangePicker } from '@/components/shared/DateRangePicker';
import { RecebimentoResumo } from '@/components/recebimentos/RecebimentoResumo';
import { RecebimentoPorBandeira } from '@/components/recebimentos/RecebimentoPorBandeira';
import { LoadingSpinner } from '@/components/shared/LoadingSpinner';
import { EmptyState } from '@/components/shared/EmptyState';

export default function RecebimentosPage() {
  const { estabelecimentoId } = useParams<{ estabelecimentoId: string }>();
  const [range, setRange] = useState({ dataInicio: '', dataFim: '' });
  const { data, isLoading } = useRecebimentos(estabelecimentoId, range.dataInicio, range.dataFim);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Recebimentos</h1>
      <DateRangePicker dataInicio={range.dataInicio} dataFim={range.dataFim} onChange={setRange} />
      {isLoading && <LoadingSpinner />}
      {data && <RecebimentoResumo data={data} />}
      {data?.porBandeira?.length ? <RecebimentoPorBandeira data={data.porBandeira} /> : !isLoading && range.dataInicio && <EmptyState message="Nenhum recebimento encontrado no período" />}
    </div>
  );
}
