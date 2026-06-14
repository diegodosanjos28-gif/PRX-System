'use client';

import { Users, Clock, Zap, CheckCircle2, AlertTriangle, ClipboardList } from 'lucide-react';
import { ImplantacaoCliente } from '@/lib/types/entities';
import { cn } from '@/lib/utils';

interface KpiDef {
  label: string;
  getValue: (list: ImplantacaoCliente[]) => number | string;
  Icon: React.ComponentType<{ className?: string }>;
  accentClass: string;
  iconClass: string;
}

const KPI_DEFS: KpiDef[] = [
  {
    label:      'Total',
    getValue:   (l) => l.length,
    Icon:       Users,
    accentClass:'border-l-slate-400',
    iconClass:  'text-slate-400',
  },
  {
    label:      'Pré-Largada',
    getValue:   (l) => l.filter((i) => i.etapa === 'pre').length,
    Icon:       Clock,
    accentClass:'border-l-blue-400',
    iconClass:  'text-blue-400',
  },
  {
    label:      'Corrida',
    getValue:   (l) => l.filter((i) => i.etapa === 'corrida').length,
    Icon:       Zap,
    accentClass:'border-l-orange-400',
    iconClass:  'text-orange-400',
  },
  {
    label:      'Onboarding',
    getValue:   (l) => l.filter((i) => i.etapa === 'onboarding').length,
    Icon:       CheckCircle2,
    accentClass:'border-l-purple-400',
    iconClass:  'text-purple-400',
  },
  {
    label:      'Travadas',
    getValue:   (l) => l.filter((i) => i.status === 'travado').length,
    Icon:       AlertTriangle,
    accentClass:'border-l-red-400',
    iconClass:  'text-red-400',
  },
  {
    // A listagem não retorna demandas (toListResponse ignora o campo).
    // KPI disponível apenas na tela de detalhe individual.
    label:      'Dem. Abertas',
    getValue:   () => '—',
    Icon:       ClipboardList,
    accentClass:'border-l-yellow-400',
    iconClass:  'text-yellow-500',
  },
];

interface Props {
  implantacoes: ImplantacaoCliente[];
}

export function ImplantacaoKpiBar({ implantacoes }: Props) {
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
      {KPI_DEFS.map(({ label, getValue, Icon, accentClass, iconClass }, index) => (
        <div
          key={label}
          style={{ animationDelay: `${index * 55}ms` }}
          className={cn(
            'anim-fade-in-up rounded-lg border border-l-4 bg-white px-4 py-3',
            accentClass,
          )}
        >
          <div className="flex items-center justify-between">
            <span className="text-2xl font-bold text-gray-800">
              {getValue(implantacoes)}
            </span>
            <Icon className={cn('h-5 w-5 flex-shrink-0', iconClass)} />
          </div>
          <p className="mt-1 text-xs text-muted-foreground leading-tight">{label}</p>
        </div>
      ))}
    </div>
  );
}
