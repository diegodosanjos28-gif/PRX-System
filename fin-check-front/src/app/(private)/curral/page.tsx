'use client';
import { useImplantacoes } from '@/lib/hooks/useImplantacoes';
import { ImplantacaoCurral } from '@/components/implantacoes/ImplantacaoCurral';
import { ImplantacaoCurralRocksPanel } from '@/components/implantacoes/ImplantacaoCurralRocksPanel';
import { LoadingSpinner } from '@/components/shared/LoadingSpinner';
import { EmptyState } from '@/components/shared/EmptyState';

export default function CurralPage() {
  const { data, isLoading } = useImplantacoes();
  const allData = data ?? [];
  const curralData = allData.filter((i) => i.etapa === 'curral');

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold">Curral</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Clientes pós-onboarding — acompanhamento operacional contínuo.
        </p>
      </div>

      {/* Painel ROCKs Concluídos — carrega independentemente via hook próprio */}
      <ImplantacaoCurralRocksPanel />

      {isLoading ? (
        <LoadingSpinner />
      ) : curralData.length === 0 ? (
        <EmptyState />
      ) : (
        <ImplantacaoCurral implantacoes={allData} />
      )}
    </div>
  );
}
