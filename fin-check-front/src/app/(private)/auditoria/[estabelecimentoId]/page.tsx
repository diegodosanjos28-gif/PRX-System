'use client';
import { useState } from 'react';
import { useParams } from 'next/navigation';
import { useAuditoria } from '@/lib/hooks/useAuditoria';
import { DateRangePicker } from '@/components/shared/DateRangePicker';
import { AuditoriaResumo } from '@/components/auditoria/AuditoriaResumo';
import { AuditoriaPorBandeira } from '@/components/auditoria/AuditoriaPorBandeira';
import { LoadingSpinner } from '@/components/shared/LoadingSpinner';
import { EmptyState } from '@/components/shared/EmptyState';

export default function AuditoriaPage() {
  const { estabelecimentoId } = useParams<{ estabelecimentoId: string }>();
  const [range, setRange] = useState({ dataInicio: '', dataFim: '' });
  const { data, isLoading } = useAuditoria(estabelecimentoId, range.dataInicio, range.dataFim);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Auditoria de Taxas</h1>
      <DateRangePicker dataInicio={range.dataInicio} dataFim={range.dataFim} onChange={setRange} />
      {isLoading && <LoadingSpinner />}
      {data && <AuditoriaResumo data={data} />}
      {data?.porBandeira?.length ? <AuditoriaPorBandeira data={data.porBandeira} /> : !isLoading && range.dataInicio && <EmptyState message="Nenhuma transação encontrada no período" />}
    </div>
  );
}
