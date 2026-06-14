'use client';

import { cn } from '@/lib/utils';
import type { ImplantacaoCliente } from '@/lib/types/entities';

// ─── Tipos ────────────────────────────────────────────────────────────────────

export type ImplantacaoFiltroKey =
  | 'todos'
  | 'pre'
  | 'corrida'
  | 'onboarding'
  | 'curral'
  | 'travado'
  | 'aguardando'
  | 'fluindo';

// ─── Definições dos filtros ───────────────────────────────────────────────────

interface FilterDef {
  key: ImplantacaoFiltroKey;
  label: string;
  activeClass: string;
  countBadgeClass: string;
}

const FILTER_DEFS: FilterDef[] = [
  {
    key: 'todos',
    label: 'Todos',
    activeClass: 'border-slate-400 bg-slate-100 text-slate-800',
    countBadgeClass: 'bg-slate-200 text-slate-700',
  },
  {
    key: 'pre',
    label: 'Pré-Largada',
    activeClass: 'border-blue-300 bg-blue-100 text-blue-800',
    countBadgeClass: 'bg-blue-200 text-blue-700',
  },
  {
    key: 'corrida',
    label: 'Corrida',
    activeClass: 'border-orange-300 bg-orange-100 text-orange-800',
    countBadgeClass: 'bg-orange-200 text-orange-700',
  },
  {
    key: 'onboarding',
    label: 'Onboarding',
    activeClass: 'border-purple-300 bg-purple-100 text-purple-800',
    countBadgeClass: 'bg-purple-200 text-purple-700',
  },
  {
    key: 'curral',
    label: 'Curral',
    activeClass: 'border-gray-400 bg-gray-100 text-gray-700',
    countBadgeClass: 'bg-gray-200 text-gray-600',
  },
  {
    key: 'travado',
    label: 'Travados',
    activeClass: 'border-red-300 bg-red-100 text-red-800',
    countBadgeClass: 'bg-red-200 text-red-700',
  },
  {
    key: 'aguardando',
    label: 'Aguardando',
    activeClass: 'border-yellow-300 bg-yellow-100 text-yellow-800',
    countBadgeClass: 'bg-yellow-200 text-yellow-700',
  },
  {
    key: 'fluindo',
    label: 'Fluindo',
    activeClass: 'border-green-300 bg-green-100 text-green-800',
    countBadgeClass: 'bg-green-200 text-green-700',
  },
];

// ─── Helpers ──────────────────────────────────────────────────────────────────

const ETAPA_KEYS = new Set<string>(['pre', 'corrida', 'onboarding', 'curral']);

function countFor(impls: ImplantacaoCliente[], key: ImplantacaoFiltroKey): number {
  if (key === 'todos') return impls.length;
  if (ETAPA_KEYS.has(key)) return impls.filter((i) => i.etapa === key).length;
  return impls.filter((i) => i.status === key).length;
}

export function applyFiltro(
  impls: ImplantacaoCliente[],
  key: ImplantacaoFiltroKey,
): ImplantacaoCliente[] {
  if (key === 'todos') return impls;
  if (ETAPA_KEYS.has(key)) return impls.filter((i) => i.etapa === key);
  return impls.filter((i) => i.status === key);
}

// ─── Componente ───────────────────────────────────────────────────────────────

interface Props {
  implantacoes: ImplantacaoCliente[];
  active: ImplantacaoFiltroKey;
  onChange: (key: ImplantacaoFiltroKey) => void;
}

export function ImplantacaoFiltros({ implantacoes, active, onChange }: Props) {
  return (
    <div className="flex flex-wrap gap-2">
      {FILTER_DEFS.map((def) => {
        const count   = countFor(implantacoes, def.key);
        const isActive = active === def.key;

        return (
          <button
            key={def.key}
            type="button"
            onClick={() => onChange(def.key)}
            className={cn(
              'inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-medium',
              'transition-colors duration-150',
              isActive
                ? def.activeClass
                : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300 hover:bg-gray-50',
            )}
          >
            {def.label}
            <span
              className={cn(
                'rounded-full px-1.5 py-0.5 text-[10px] font-semibold tabular-nums',
                isActive ? def.countBadgeClass : 'bg-gray-100 text-gray-500',
              )}
            >
              {count}
            </span>
          </button>
        );
      })}
    </div>
  );
}
