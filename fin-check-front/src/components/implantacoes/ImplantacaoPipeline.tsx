'use client';

import Link from 'next/link';
import { User, AlertTriangle, Clock, Flag } from 'lucide-react';
import { ImplantacaoCliente } from '@/lib/types/entities';
import { cn } from '@/lib/utils';

// ─── Colunas ──────────────────────────────────────────────────────────────────

const STAGES = [
  { key: 'pre',        label: 'Pré-Largada',  headerAccent: 'border-t-blue-400',   colBg: 'bg-blue-50/30'   },
  { key: 'corrida',    label: 'Corrida',       headerAccent: 'border-t-orange-400', colBg: 'bg-orange-50/30' },
  { key: 'onboarding', label: 'Onboarding',    headerAccent: 'border-t-purple-400', colBg: 'bg-purple-50/30' },
  { key: 'curral',     label: 'Curral',        headerAccent: 'border-t-gray-400',   colBg: 'bg-gray-50/30'   },
] as const;

type StageKey = typeof STAGES[number]['key'];

// ─── Status ───────────────────────────────────────────────────────────────────

const STATUS_CONFIG: Record<
  string,
  { badge: string; cardBar: string; dot: string; label: string }
> = {
  fluindo: {
    badge:   'bg-green-500/10 text-green-700 border-green-200',
    cardBar: 'border-l-green-400',
    dot:     'bg-green-400',
    label:   'Fluindo',
  },
  aguardando: {
    badge:   'bg-yellow-500/10 text-yellow-700 border-yellow-200',
    cardBar: 'border-l-yellow-400',
    dot:     'bg-yellow-400',
    label:   'Aguardando',
  },
  travado: {
    badge:   'bg-red-500/10 text-red-700 border-red-200',
    cardBar: 'border-l-red-500',
    dot:     'bg-red-500',
    label:   'Travado',
  },
};

const ETAPA_LABELS: Record<string, string> = {
  pre:        'Pré-Largada',
  corrida:    'Corrida',
  onboarding: 'Onboarding',
  curral:     'Curral',
};

// ─── Componente ───────────────────────────────────────────────────────────────

interface Props {
  implantacoes: ImplantacaoCliente[];
}

export function ImplantacaoPipeline({ implantacoes }: Props) {
  const grouped = Object.fromEntries(
    STAGES.map(({ key }) => [key, implantacoes.filter((i) => i.etapa === key)]),
  ) as Record<StageKey, ImplantacaoCliente[]>;

  return (
    <div
      className="anim-fade-in-up grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4"
      style={{ animationDelay: '200ms' }}
    >
      {STAGES.map(({ key, label, headerAccent, colBg }) => {
        const items = grouped[key] ?? [];

        return (
          <div key={key} className="flex flex-col gap-2">
            {/* Cabeçalho da coluna */}
            <div
              className={cn(
                'rounded-lg border border-t-4 bg-white px-3 py-2 shadow-sm',
                headerAccent,
              )}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1.5">
                  <Flag className="h-3.5 w-3.5 text-muted-foreground" />
                  <span className="text-sm font-semibold text-gray-700">{label}</span>
                </div>
                <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-500">
                  {items.length}
                </span>
              </div>
            </div>

            {/* Área da coluna */}
            <div className={cn('flex flex-col gap-2 rounded-lg p-1.5 min-h-16', colBg)}>
              {items.length === 0 ? (
                <div className="rounded-lg border border-dashed bg-white/60 px-3 py-6 text-center text-xs text-muted-foreground">
                  Nenhuma aqui
                </div>
              ) : (
                items.map((impl) => {
                  const stConf = impl.status
                    ? (STATUS_CONFIG[impl.status] ?? null)
                    : null;
                  const isTravado    = impl.status === 'travado';
                  const isAguardando = impl.status === 'aguardando';

                  return (
                    <Link
                      key={impl.id}
                      href={`/implantacoes/${impl.id}`}
                      className={cn(
                        'group relative flex flex-col gap-2.5 rounded-lg border border-l-4 bg-white p-3',
                        'shadow-sm transition-all duration-150 hover:shadow-md hover:-translate-y-px',
                        stConf?.cardBar ?? 'border-l-gray-200',
                        isTravado && 'ring-1 ring-red-200 anim-travado',
                      )}
                    >
                      {/* Cavalo + nome do cliente */}
                      <div className="flex items-start gap-2">
                        <span
                          className="select-none text-base leading-none mt-0.5 flex-shrink-0"
                          aria-hidden
                        >
                          🏇
                        </span>
                        <div className="flex-1 min-w-0">
                          <p className="truncate text-sm font-semibold text-gray-800 leading-snug group-hover:text-black">
                            {impl.clienteRazaoSocial}
                          </p>
                          {impl.clienteNomeFantasia && (
                            <p className="truncate text-xs text-muted-foreground leading-tight">
                              {impl.clienteNomeFantasia}
                            </p>
                          )}
                        </div>
                      </div>

                      {/* Status badge */}
                      <div className="flex flex-wrap items-center gap-1.5">
                        {stConf ? (
                          <span
                            className={cn(
                              'inline-flex items-center gap-1 rounded border px-1.5 py-0.5 text-xs font-medium',
                              stConf.badge,
                            )}
                          >
                            <span
                              className={cn(
                                'h-1.5 w-1.5 rounded-full flex-shrink-0',
                                stConf.dot,
                              )}
                            />
                            {stConf.label}
                          </span>
                        ) : (
                          <span className="rounded border border-gray-200 bg-gray-100 px-1.5 py-0.5 text-xs font-medium text-gray-500">
                            —
                          </span>
                        )}

                        {/* Etapa — exibida discretamente no card */}
                        <span className="text-xs text-muted-foreground">
                          {ETAPA_LABELS[impl.etapa] ?? impl.etapa}
                        </span>
                      </div>

                      {/* Responsável */}
                      {impl.responsavel && (
                        <div className="flex items-center gap-1 text-xs text-muted-foreground">
                          <User className="h-3 w-3 flex-shrink-0" />
                          <span className="truncate">{impl.responsavel}</span>
                        </div>
                      )}

                      {/* Banner de alerta — travado ou aguardando */}
                      {(isTravado || isAguardando) && (
                        <div
                          className={cn(
                            'flex items-center gap-1.5 rounded px-2 py-1 text-xs font-medium',
                            isTravado
                              ? 'bg-red-50 text-red-700'
                              : 'bg-yellow-50 text-yellow-700',
                          )}
                        >
                          {isTravado ? (
                            <AlertTriangle className="h-3 w-3 flex-shrink-0" />
                          ) : (
                            <Clock className="h-3 w-3 flex-shrink-0" />
                          )}
                          <span>
                            {isTravado ? 'Atenção: travado' : 'Aguardando retorno'}
                          </span>
                        </div>
                      )}
                    </Link>
                  );
                })
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
