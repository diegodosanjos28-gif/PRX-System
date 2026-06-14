'use client';
import { useState } from 'react';
import Link from 'next/link';
import { useImplantacoes } from '@/lib/hooks/useImplantacoes';
import { ImplantacaoKpiBar } from '@/components/implantacoes/ImplantacaoKpiBar';
import { ImplantacaoAttentionPanel } from '@/components/implantacoes/ImplantacaoAttentionPanel';
import { ImplantacaoDerbyTrack } from '@/components/implantacoes/ImplantacaoDerbyTrack';
import { ImplantacaoPipeline } from '@/components/implantacoes/ImplantacaoPipeline';
import { ImplantacaoTable } from '@/components/implantacoes/ImplantacaoTable';
import {
  ImplantacaoFiltros,
  applyFiltro,
  type ImplantacaoFiltroKey,
} from '@/components/implantacoes/ImplantacaoFiltros';
import { LoadingSpinner } from '@/components/shared/LoadingSpinner';
import { EmptyState } from '@/components/shared/EmptyState';
import { Button } from '@/components/ui/button';

export default function ImplantacoesPage() {
  const { data, isLoading } = useImplantacoes();
  const [filtro, setFiltro] = useState<ImplantacaoFiltroKey>('todos');

  const allData      = data ?? [];
  const filteredData = applyFiltro(allData, filtro);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Implantações</h1>
        <Button asChild>
          <Link href="/implantacoes/nova">Nova Implantação</Link>
        </Button>
      </div>

      {isLoading ? (
        <LoadingSpinner />
      ) : (
        <>
          {/* KPIs e Atenção: sempre com dados totais (não filtrados) */}
          <ImplantacaoKpiBar implantacoes={allData} />
          <ImplantacaoAttentionPanel implantacoes={allData} />

          {/* Filtros rápidos */}
          <ImplantacaoFiltros
            implantacoes={allData}
            active={filtro}
            onChange={setFiltro}
          />

          {/* Pista Derby, Pipeline e Tabela: com dados filtrados */}
          <ImplantacaoDerbyTrack implantacoes={filteredData} />
          <ImplantacaoPipeline implantacoes={filteredData} />

          {!allData.length ? (
            <EmptyState />
          ) : filteredData.length === 0 ? (
            <p className="py-6 text-center text-sm text-muted-foreground">
              Nenhuma implantação neste filtro.
            </p>
          ) : (
            <ImplantacaoTable implantacoes={filteredData} />
          )}
        </>
      )}
    </div>
  );
}
