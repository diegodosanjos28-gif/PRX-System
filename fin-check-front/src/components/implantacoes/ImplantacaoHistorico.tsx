'use client';

import { Activity, Archive, CirclePlus, Flag, History, RefreshCw } from 'lucide-react';
import type { ImplantacaoCliente } from '@/lib/types/entities';

interface Props {
  impl: ImplantacaoCliente;
}

interface TimelineItem {
  date: string;
  label: string;
  Icon: React.ComponentType<{ className?: string }>;
  dotClass: string;
}

function fmt(val: string) {
  return new Date(val).toLocaleDateString('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

export function ImplantacaoHistorico({ impl }: Props) {
  const candidates: (TimelineItem | null)[] = [
    impl.createdAt
      ? { date: impl.createdAt,         label: 'Implantação criada',    Icon: CirclePlus, dotClass: 'bg-blue-50 border-blue-200 text-blue-500'    }
      : null,
    impl.etapaIniciadaEm
      ? { date: impl.etapaIniciadaEm,   label: 'Início da etapa atual', Icon: Flag,       dotClass: 'bg-orange-50 border-orange-200 text-orange-500' }
      : null,
    impl.dataEntradaCurral
      ? { date: impl.dataEntradaCurral, label: 'Entrada no curral',     Icon: Archive,    dotClass: 'bg-gray-50 border-gray-200 text-gray-500'    }
      : null,
    impl.ultimoMovimento
      ? { date: impl.ultimoMovimento,   label: 'Último movimento',      Icon: Activity,   dotClass: 'bg-purple-50 border-purple-200 text-purple-500' }
      : null,
    impl.updatedAt
      ? { date: impl.updatedAt,         label: 'Última atualização',    Icon: RefreshCw,  dotClass: 'bg-green-50 border-green-200 text-green-500'  }
      : null,
  ];

  const items = candidates
    .filter((c): c is TimelineItem => c !== null)
    .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());

  if (items.length === 0) return null;

  return (
    <div className="rounded-lg border bg-white">
      {/* Cabeçalho */}
      <div className="flex items-center gap-2 border-b px-4 py-2.5">
        <History className="h-4 w-4 text-muted-foreground flex-shrink-0" />
        <span className="text-sm font-semibold text-gray-800">Histórico Operacional</span>
      </div>

      {/* Timeline */}
      <div className="px-4 py-3">
        <div className="relative">
          {/* Linha vertical entre os itens */}
          {items.length > 1 && (
            <div className="absolute left-4 top-4 bottom-4 w-px bg-gray-100" />
          )}

          <ul className="space-y-4">
            {items.map((item, idx) => (
              <li key={idx} className="relative flex items-start gap-3">
                {/* Ícone (dot) */}
                <div
                  className={`relative z-10 flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full border ${item.dotClass}`}
                >
                  <item.Icon className="h-3.5 w-3.5" />
                </div>

                {/* Texto */}
                <div className="min-w-0 pt-1">
                  <p className="text-sm font-medium leading-none text-gray-800">
                    {item.label}
                  </p>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {fmt(item.date)}
                  </p>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}
