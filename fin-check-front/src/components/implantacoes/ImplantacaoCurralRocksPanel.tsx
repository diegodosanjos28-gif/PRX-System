'use client';

import { Trophy, CheckCircle2 } from 'lucide-react';
import { useRocksConcluidos } from '@/lib/hooks/useImplantacoes';
import { ImplantacaoRockConcluido, DemandaPrioridade } from '@/lib/types/entities';

// ── helpers ───────────────────────────────────────────────────────────────────

function dateKey(iso: string): string {
  return iso.split('T')[0]; // "2026-06-05"
}

function formatDate(iso: string): string {
  const [y, m, d] = iso.split('T')[0].split('-');
  return `${d}/${m}/${y}`;
}

const PRIO_STYLE: Record<DemandaPrioridade, { label: string; cls: string }> = {
  baixa:  { label: 'Baixa',   cls: 'bg-gray-100 text-gray-500' },
  media:  { label: 'Média',   cls: 'bg-blue-50 text-blue-600'  },
  alta:   { label: 'Alta',    cls: 'bg-orange-50 text-orange-600' },
  critica:{ label: 'Crítica', cls: 'bg-red-50 text-red-600'    },
};

// ── sub-components ────────────────────────────────────────────────────────────

function EmptyRocks() {
  return (
    <div className="flex flex-col items-center justify-center py-8 gap-2 text-muted-foreground">
      <CheckCircle2 className="h-8 w-8 opacity-20" />
      <p className="text-sm font-medium">Nenhum ROCK concluído ainda.</p>
      <p className="text-xs opacity-60">Assim que uma demanda for concluída, ela aparecerá aqui.</p>
    </div>
  );
}

interface RockItemProps {
  rock: ImplantacaoRockConcluido;
}

function RockItem({ rock }: RockItemProps) {
  const prio = PRIO_STYLE[rock.prioridade] ?? { label: rock.prioridade, cls: 'bg-gray-100 text-gray-500' };
  const clienteNome = rock.clienteNomeFantasia ?? rock.clienteRazaoSocial;

  return (
    <div className="flex items-start gap-3 py-3 border-b border-dashed border-gray-100 last:border-0">
      <CheckCircle2 className="h-4 w-4 mt-0.5 text-emerald-500 flex-shrink-0" />
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-gray-800 truncate">{clienteNome}</p>
        <p className="text-sm text-gray-600 mt-0.5">{rock.descricao}</p>
        <div className="flex items-center gap-2 mt-1.5 flex-wrap">
          <span className={`text-[10px] font-700 px-2 py-0.5 rounded-full ${prio.cls}`}>
            {prio.label}
          </span>
          {rock.adquirente && (
            <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-gray-100 text-gray-500">
              {rock.adquirente}
            </span>
          )}
        </div>
      </div>
    </div>
  );
}

// ── main component ────────────────────────────────────────────────────────────

export function ImplantacaoCurralRocksPanel() {
  const { data: rocks, isLoading } = useRocksConcluidos();

  if (isLoading) return null;

  const allRocks = rocks ?? [];

  // Encontra a data mais recente de conclusão (por dia calendário)
  const latestDate = allRocks.reduce<string | null>((max, r) => {
    const d = dateKey(r.concluidaEm);
    return max === null || d > max ? d : max;
  }, null);

  const rocksToShow = latestDate
    ? allRocks.filter((r) => dateKey(r.concluidaEm) === latestDate)
    : [];

  return (
    <div
      style={{
        background: '#fff',
        border: '1px solid #E6E9EC',
        borderRadius: 16,
        padding: '20px 24px 16px',
        boxShadow: '0 1px 4px rgba(0,0,0,.06)',
      }}
    >
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
        <Trophy
          style={{ width: 18, height: 18, color: '#E8A100', flexShrink: 0 }}
          strokeWidth={2.2}
        />
        <span style={{ fontSize: 13, fontWeight: 800, color: '#1C2024', letterSpacing: '.3px' }}>
          ROCKs Concluídos
        </span>
        {latestDate && (
          <span
            style={{
              fontSize: 11, fontWeight: 700, color: '#207A4F',
              background: '#E6F5EE', padding: '3px 10px', borderRadius: 999,
              marginLeft: 2,
            }}
          >
            {formatDate(latestDate)}
          </span>
        )}
        {rocksToShow.length > 0 && (
          <span
            style={{
              marginLeft: 'auto', fontSize: 11, fontWeight: 700,
              color: '#6B7178',
            }}
          >
            {rocksToShow.length} {rocksToShow.length === 1 ? 'demanda' : 'demandas'}
          </span>
        )}
      </div>

      {/* Subtitle */}
      {latestDate && (
        <p style={{ fontSize: 11, color: '#9BA3AE', marginBottom: 12, marginLeft: 28 }}>
          Demandas concluídas na última data com registro
        </p>
      )}

      {/* Content */}
      {rocksToShow.length === 0 ? (
        <EmptyRocks />
      ) : (
        <div style={{ marginLeft: 4 }}>
          {rocksToShow.map((r) => (
            <RockItem key={r.demandaId} rock={r} />
          ))}
        </div>
      )}
    </div>
  );
}
