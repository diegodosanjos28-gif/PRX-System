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

export interface ChecklistNode {
  label: string;
  concluido: boolean;
  children?: ChecklistNode[];
}

// Alias para retrocompatibilidade com page.tsx
export type ChecklistItem = ChecklistNode;

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

// ─── Helpers ─────────────────────────────────────────────────────────────────

// Um nó é concluído quando é folha e concluido=true,
// ou quando tem filhos e TODOS os filhos são concluídos recursivamente.
export function isNodeDone(node: ChecklistNode): boolean {
  if (!node.children || node.children.length === 0) return node.concluido;
  return node.children.every(isNodeDone);
}

// Alias para retrocompatibilidade com page.tsx
export const isItemConcluido = isNodeDone;

function pathsEqual(a: number[], b: number[]): boolean {
  return a.length === b.length && a.every((v, i) => v === b[i]);
}

// Converte formato antigo (subtasks) para novo (children), recursivamente.
function migrateNode(raw: unknown): ChecklistNode | null {
  if (raw === null || typeof raw !== 'object' || Array.isArray(raw)) return null;
  const obj = raw as Record<string, unknown>;
  if (typeof obj.label !== 'string') return null;

  const rawChildren = Array.isArray(obj.children) ? obj.children
                    : Array.isArray(obj.subtasks)  ? obj.subtasks
                    : [];

  const children = rawChildren.map(migrateNode).filter((n): n is ChecklistNode => n !== null);

  return {
    label:     obj.label,
    concluido: Boolean(obj.concluido),
    ...(children.length > 0 ? { children } : {}),
  };
}

function parseChecklist(progressJson: unknown): ChecklistNode[] | null {
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
    const parsed = arr.map(migrateNode).filter((n): n is ChecklistNode => n !== null);
    return parsed.length > 0 ? parsed : null;
  } catch {
    return null;
  }
}

// ─── Operações imutáveis na árvore ───────────────────────────────────────────

// Aplica `updater` no nó em `path` e recalcula `concluido` de cada ancestral (cascade up).
function updateAtPath(
  nodes: ChecklistNode[],
  path: number[],
  updater: (node: ChecklistNode) => ChecklistNode,
): ChecklistNode[] {
  const [idx, ...rest] = path;
  return nodes.map((node, i) => {
    if (i !== idx) return node;
    if (rest.length === 0) return updater(node);
    const newChildren = updateAtPath(node.children ?? [], rest, updater);
    return {
      ...node,
      children: newChildren,
      // Cascade up: pai concluído quando todos os filhos estiverem concluídos
      concluido: newChildren.length > 0 ? newChildren.every(isNodeDone) : node.concluido,
    };
  });
}

function getAtPath(nodes: ChecklistNode[], path: number[]): ChecklistNode {
  const [idx, ...rest] = path;
  if (rest.length === 0) return nodes[idx];
  return getAtPath(nodes[idx].children ?? [], rest);
}

// Marca recursivamente todos os nós da subárvore com o mesmo `concluido`.
function cascadeDown(node: ChecklistNode, concluido: boolean): ChecklistNode {
  return {
    ...node,
    concluido,
    children: node.children?.map(c => cascadeDown(c, concluido)),
  };
}

// Remove o nó em `path` e recalcula `concluido` dos ancestrais.
function removeAtPath(nodes: ChecklistNode[], path: number[]): ChecklistNode[] {
  const [idx, ...rest] = path;
  if (rest.length === 0) return nodes.filter((_, i) => i !== idx);
  return nodes.map((node, i) => {
    if (i !== idx) return node;
    const newChildren = removeAtPath(node.children ?? [], rest);
    return {
      ...node,
      children:  newChildren.length > 0 ? newChildren : undefined,
      concluido: newChildren.length > 0 ? newChildren.every(isNodeDone) : node.concluido,
    };
  });
}

// Conta apenas folhas (nós sem filhos) para o progresso global.
function countLeaves(nodes: ChecklistNode[]): { total: number; done: number } {
  let total = 0, done = 0;
  for (const node of nodes) {
    if (!node.children || node.children.length === 0) {
      total++;
      if (node.concluido) done++;
    } else {
      const sub  = countLeaves(node.children);
      total += sub.total;
      done  += sub.done;
    }
  }
  return { total, done };
}

// ─── Tipos internos ───────────────────────────────────────────────────────────

type EditingTarget = { path: number[]; isNew?: boolean };

// ─── Componente recursivo de nó ──────────────────────────────────────────────

interface NodeItemProps {
  node:          ChecklistNode;
  path:          number[];
  editingTarget: EditingTarget | null;
  confirmPath:   number[] | null;
  editInputRef:  React.RefObject<HTMLInputElement>;
  onToggle:        (path: number[]) => void;
  onStartEdit:     (path: number[]) => void;
  onAddChild:      (path: number[]) => void;
  onRequestDelete: (path: number[]) => void;
  onCommitEdit:    () => void;
  onCancelEdit:    () => void;
  onConfirmDelete: () => void;
  onCancelDelete:  () => void;
  canEdit:   boolean;
  isPending: boolean;
}

function ChecklistNodeItem({
  node, path, editingTarget, confirmPath, editInputRef,
  onToggle, onStartEdit, onAddChild, onRequestDelete,
  onCommitEdit, onCancelEdit, onConfirmDelete, onCancelDelete,
  canEdit, isPending,
}: NodeItemProps) {
  const isEditing     = editingTarget !== null && pathsEqual(editingTarget.path, path);
  const isConfirm     = confirmPath   !== null && pathsEqual(confirmPath, path);
  const hasChildren   = (node.children?.length ?? 0) > 0;
  const effectiveDone = isNodeDone(node);
  const depth         = path.length - 1; // 0 = raiz

  const childTotal = node.children?.length ?? 0;
  const childDone  = node.children?.filter(isNodeDone).length ?? 0;

  const iconCls  = depth === 0 ? 'h-5 w-5' : 'h-4 w-4';
  const labelCls = cn(
    'flex-1 min-w-0 leading-snug break-words',
    depth === 0 ? 'text-base font-medium' : 'text-sm',
    effectiveDone ? 'line-through text-muted-foreground opacity-60' : 'text-gray-800',
  );

  const btnSave   = 'flex-shrink-0 rounded p-1 text-green-600 hover:bg-green-50';
  const btnCancel = 'flex-shrink-0 rounded p-1 text-gray-400 hover:bg-gray-100';

  return (
    <div>
      {/* ── Linha do nó ──────────────────────────────────────── */}
      <div className={cn('group flex items-start gap-2 min-w-0', depth === 0 ? 'py-2' : 'py-1')}>

        {/* Bolinha de conclusão */}
        <button
          type="button"
          onClick={() => onToggle(path)}
          disabled={!canEdit || isPending}
          title={hasChildren ? 'Alterna todos os filhos' : undefined}
          className={cn(
            'mt-0.5 flex-shrink-0 transition-opacity',
            (!canEdit || isPending) ? 'cursor-default' : 'cursor-pointer hover:opacity-70',
          )}
        >
          {effectiveDone
            ? <CheckCircle2 className={cn(iconCls, 'text-green-500')} />
            : <Circle       className={cn(iconCls, 'text-gray-300')}  />}
        </button>

        {/* Input não-controlado (só durante edição) */}
        {isEditing ? (
          <>
            <input
              ref={editInputRef}
              defaultValue={node.label}
              onKeyDown={(e) => {
                if (e.key === 'Enter')  { e.preventDefault(); onCommitEdit(); }
                if (e.key === 'Escape') onCancelEdit();
              }}
              className={cn(
                'flex-1 min-w-0 rounded border border-blue-300 bg-blue-50 px-2 py-0.5 outline-none focus:ring-1 focus:ring-blue-400',
                depth === 0 ? 'text-base' : 'text-sm',
              )}
            />
            <button type="button" onClick={onCommitEdit} title="Salvar (Enter)" className={btnSave}>
              <Check className="h-3.5 w-3.5" />
            </button>
            <button type="button" onClick={onCancelEdit} title="Cancelar (Esc)" className={btnCancel}>
              <X className="h-3.5 w-3.5" />
            </button>
          </>
        ) : (
          <>
            <span className={labelCls}>
              {node.label || <span className="italic text-muted-foreground">vazio</span>}
            </span>

            {/* Badge [n/m] — apenas em nós com filhos */}
            {hasChildren && (
              <span className={cn(
                'flex-shrink-0 rounded-full bg-gray-100 px-2 py-0.5 font-mono tabular-nums text-muted-foreground',
                depth === 0 ? 'text-sm' : 'text-xs',
                childDone > 0 && childDone === childTotal && 'bg-green-100 text-green-700',
              )}>
                {childDone}/{childTotal}
              </span>
            )}

            {/* Botões de ação — aparecem ao hover */}
            {canEdit && !isConfirm && (
              <div className="flex flex-shrink-0 items-center gap-0.5 opacity-0 transition-opacity group-hover:opacity-100">
                <button
                  type="button"
                  onClick={() => onStartEdit(path)}
                  title="Editar"
                  className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
                >
                  <Pencil className={depth === 0 ? 'h-3.5 w-3.5' : 'h-3 w-3'} />
                </button>
                <button
                  type="button"
                  onClick={() => onAddChild(path)}
                  title="Adicionar subtarefa"
                  className="rounded p-1 text-gray-400 hover:bg-blue-50 hover:text-blue-600"
                >
                  <Plus className={depth === 0 ? 'h-3.5 w-3.5' : 'h-3 w-3'} />
                </button>
                <button
                  type="button"
                  onClick={() => onRequestDelete(path)}
                  title="Excluir"
                  className="rounded p-1 text-gray-400 hover:bg-red-50 hover:text-red-600"
                >
                  <Trash2 className={depth === 0 ? 'h-3.5 w-3.5' : 'h-3 w-3'} />
                </button>
              </div>
            )}
          </>
        )}
      </div>

      {/* Confirmação de exclusão inline */}
      {isConfirm && (
        <div className="ml-8 mt-0.5 mb-1.5 flex items-center gap-2 rounded-md bg-red-50 px-3 py-1.5 text-xs text-red-700">
          <span className="flex-1">
            Excluir {hasChildren ? 'este item e todos os seus filhos' : 'este item'}?
          </span>
          <button
            type="button"
            onClick={onConfirmDelete}
            className="flex items-center gap-1 rounded bg-red-600 px-2 py-0.5 text-white hover:bg-red-700"
          >
            <Check className="h-3 w-3" /> Sim
          </button>
          <button
            type="button"
            onClick={onCancelDelete}
            className="flex items-center gap-1 rounded border border-red-200 px-2 py-0.5 hover:bg-red-100"
          >
            <X className="h-3 w-3" /> Não
          </button>
        </div>
      )}

      {/* Filhos — renderização recursiva */}
      {hasChildren && (
        <div className="ml-7 border-l border-gray-200 pl-2.5">
          {node.children!.map((child, childIdx) => (
            <ChecklistNodeItem
              key={childIdx}
              node={child}
              path={[...path, childIdx]}
              editingTarget={editingTarget}
              confirmPath={confirmPath}
              editInputRef={editInputRef}
              onToggle={onToggle}
              onStartEdit={onStartEdit}
              onAddChild={onAddChild}
              onRequestDelete={onRequestDelete}
              onCommitEdit={onCommitEdit}
              onCancelEdit={onCancelEdit}
              onConfirmDelete={onConfirmDelete}
              onCancelDelete={onCancelDelete}
              canEdit={canEdit}
              isPending={isPending}
            />
          ))}
        </div>
      )}
    </div>
  );
}

// ─── Componente principal ─────────────────────────────────────────────────────

interface Props {
  progressJson: unknown;
  etapa:        string;
  onSave?:      (items: ChecklistNode[]) => void;
  isPending?:   boolean;
}

export function ImplantacaoChecklist({ progressJson, etapa, onSave, isPending }: Props) {
  const [items, setItems]                 = useState<ChecklistNode[]>(() => parseChecklist(progressJson) ?? []);
  const [editingTarget, setEditingTarget] = useState<EditingTarget | null>(null);
  const [confirmPath,   setConfirmPath]   = useState<number[] | null>(null);
  const editInputRef                      = useRef<HTMLInputElement>(null);

  // Foca no input ao entrar em modo de edição.
  // Input não-controlado → editingTarget nunca muda durante a digitação
  // → este efeito não roda durante a digitação.
  useEffect(() => {
    if (editingTarget !== null && editInputRef.current) {
      const el  = editInputRef.current;
      el.focus();
      const len = el.value.length;
      el.setSelectionRange(len, len);
    }
  }, [editingTarget]);

  const canEdit = !!onSave;

  const { total, done } = countLeaves(items);
  const pct = total > 0 ? Math.round((done / total) * 100) : 0;
  const defaultCount = DEFAULT_CHECKLIST[etapa]?.length ?? 0;

  const applyAndSave = (next: ChecklistNode[]) => {
    setItems(next);
    onSave?.(next);
  };

  // ── Toggle ────────────────────────────────────────────────────────────────

  const handleToggle = (path: number[]) => {
    if (!canEdit || isPending) return;
    const node    = getAtPath(items, path);
    const newDone = !isNodeDone(node);
    applyAndSave(updateAtPath(items, path, n => cascadeDown(n, newDone)));
  };

  // ── Edição ────────────────────────────────────────────────────────────────

  const handleStartEdit = (path: number[]) => {
    setConfirmPath(null);
    setEditingTarget({ path });
  };

  const handleCancelEdit = () => {
    if (editingTarget?.isNew) {
      const { path } = editingTarget;
      setItems(prev => removeAtPath(prev, path));
    }
    setEditingTarget(null);
  };

  const handleCommitEdit = () => {
    if (!editingTarget || !editInputRef.current) return;
    const trimmed = editInputRef.current.value.trim();
    if (!trimmed) { handleCancelEdit(); return; }
    const newItems = updateAtPath(items, editingTarget.path, n => ({ ...n, label: trimmed }));
    setEditingTarget(null);
    applyAndSave(newItems);
  };

  // ── Adicionar filho em qualquer nível ─────────────────────────────────────

  const handleAddChild = (parentPath: number[]) => {
    if (!canEdit || isPending) return;
    const parent      = getAtPath(items, parentPath);
    const newChildIdx = (parent.children?.length ?? 0);
    const newItems    = updateAtPath(items, parentPath, n => ({
      ...n,
      children: [...(n.children ?? []), { label: '', concluido: false }],
    }));
    setItems(newItems);
    setEditingTarget({ path: [...parentPath, newChildIdx], isNew: true });
  };

  const handleAddRoot = () => {
    if (!canEdit || isPending) return;
    const newIdx = items.length;
    setItems(prev => [...prev, { label: '', concluido: false }]);
    setEditingTarget({ path: [newIdx], isNew: true });
  };

  // ── Exclusão ──────────────────────────────────────────────────────────────

  const handleRequestDelete = (path: number[]) => {
    setEditingTarget(null);
    setConfirmPath(path);
  };

  const handleConfirmDelete = () => {
    if (!confirmPath) return;
    applyAndSave(removeAtPath(items, confirmPath));
    setConfirmPath(null);
  };

  const handleCancelDelete = () => setConfirmPath(null);

  // ── Props compartilhadas para o componente recursivo ─────────────────────

  const sharedProps = {
    editingTarget,
    confirmPath,
    editInputRef,
    onToggle:        handleToggle,
    onStartEdit:     handleStartEdit,
    onAddChild:      handleAddChild,
    onRequestDelete: handleRequestDelete,
    onCommitEdit:    handleCommitEdit,
    onCancelEdit:    handleCancelEdit,
    onConfirmDelete: handleConfirmDelete,
    onCancelDelete:  handleCancelDelete,
    canEdit,
    isPending: !!isPending,
  } as const;

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
          <span className="text-sm text-muted-foreground">
            {done}/{total} tarefa{total !== 1 ? 's' : ''} concluída{done !== 1 ? 's' : ''}
          </span>
        ) : defaultCount > 0 ? (
          <span className="text-sm text-muted-foreground">
            {defaultCount} item{defaultCount !== 1 ? 's' : ''} previstos
          </span>
        ) : null}
      </div>

      {/* Estado vazio */}
      {items.length === 0 ? (
        <div className="px-4 py-5 text-center">
          <ClipboardList className="mx-auto mb-2 h-8 w-8 text-gray-200" />
          <p className="text-sm text-muted-foreground">Nenhum checklist cadastrado.</p>
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
            <p className="mt-1 text-right text-xs text-muted-foreground">{pct}% concluído</p>
          </div>

          {/* Árvore de itens */}
          <div className="px-4 pb-3">
            {items.map((item, idx) => (
              <ChecklistNodeItem
                key={idx}
                node={item}
                path={[idx]}
                {...sharedProps}
              />
            ))}
          </div>
        </>
      )}

      {/* Botão adicionar item raiz */}
      {canEdit && (
        <div className="border-t px-4 py-2">
          <button
            type="button"
            onClick={handleAddRoot}
            disabled={!!isPending}
            className="flex items-center gap-1.5 rounded px-2 py-1 text-sm text-muted-foreground hover:bg-gray-50 hover:text-gray-700 disabled:opacity-40"
          >
            <Plus className="h-3.5 w-3.5" />
            Adicionar item
          </button>
        </div>
      )}
    </div>
  );
}
