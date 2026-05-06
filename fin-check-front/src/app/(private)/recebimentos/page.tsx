'use client';
import { useState } from 'react';
import { useRecebimentos } from '@/lib/hooks/useRecebimentos';
import { EstabelecimentoSelector } from '@/components/shared/EstabelecimentoSelector';
import { DateRangePicker } from '@/components/shared/DateRangePicker';
import { RecebimentoResumo } from '@/components/recebimentos/RecebimentoResumo';
import { RecebimentoPorBandeira } from '@/components/recebimentos/RecebimentoPorBandeira';
import { RecebimentoGrid } from '@/components/recebimentos/RecebimentoGrid';
import { ViewToggle, ViewMode } from '@/components/shared/ViewToggle';
import { LoadingSpinner } from '@/components/shared/LoadingSpinner';
import { EmptyState } from '@/components/shared/EmptyState';

export default function RecebimentosPage() {
  const [estabelecimentoId, setEstabelecimentoId] = useState('');
  const [range, setRange] = useState({ dataInicio: '', dataFim: '' });
  const [view, setView] = useState<ViewMode>('grid');

  const { data, isLoading } = useRecebimentos(estabelecimentoId, range.dataInicio, range.dataFim);

  const hasRecords = (data?.recebimentos?.length ?? 0) > 0;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Recebimentos</h1>
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
        <EmptyState message="Selecione um estabelecimento para visualizar os recebimentos" />
      ) : isLoading ? (
        <LoadingSpinner />
      ) : data ? (
        <>
          <RecebimentoResumo data={data} />
          {!hasRecords ? (
            range.dataInicio && <EmptyState message="Nenhum recebimento encontrado no período" />
          ) : view === 'grid' ? (
            <RecebimentoGrid data={data.recebimentos} />
          ) : (
            data.porBandeira?.length > 0 && <RecebimentoPorBandeira data={data.porBandeira} />
          )}
        </>
      ) : (
        range.dataInicio && <EmptyState message="Nenhum recebimento encontrado no período" />
      )}
    </div>
  );
}
