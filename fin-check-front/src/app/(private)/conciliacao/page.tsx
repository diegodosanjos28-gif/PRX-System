'use client';
import { useState } from 'react';
import { useAuditoria } from '@/lib/hooks/useAuditoria';
import { EstabelecimentoSelector } from '@/components/shared/EstabelecimentoSelector';
import { DateRangePicker } from '@/components/shared/DateRangePicker';
import { AuditoriaResumo } from '@/components/auditoria/AuditoriaResumo';
import { AuditoriaPorBandeira } from '@/components/auditoria/AuditoriaPorBandeira';
import { ConciliacaoTaxaGrid } from '@/components/conciliacao/ConciliacaoTaxaGrid';
import { ViewToggle, ViewMode } from '@/components/shared/ViewToggle';
import { LoadingSpinner } from '@/components/shared/LoadingSpinner';
import { EmptyState } from '@/components/shared/EmptyState';

export default function ConciliacaoPage() {
  const [estabelecimentoId, setEstabelecimentoId] = useState('');
  const [range, setRange] = useState({ dataInicio: '', dataFim: '' });
  const [view, setView] = useState<ViewMode>('grid');

  const { data, isLoading } = useAuditoria(estabelecimentoId, range.dataInicio, range.dataFim);

  const hasRecords = (data?.conciliacaoTaxaResponse?.length ?? 0) > 0;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Conciliação de Taxas</h1>
        {data && hasRecords && <ViewToggle view={view} onChange={setView} />}
      </div>

      <div className="rounded-lg border bg-card p-4 space-y-4">
        <EstabelecimentoSelector onSelect={setEstabelecimentoId} />
        {estabelecimentoId && (
          <DateRangePicker
            dataInicio={range.dataInicio}
            dataFim={range.dataFim}
            onChange={setRange}
          />
        )}
      </div>

      {!estabelecimentoId ? (
        <EmptyState message="Selecione um estabelecimento para visualizar a conciliação de taxas" />
      ) : isLoading ? (
        <LoadingSpinner />
      ) : data ? (
        <>
          <AuditoriaResumo data={data} />
          {!hasRecords ? (
            range.dataInicio && <EmptyState message="Nenhuma transação encontrada no período" />
          ) : view === 'grid' ? (
            <ConciliacaoTaxaGrid data={data.conciliacaoTaxaResponse} />
          ) : (
            data.porBandeira?.length > 0 && <AuditoriaPorBandeira data={data.porBandeira} />
          )}
        </>
      ) : (
        range.dataInicio && <EmptyState message="Nenhuma transação encontrada no período" />
      )}
    </div>
  );
}
