'use client';

import Link from 'next/link';
import { Flag, ChevronRight } from 'lucide-react';
import { ImplantacaoCliente } from '@/lib/types/entities';
import { cn } from '@/lib/utils';

// ─── Configuração de etapas ───────────────────────────────────────────────────

const STAGES = [
  { key: 'pre',        label: 'Pré-Largada', emoji: '🚦' },
  { key: 'corrida',    label: 'Corrida',      emoji: '🏇' },
  { key: 'onboarding', label: 'Onboarding',   emoji: '📋' },
  { key: 'curral',     label: 'Curral',       emoji: '🏁' },
] as const;

type StageKey = typeof STAGES[number]['key'];

// ─── Cores por status ─────────────────────────────────────────────────────────

function markerCfg(status: string | null | undefined, etapa: string) {
  if (etapa === 'curral') {
    return {
      pill:    'bg-gray-100 text-gray-600 border-gray-300 hover:bg-gray-200',
      dot:     'bg-gray-400',
      nodeRing:'border-gray-300 bg-gray-50',
    };
  }
  switch (status) {
    case 'fluindo':
      return {
        pill:    'bg-green-50 text-green-800 border-green-200 hover:bg-green-100',
        dot:     'bg-green-500',
        nodeRing:'border-green-400 bg-green-50',
      };
    case 'aguardando':
      return {
        pill:    'bg-yellow-50 text-yellow-800 border-yellow-300 hover:bg-yellow-100',
        dot:     'bg-yellow-400',
        nodeRing:'border-yellow-400 bg-yellow-50',
      };
    case 'travado':
      return {
        pill:    'bg-red-50 text-red-800 border-red-300 hover:bg-red-100',
        dot:     'bg-red-500',
        nodeRing:'border-red-400 bg-red-50',
      };
    default:
      return {
        pill:    'bg-gray-50 text-gray-600 border-gray-200 hover:bg-gray-100',
        dot:     'bg-gray-400',
        nodeRing:'border-gray-300 bg-gray-50',
      };
  }
}

// ─── Legenda de status ────────────────────────────────────────────────────────

const LEGEND = [
  { label: 'Fluindo',    dot: 'bg-green-500'  },
  { label: 'Aguardando', dot: 'bg-yellow-400' },
  { label: 'Travado',    dot: 'bg-red-500'    },
  { label: 'Curral',     dot: 'bg-gray-400'   },
];

// ─── Componente ───────────────────────────────────────────────────────────────

interface Props {
  implantacoes: ImplantacaoCliente[];
}

export function ImplantacaoDerbyTrack({ implantacoes }: Props) {
  const grouped = Object.fromEntries(
    STAGES.map(({ key }) => [key, implantacoes.filter((i) => i.etapa === key)]),
  ) as Record<StageKey, ImplantacaoCliente[]>;

  return (
    <div className="anim-fade-in-up rounded-lg border bg-white shadow-sm" style={{ animationDelay: '140ms' }}>
      {/* Cabeçalho */}
      <div className="flex items-center gap-2 border-b px-4 py-2.5">
        <Flag className="h-4 w-4 text-orange-500 flex-shrink-0" />
        <span className="text-sm font-semibold text-gray-800">Pista Derby</span>

        {/* Legenda */}
        <div className="ml-auto flex items-center gap-3">
          {LEGEND.map(({ label, dot }) => (
            <span key={label} className="hidden sm:flex items-center gap-1 text-xs text-muted-foreground">
              <span className={cn('h-2 w-2 rounded-full flex-shrink-0', dot)} />
              {label}
            </span>
          ))}
          <span className="text-xs text-muted-foreground">
            {implantacoes.length} total
          </span>
        </div>
      </div>

      {/* Pista — scroll horizontal em mobile */}
      <div className="overflow-x-auto">
        <div className="min-w-[580px] px-6 pt-6 pb-5">

          {/* Trilho de progresso com nós de etapa */}
          <div className="relative mb-5">
            {/* Linha do trilho */}
            <div className="absolute top-[18px] left-[12.5%] right-[12.5%] h-0.5 bg-gray-200" />

            {/* Nós das etapas */}
            <div className="relative grid grid-cols-4">
              {STAGES.map(({ key, label, emoji }) => {
                const count = grouped[key]?.length ?? 0;
                const hasItems = count > 0;

                return (
                  <div key={key} className="flex flex-col items-center gap-1.5">
                    {/* Círculo do nó */}
                    <div
                      className={cn(
                        'relative z-10 flex h-9 w-9 items-center justify-center rounded-full border-2 text-sm',
                        'transition-colors',
                        hasItems
                          ? 'border-orange-400 bg-orange-400 shadow-sm'
                          : 'border-gray-200 bg-white',
                      )}
                    >
                      {hasItems ? (
                        <span className="text-base leading-none select-none" aria-hidden>
                          {emoji}
                        </span>
                      ) : (
                        <span className="h-2.5 w-2.5 rounded-full bg-gray-200" />
                      )}
                    </div>

                    {/* Label + contador */}
                    <div className="flex flex-col items-center gap-0.5">
                      <span className="text-xs font-semibold text-gray-700 text-center leading-tight">
                        {label}
                      </span>
                      {hasItems && (
                        <span className="rounded-full bg-orange-100 px-1.5 py-0.5 text-[10px] font-medium text-orange-700">
                          {count}
                        </span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Separador de progresso com seta */}
          <div className="mb-4 flex items-center gap-1 text-xs text-muted-foreground">
            <span>Início</span>
            <ChevronRight className="h-3 w-3" />
            <span>Progresso</span>
            <ChevronRight className="h-3 w-3" />
            <span>Chegada</span>
          </div>

          {/* Marcadores de implantação por etapa */}
          <div className="grid grid-cols-4 gap-x-3">
            {STAGES.map(({ key }) => {
              const items = grouped[key] ?? [];

              return (
                <div key={key} className="flex flex-col gap-1.5">
                  {items.length === 0 ? (
                    <div className="rounded border border-dashed border-gray-200 px-2 py-3 text-center text-xs text-muted-foreground">
                      —
                    </div>
                  ) : (
                    items.map((impl) => {
                      const cfg = markerCfg(impl.status, impl.etapa);
                      return (
                        <Link
                          key={impl.id}
                          href={`/implantacoes/${impl.id}`}
                          title={`${impl.clienteRazaoSocial}${impl.responsavel ? ` — ${impl.responsavel}` : ''}`}
                          className={cn(
                            'group flex items-center gap-1.5 rounded border px-2 py-1.5 text-xs',
                            'transition-all duration-100 hover:shadow-sm hover:-translate-y-px',
                            cfg.pill,
                          )}
                        >
                          {/* Cavalo */}
                          <span
                            className="flex-shrink-0 text-sm leading-none select-none"
                            aria-hidden
                          >
                            🏇
                          </span>

                          {/* Nome do cliente */}
                          <span className="min-w-0 flex-1 truncate font-medium leading-tight">
                            {impl.clienteRazaoSocial}
                          </span>

                          {/* Ponto de status */}
                          <span
                            className={cn(
                              'ml-auto h-2 w-2 flex-shrink-0 rounded-full',
                              cfg.dot,
                            )}
                          />
                        </Link>
                      );
                    })
                  )}
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
