'use client';

import { CheckCircle2, Circle, ClipboardList } from 'lucide-react';
import { cn } from '@/lib/utils';

// ─── Tipos ────────────────────────────────────────────────────────────────────

interface ChecklistItem {
  label: string;
  concluido: boolean;
}

// ─── Itens padrão por etapa (referência para futura edição) ───────────────────

export const DEFAULT_CHECKLIST: Record<string, string[]> = {
  pre: [
    'Reunião coleta dados',
    'Acessos portais',
    'Fichário',
    'Assinar proposta',
    'Subir à Conciflex',
  ],
  corrida: [
    'Contato gerente conta',
    'Conciflex enviar cartas',
    'Assinatura cartas',
    'Aguardando terceiros',
  ],
  onboarding: [
    'Marcar reunião',
    'Apresentar sistema',
  ],
  curral: [],
};

// ─── Parser de progressJson ───────────────────────────────────────────────────
// Aceita: null, string JSON, array JS ou { items: [...] }

function parseChecklist(progressJson: unknown): ChecklistItem[] | null {
  if (progressJson == null) return null;

  try {
    let data: unknown = progressJson;

    if (typeof data === 'string') {
      if (!data.trim()) return null;
      data = JSON.parse(data);
    }

    const arr: unknown = Array.isArray(data)
      ? data
      : typeof data === 'object' && data !== null && Array.isArray((data as Record<string, unknown>).items)
        ? (data as Record<string, unknown>).items
        : null;

    if (!Array.isArray(arr)) return null;

    const items = arr
      .filter(
        (item): item is Record<string, unknown> =>
          item !== null &&
          typeof item === 'object' &&
          typeof (item as Record<string, unknown>).label === 'string',
      )
      .map((item) => ({
        label:    item.label as string,
        concluido: Boolean(item.concluido),
      }));

    return items.length > 0 ? items : null;
  } catch {
    return null;
  }
}

// ─── Componente ───────────────────────────────────────────────────────────────

interface Props {
  progressJson: unknown;
  etapa: string;
  onToggle?: (index: number) => void;
  isPending?: boolean;
}

export function ImplantacaoChecklist({ progressJson, etapa, onToggle, isPending }: Props) {
  const items = parseChecklist(progressJson);
  const concluidos = items?.filter((i) => i.concluido).length ?? 0;
  const total      = items?.length ?? 0;
  const pct        = total > 0 ? Math.round((concluidos / total) * 100) : 0;

  const defaultCount = DEFAULT_CHECKLIST[etapa]?.length ?? 0;

  return (
    <div className="rounded-lg border bg-white">

      {/* Cabeçalho */}
      <div className="flex items-center justify-between border-b px-4 py-2.5">
        <div className="flex items-center gap-2">
          <ClipboardList className="h-4 w-4 text-muted-foreground flex-shrink-0" />
          <span className="text-sm font-semibold text-gray-800">Checklist Operacional</span>
        </div>
        {items ? (
          <span className="text-xs text-muted-foreground">
            {concluidos}/{total} concluído{total !== 1 ? 's' : ''}
          </span>
        ) : defaultCount > 0 ? (
          <span className="text-xs text-muted-foreground">
            {defaultCount} item{defaultCount !== 1 ? 's' : ''} previstos
          </span>
        ) : null}
      </div>

      {/* Corpo */}
      {!items ? (
        /* Estado vazio */
        <div className="px-4 py-5 text-center">
          <ClipboardList className="mx-auto mb-2 h-8 w-8 text-gray-200" />
          <p className="text-sm text-muted-foreground">
            Nenhum checklist cadastrado para esta implantação.
          </p>
          {defaultCount > 0 && (
            <p className="mt-1 text-xs text-muted-foreground/70">
              Esta etapa possui {defaultCount} item{defaultCount !== 1 ? 's' : ''} sugeridos.
            </p>
          )}
        </div>
      ) : (
        <>
          {/* Barra de progresso */}
          <div className="px-4 pt-3 pb-1">
            <div className="h-1.5 w-full overflow-hidden rounded-full bg-gray-100">
              <div
                className={cn(
                  'h-full rounded-full transition-all duration-300',
                  pct === 100 ? 'bg-green-500' : pct >= 50 ? 'bg-blue-400' : 'bg-orange-400',
                )}
                style={{ width: `${pct}%` }}
              />
            </div>
            <p className="mt-1 text-right text-[11px] text-muted-foreground">{pct}% concluído</p>
          </div>

          {/* Lista de itens */}
          <div className="divide-y px-4 pb-2">
            {items.map((item, idx) =>
              onToggle ? (
                <button
                  key={idx}
                  type="button"
                  onClick={() => onToggle(idx)}
                  disabled={isPending}
                  className={cn(
                    'flex w-full items-center gap-3 py-2.5 text-sm text-left',
                    'rounded transition-colors duration-150',
                    item.concluido ? 'text-muted-foreground' : 'text-gray-800',
                    !isPending && 'hover:bg-gray-50',
                    isPending ? 'cursor-wait opacity-60' : 'cursor-pointer',
                  )}
                >
                  {item.concluido ? (
                    <CheckCircle2 className="h-4 w-4 flex-shrink-0 text-green-500" />
                  ) : (
                    <Circle className="h-4 w-4 flex-shrink-0 text-gray-300" />
                  )}
                  <span className={cn(item.concluido && 'line-through opacity-60')}>
                    {item.label}
                  </span>
                </button>
              ) : (
                <div
                  key={idx}
                  className={cn(
                    'flex items-center gap-3 py-2.5 text-sm',
                    item.concluido ? 'text-muted-foreground' : 'text-gray-800',
                  )}
                >
                  {item.concluido ? (
                    <CheckCircle2 className="h-4 w-4 flex-shrink-0 text-green-500" />
                  ) : (
                    <Circle className="h-4 w-4 flex-shrink-0 text-gray-300" />
                  )}
                  <span className={cn(item.concluido && 'line-through opacity-60')}>
                    {item.label}
                  </span>
                </div>
              ),
            )}
          </div>
        </>
      )}
    </div>
  );
}
