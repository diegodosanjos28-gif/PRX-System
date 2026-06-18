'use client';
import { useState } from 'react';
import Link from 'next/link';
import { useImplantacoes } from '@/lib/hooks/useImplantacoes';
import { ImplantacaoKpiBar } from '@/components/implantacoes/ImplantacaoKpiBar';
import { ImplantacaoAttentionPanel } from '@/components/implantacoes/ImplantacaoAttentionPanel';
import { ImplantacaoDerbyTrack } from '@/components/implantacoes/ImplantacaoDerbyTrack';
import { ImplantacaoCompactTable } from '@/components/implantacoes/ImplantacaoCompactTable';
import { ImplantacaoCurral } from '@/components/implantacoes/ImplantacaoCurral';
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
          {/* KPIs e Atenção: sempre dados totais */}
          <ImplantacaoKpiBar implantacoes={allData} />
          <ImplantacaoAttentionPanel implantacoes={allData} />

          {/* Filtros gerais */}
          <ImplantacaoFiltros
            implantacoes={allData}
            active={filtro}
            onChange={setFiltro}
          />

          {!allData.length ? (
            <EmptyState />
          ) : filteredData.length === 0 ? (
            <p className="py-6 text-center text-sm text-muted-foreground">
              Nenhuma implantação neste filtro.
            </p>
          ) : (
            <>
              {/* 1. Derby Track — pista, cavalos em andamento, faixa de chegada */}
              <ImplantacaoDerbyTrack implantacoes={filteredData} />

              {/* 2. Resumo compacto da implantação */}
              <ImplantacaoCompactTable implantacoes={filteredData} />

              {/* 3. Legenda de status */}
              <div style={{
                display: 'flex', gap: 18, flexWrap: 'wrap',
                padding: '14px 18px',
                background: '#fff',
                border: '1px solid #E6E9EC',
                borderRadius: 16,
              }}>
                {[
                  { color: '#00A19B', label: 'Fluindo no prazo' },
                  { color: '#E8A100', label: 'Atenção / aguardando terceiro' },
                  { color: '#D9534F', label: 'Travado / crítico' },
                ].map(({ color, label }) => (
                  <div key={label} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12, fontWeight: 600, color: '#6B7178' }}>
                    <span style={{ width: 11, height: 11, borderRadius: 4, background: color, display: 'inline-block', flexShrink: 0 }} />
                    {label}
                  </div>
                ))}
                {[
                  { em: '🥩', label: 'Box de largada' },
                  { em: '🏁', label: 'Onboarding concluído' },
                ].map(({ em, label }) => (
                  <div key={label} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12, fontWeight: 600, color: '#6B7178' }}>
                    <span style={{ fontSize: 16 }}>{em}</span>
                    {label}
                  </div>
                ))}
              </div>

              {/* 4. Currais Operacionais — seção header + filtros + fazenda */}
              <ImplantacaoCurral implantacoes={filteredData} />
            </>
          )}
        </>
      )}
    </div>
  );
}
