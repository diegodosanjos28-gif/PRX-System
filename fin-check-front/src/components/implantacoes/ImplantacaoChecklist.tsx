'use client';

import { useState, useRef, useEffect } from 'react';
import {
  CheckCircle2,
  Circle,
  ClipboardList,
  Pencil,
  Trash2,
  Plus,
  X,
  Check,
} from 'lucide-react';
import { cn } from '@/lib/utils';

// ─── Tipos ────────────────────────────────────────────────────────────────────

interface Subtask {
  label: string;
  concluido: boolean;
}

export interface ChecklistItem {
  label: string;
  concluido: boolean;
  subtasks?: Subtask[];
}

// ─── Itens padrão por etapa ──────────────────────────────────────────────────

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
  onboarding: ['Marcar reunião', 'Apresentar sistema'],
  curral: [],
};

// ─── Helper exportado: item efetivamente concluído? ──────────────────────────

export function isItemConcluido(item: ChecklistItem): boolean {
  if (!item.subtasks || item.subtasks.length === 0) return item.concluido;
  return item.subtasks.every((s) => s.concluido);
}

// ─── Parser de progressJson ──────────────────────────────────────────────────

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
      : typeof data === 'object' &&
        data !== null &&
        Array.isArray((data as Record<string, unknown>).items)
      ? (data as Record<string, unknown>).items
      : null;

    if (!Array.isArray(arr)) return null;

    const parsed = arr
      .filter(
        (item): item is Record<string, unknown> =>
          item !== null &&
          typeof item === 'object' &&
          typeof (item as Record<string, unknown>).label === 'string',
      )
      .map((item) => {
        const rawSubs = Array.isArray(item.subtasks) ? item.subtasks : [];
        const subtasks: Subtask[] = rawSubs
          .filter(
            (s): s is Record<string, unknown> =>
              s !== null &&
              typeof s === 'object' &&
              typeof (s as Record<string, unknown>).label === 'string',
          )
          .map((s) => ({ label: s.label as string, concluido: Boolean(s.concluido) }));
        return {
          label:     item.label as string,
          concluido: Boolean(item.concluido),
          ...(subtasks.length > 0 ? { subtasks } : {}),
        };
      });

    return parsed.length > 0 ? parsed : null;
  } catch {
    return null;
  }
}

// ─── Estado de edição ─────────────────────────────────────────────────────────
// O input é NÃO-CONTROLADO (defaultValue, sem value/onChange).
// Isso garante ZERO re-renders durante a digitação — o DOM gerencia o valor sozinho.
// editingTarget identifica qual item/subtarefa está em edição.
// isNew = true marca subtarefas recém-criadas (label vazio) para remoção ao cancelar.

type EditingTarget =
  | { type: 'item';    itemIdx: number }
  | { type: 'subtask'; itemIdx: number; subtaskIdx: number; isNew?: boolean };

type ConfirmState =
  | { type: 'item';    itemIdx: number }
  | { type: 'subtask'; itemIdx: number; subtaskIdx: number };

// ─── Componente ───────────────────────────────────────────────────────────────

interface Props {
  progressJson: unknown;
  etapa: string;
  onSave?: (items: ChecklistItem[]) => void;
  isPending?: boolean;
}

export function ImplantacaoChecklist({ progressJson, etapa, onSave, isPending }: Props) {
  const [items, setItems]                       = useState<ChecklistItem[]>(() => parseChecklist(progressJson) ?? []);
  const [editingTarget, setEditingTarget]       = useState<EditingTarget | null>(null);
  const [confirm,       setConfirm]             = useState<ConfirmState | null>(null);
  const editInputRef                            = useRef<HTMLInputElement>(null);

  // Foca o campo UMA ÚNICA VEZ ao entrar em edição.
  // O input é não-controlado, então este efeito NÃO RODA durante a digitação
  // (editingTarget não muda enquanto o usuário digita).
  // Sem select() — cursor aparece no final do texto.
  useEffect(() => {
    if (editingTarget !== null && editInputRef.current) {
      // Posiciona o cursor no final do texto existente
      const el = editInputRef.current;
      el.focus();
      const len = el.value.length;
      el.setSelectionRange(len, len);
    }
  }, [editingTarget]);

  const canEdit      = !!onSave;
  const defaultCount = DEFAULT_CHECKLIST[etapa]?.length ?? 0;
  const total        = items.length;
  const concluidos   = items.filter(isItemConcluido).length;
  const pct          = total > 0 ? Math.round((concluidos / total) * 100) : 0;

  // ── Persistência ─────────────────────────────────────────────────────────

  const applyAndSave = (next: ChecklistItem[]) => {
    setItems(next);
    onSave?.(next);
  };

  // ── Toggle item principal (só sem subtarefas) ─────────────────────────────

  const toggleItem = (idx: number) => {
    if (!canEdit || isPending) return;
    if ((items[idx].subtasks?.length ?? 0) > 0) return;
    applyAndSave(
      items.map((it, i) => (i === idx ? { ...it, concluido: !it.concluido } : it)),
    );
  };

  // ── Toggle subtarefa ──────────────────────────────────────────────────────

  const toggleSubtask = (itemIdx: number, subIdx: number) => {
    if (!canEdit || isPending) return;
    const next = items.map((it, i) => {
      if (i !== itemIdx) return it;
      const subs = (it.subtasks ?? []).map((s, j) =>
        j === subIdx ? { ...s, concluido: !s.concluido } : s,
      );
      return { ...it, subtasks: subs, concluido: subs.every((s) => s.concluido) };
    });
    applyAndSave(next);
  };

  // ── Iniciar edição ────────────────────────────────────────────────────────

  const startEditItem = (idx: number) => {
    setConfirm(null);
    setEditingTarget({ type: 'item', itemIdx: idx });
  };

  const startEditSubtask = (itemIdx: number, subIdx: number) => {
    setConfirm(null);
    setEditingTarget({ type: 'subtask', itemIdx, subtaskIdx: subIdx });
  };

  // ── Cancelar edição ───────────────────────────────────────────────────────

  const cancelEdit = () => {
    // Se a subtarefa foi recém-criada e o usuário cancelou, remover o placeholder vazio
    if (editingTarget?.type === 'subtask' && editingTarget.isNew) {
      const { itemIdx, subtaskIdx } = editingTarget;
      setItems((prev) =>
        prev.map((it, i) => {
          if (i !== itemIdx) return it;
          const subs = (it.subtasks ?? []).filter((_, j) => j !== subtaskIdx);
          return subs.length === 0
            ? { label: it.label, concluido: false }
            : { ...it, subtasks: subs };
        }),
      );
    }
    setEditingTarget(null);
  };

  // ── Confirmar edição (Enter ou botão ✅) ─────────────────────────────────
  // Lê o valor diretamente do DOM — nenhuma re-renderização durante a digitação.

  const commitEdit = () => {
    if (!editingTarget || !editInputRef.current) return;
    const trimmed = editInputRef.current.value.trim();
    if (!trimmed) { cancelEdit(); return; }

    let next: ChecklistItem[];
    if (editingTarget.type === 'item') {
      next = items.map((it, i) =>
        i === editingTarget.itemIdx ? { ...it, label: trimmed } : it,
      );
    } else {
      next = items.map((it, i) => {
        if (i !== editingTarget.itemIdx) return it;
        const subs = (it.subtasks ?? []).map((s, j) =>
          j === editingTarget.subtaskIdx ? { ...s, label: trimmed } : s,
        );
        return { ...it, subtasks: subs };
      });
    }
    setEditingTarget(null);
    applyAndSave(next);
  };

  // ── Adicionar subtarefa ───────────────────────────────────────────────────

  const addSubtask = (itemIdx: number) => {
    if (!canEdit || isPending) return;
    const currentSubs = items[itemIdx].subtasks ?? [];
    const newSubIdx   = currentSubs.length;
    // Adiciona ao estado local com label vazio — NÃO chama onSave ainda.
    // Se o usuário cancelar, cancelEdit() remove este item.
    const next = items.map((it, i) =>
      i === itemIdx
        ? { ...it, subtasks: [...currentSubs, { label: '', concluido: false }] }
        : it,
    );
    setItems(next);
    setEditingTarget({ type: 'subtask', itemIdx, subtaskIdx: newSubIdx, isNew: true });
  };

  // ── Excluir item ──────────────────────────────────────────────────────────

  const requestDeleteItem = (idx: number) => {
    setEditingTarget(null);
    setConfirm({ type: 'item', itemIdx: idx });
  };

  const doDeleteItem = () => {
    if (!confirm || confirm.type !== 'item') return;
    applyAndSave(items.filter((_, i) => i !== confirm.itemIdx));
    setConfirm(null);
  };

  // ── Excluir subtarefa ─────────────────────────────────────────────────────

  const requestDeleteSubtask = (itemIdx: number, subIdx: number) => {
    setEditingTarget(null);
    setConfirm({ type: 'subtask', itemIdx, subtaskIdx: subIdx });
  };

  const doDeleteSubtask = () => {
    if (!confirm || confirm.type !== 'subtask') return;
    const { itemIdx, subtaskIdx } = confirm;
    const next = items.map((it, i) => {
      if (i !== itemIdx) return it;
      const subs = (it.subtasks ?? []).filter((_, j) => j !== subtaskIdx);
      return subs.length === 0
        ? { label: it.label, concluido: false }
        : { ...it, subtasks: subs };
    });
    applyAndSave(next);
    setConfirm(null);
  };

  // ── Estilos dos botões de edição inline ──────────────────────────────────

  const btnSave   = 'flex-shrink-0 rounded p-1 text-green-600 hover:bg-green-50 disabled:opacity-40';
  const btnCancel = 'flex-shrink-0 rounded p-1 text-gray-400 hover:bg-gray-100';

  // ─────────────────────────────────────────────────────────────────────────

  return (
    <div className="rounded-lg border bg-white">

      {/* Cabeçalho */}
      <div className="flex items-center justify-between border-b px-4 py-2.5">
        <div className="flex items-center gap-2">
          <ClipboardList className="h-4 w-4 text-muted-foreground flex-shrink-0" />
          <span className="text-sm font-semibold text-gray-800">Checklist Operacional</span>
        </div>
        {total > 0 ? (
          <span className="text-xs text-muted-foreground">
            {concluidos}/{total} concluído{total !== 1 ? 's' : ''}
          </span>
        ) : defaultCount > 0 ? (
          <span className="text-xs text-muted-foreground">
            {defaultCount} item{defaultCount !== 1 ? 's' : ''} previstos
          </span>
        ) : null}
      </div>

      {/* Estado vazio */}
      {total === 0 ? (
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

          {/* Lista */}
          <div className="divide-y px-4 pb-2">
            {items.map((item, idx) => {
              const hasSubs       = (item.subtasks?.length ?? 0) > 0;
              const subTotal      = item.subtasks?.length ?? 0;
              const subDone       = item.subtasks?.filter((s) => s.concluido).length ?? 0;
              const itemDone      = isItemConcluido(item);
              const isEditingItem = !!editingTarget && editingTarget.type === 'item' && editingTarget.itemIdx === idx;
              const isConfirmItem = !!confirm && confirm.type === 'item' && confirm.itemIdx === idx;

              return (
                <div key={`item-${idx}`} className="py-2">

                  {/* ── Item principal ───────────────────────────────────── */}
                  <div className="group flex items-center gap-2 min-w-0">

                    {/* Bolinha de conclusão */}
                    <button
                      type="button"
                      onClick={() => toggleItem(idx)}
                      disabled={hasSubs || !canEdit || isPending}
                      title={hasSubs ? 'Conclua as subtarefas para completar este item' : undefined}
                      className={cn(
                        'flex-shrink-0 transition-opacity',
                        hasSubs || !canEdit ? 'cursor-default opacity-40' : 'cursor-pointer hover:opacity-70',
                        isPending && 'cursor-wait',
                      )}
                    >
                      {itemDone
                        ? <CheckCircle2 className="h-4 w-4 text-green-500" />
                        : <Circle       className="h-4 w-4 text-gray-300"  />}
                    </button>

                    {/* Label / input não-controlado */}
                    {isEditingItem ? (
                      <>
                        <input
                          ref={editInputRef}
                          defaultValue={item.label}
                          onKeyDown={(e) => {
                            if (e.key === 'Enter')  { e.preventDefault(); commitEdit(); }
                            if (e.key === 'Escape') cancelEdit();
                          }}
                          className="flex-1 min-w-0 rounded border border-blue-300 bg-blue-50 px-2 py-0.5 text-sm outline-none focus:ring-1 focus:ring-blue-400"
                        />
                        <button type="button" onClick={commitEdit} title="Salvar (Enter)" className={btnSave}>
                          <Check className="h-3.5 w-3.5" />
                        </button>
                        <button type="button" onClick={cancelEdit} title="Cancelar (Esc)" className={btnCancel}>
                          <X className="h-3.5 w-3.5" />
                        </button>
                      </>
                    ) : (
                      <span
                        className={cn(
                          'flex-1 min-w-0 text-sm leading-snug truncate',
                          itemDone ? 'line-through text-muted-foreground opacity-60' : 'text-gray-800',
                        )}
                      >
                        {item.label}
                        {hasSubs && (
                          <span className="ml-2 text-[10px] font-normal text-muted-foreground">
                            {subDone}/{subTotal} subtarefa{subTotal !== 1 ? 's' : ''}
                          </span>
                        )}
                      </span>
                    )}

                    {/* Botões de ação (aparecem no hover) */}
                    {!isEditingItem && canEdit && !isConfirmItem && (
                      <div className="flex flex-shrink-0 items-center gap-0.5 opacity-0 transition-opacity group-hover:opacity-100">
                        <button
                          type="button"
                          onClick={() => startEditItem(idx)}
                          title="Editar"
                          className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
                        >
                          <Pencil className="h-3 w-3" />
                        </button>
                        <button
                          type="button"
                          onClick={() => addSubtask(idx)}
                          title="Adicionar subtarefa"
                          className="rounded p-1 text-gray-400 hover:bg-blue-50 hover:text-blue-600"
                        >
                          <Plus className="h-3 w-3" />
                        </button>
                        <button
                          type="button"
                          onClick={() => requestDeleteItem(idx)}
                          title="Excluir item"
                          className="rounded p-1 text-gray-400 hover:bg-red-50 hover:text-red-600"
                        >
                          <Trash2 className="h-3 w-3" />
                        </button>
                      </div>
                    )}
                  </div>

                  {/* Confirmação de exclusão do item */}
                  {isConfirmItem && (
                    <div className="mt-1.5 flex items-center gap-2 rounded-md bg-red-50 px-3 py-1.5 text-xs text-red-700">
                      <span className="flex-1">Excluir este item{hasSubs ? ' e suas subtarefas' : ''}?</span>
                      <button
                        type="button"
                        onClick={doDeleteItem}
                        className="flex items-center gap-1 rounded bg-red-600 px-2 py-0.5 text-white hover:bg-red-700"
                      >
                        <Check className="h-3 w-3" /> Sim
                      </button>
                      <button
                        type="button"
                        onClick={() => setConfirm(null)}
                        className="flex items-center gap-1 rounded border border-red-200 px-2 py-0.5 hover:bg-red-100"
                      >
                        <X className="h-3 w-3" /> Não
                      </button>
                    </div>
                  )}

                  {/* ── Subtarefas ────────────────────────────────────────── */}
                  {hasSubs && (
                    <div className="ml-6 mt-1 space-y-0.5">
                      {item.subtasks!.map((sub, subIdx) => {
                        const isEditingSub = !!editingTarget && editingTarget.type === 'subtask' && editingTarget.itemIdx === idx && editingTarget.subtaskIdx === subIdx;
                        const isConfirmSub = !!confirm && confirm.type === 'subtask' && confirm.itemIdx === idx && confirm.subtaskIdx === subIdx;

                        return (
                          <div key={`sub-${idx}-${subIdx}`}>
                            <div className="group flex items-center gap-2 min-w-0 py-0.5">

                              {/* Bolinha da subtarefa */}
                              <button
                                type="button"
                                onClick={() => toggleSubtask(idx, subIdx)}
                                disabled={!canEdit || isPending}
                                className={cn(
                                  'flex-shrink-0 transition-opacity hover:opacity-70',
                                  (!canEdit || isPending) && 'cursor-default',
                                )}
                              >
                                {sub.concluido
                                  ? <CheckCircle2 className="h-3.5 w-3.5 text-green-500" />
                                  : <Circle       className="h-3.5 w-3.5 text-gray-300"  />}
                              </button>

                              {/* Label / input não-controlado da subtarefa */}
                              {isEditingSub ? (
                                <>
                                  <input
                                    ref={editInputRef}
                                    defaultValue={sub.label}
                                    onKeyDown={(e) => {
                                      if (e.key === 'Enter')  { e.preventDefault(); commitEdit(); }
                                      if (e.key === 'Escape') cancelEdit();
                                    }}
                                    className="flex-1 min-w-0 rounded border border-blue-300 bg-blue-50 px-2 py-0.5 text-xs outline-none focus:ring-1 focus:ring-blue-400"
                                  />
                                  <button type="button" onClick={commitEdit} title="Salvar (Enter)" className={btnSave}>
                                    <Check className="h-3 w-3" />
                                  </button>
                                  <button type="button" onClick={cancelEdit} title="Cancelar (Esc)" className={btnCancel}>
                                    <X className="h-3 w-3" />
                                  </button>
                                </>
                              ) : (
                                <span
                                  className={cn(
                                    'flex-1 min-w-0 text-xs leading-snug',
                                    sub.concluido
                                      ? 'line-through text-muted-foreground opacity-60'
                                      : 'text-gray-700',
                                  )}
                                >
                                  {sub.label || <span className="italic text-muted-foreground">vazio</span>}
                                </span>
                              )}

                              {/* Ações da subtarefa */}
                              {!isEditingSub && canEdit && !isConfirmSub && (
                                <div className="flex flex-shrink-0 items-center gap-0.5 opacity-0 transition-opacity group-hover:opacity-100">
                                  <button
                                    type="button"
                                    onClick={() => startEditSubtask(idx, subIdx)}
                                    title="Editar subtarefa"
                                    className="rounded p-0.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
                                  >
                                    <Pencil className="h-3 w-3" />
                                  </button>
                                  <button
                                    type="button"
                                    onClick={() => requestDeleteSubtask(idx, subIdx)}
                                    title="Excluir subtarefa"
                                    className="rounded p-0.5 text-gray-400 hover:bg-red-50 hover:text-red-600"
                                  >
                                    <Trash2 className="h-3 w-3" />
                                  </button>
                                </div>
                              )}
                            </div>

                            {/* Confirmação de exclusão da subtarefa */}
                            {isConfirmSub && (
                              <div className="mt-1 flex items-center gap-2 rounded-md bg-red-50 px-3 py-1 text-xs text-red-700">
                                <span className="flex-1">Excluir esta subtarefa?</span>
                                <button
                                  type="button"
                                  onClick={doDeleteSubtask}
                                  className="flex items-center gap-1 rounded bg-red-600 px-2 py-0.5 text-white hover:bg-red-700"
                                >
                                  <Check className="h-3 w-3" /> Sim
                                </button>
                                <button
                                  type="button"
                                  onClick={() => setConfirm(null)}
                                  className="flex items-center gap-1 rounded border border-red-200 px-2 py-0.5 hover:bg-red-100"
                                >
                                  <X className="h-3 w-3" /> Não
                                </button>
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
}
