'use client';

import { useState } from 'react';
import { toast } from 'sonner';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useDespacharCliente } from '@/lib/hooks/useClientes';
import { ImplantacaoCliente } from '@/lib/types/entities';

/** Palavra que o operador precisa digitar para liberar a exclusão. */
const PALAVRA_CONFIRMACAO = 'EXCLUIR';

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Somente os cavalos atualmente no Curral. */
  candidatos: ImplantacaoCliente[];
}

function nomeDoCliente(i: ImplantacaoCliente) {
  return i.clienteNomeFantasia?.trim() || i.clienteRazaoSocial;
}

/**
 * Despacha um cliente do Curral — exclusão DEFINITIVA no Sistema PRX.
 *
 * <p>Duas barreiras antes de qualquer chamada: escolher o cliente e digitar
 * {@link PALAVRA_CONFIRMACAO}. Nada é removido da tela antes da resposta do backend;
 * em caso de erro o Curral permanece exatamente como estava.
 */
export function ImplantacaoDespacharDialog({ open, onOpenChange, candidatos }: Props) {
  const [selecionadoId, setSelecionadoId] = useState<string | null>(null);
  const [confirmacao, setConfirmacao] = useState('');
  const despachar = useDespacharCliente();

  const selecionado = candidatos.find((c) => c.clienteId === selecionadoId) ?? null;
  const confirmacaoValida = confirmacao.trim().toUpperCase() === PALAVRA_CONFIRMACAO;
  const podeExcluir = !!selecionado && confirmacaoValida && !despachar.isPending;

  function limpar() {
    setSelecionadoId(null);
    setConfirmacao('');
  }

  function handleOpenChange(next: boolean) {
    // Fechar durante o envio deixaria a operação sem feedback: bloqueia até terminar.
    if (despachar.isPending) return;
    if (!next) limpar();
    onOpenChange(next);
  }

  function handleExcluir() {
    if (!selecionado || !podeExcluir) return;

    const nome = nomeDoCliente(selecionado);
    despachar.mutate(selecionado.clienteId, {
      onSuccess: () => {
        limpar();
        onOpenChange(false);
        toast.success(`${nome} foi despachado e excluído do Sistema PRX`);
      },
      onError: () => {
        // O Curral não é alterado: o cavalo continua visível para nova tentativa.
        toast.error(`Não foi possível despachar ${nome}. Nenhum dado foi removido.`);
      },
    });
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-[520px]">
        <DialogHeader>
          <DialogTitle>Despachar cliente do Curral</DialogTitle>
          <DialogDescription>
            Selecione o cliente que será despachado. Esta ação exclui o cliente do Sistema PRX.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="despachar-cliente">Cliente no Curral</Label>
            <select
              id="despachar-cliente"
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
              value={selecionadoId ?? ''}
              disabled={despachar.isPending}
              onChange={(e) => {
                setSelecionadoId(e.target.value || null);
                // Trocar de cliente invalida a confirmação já digitada.
                setConfirmacao('');
              }}
            >
              <option value="">Selecione um cliente…</option>
              {candidatos.map((c) => (
                <option key={c.clienteId} value={c.clienteId}>
                  {nomeDoCliente(c)}
                </option>
              ))}
            </select>
          </div>

          {selecionado && (
            <div className="rounded-md border border-destructive/40 bg-destructive/5 p-3 space-y-2">
              <p className="text-sm font-semibold text-destructive">
                Excluir definitivamente {nomeDoCliente(selecionado)} do Sistema PRX?
              </p>
              <p className="text-xs text-muted-foreground">
                Esta ação removerá permanentemente o cliente e seus dados vinculados do
                Sistema PRX. Esta ação não poderá ser desfeita.
              </p>

              <div className="space-y-1 pt-1">
                <Label htmlFor="despachar-confirmacao" className="text-xs">
                  Digite <span className="font-mono font-semibold">{PALAVRA_CONFIRMACAO}</span> para
                  liberar a exclusão
                </Label>
                <Input
                  id="despachar-confirmacao"
                  autoComplete="off"
                  value={confirmacao}
                  disabled={despachar.isPending}
                  placeholder={PALAVRA_CONFIRMACAO}
                  onChange={(e) => setConfirmacao(e.target.value)}
                />
              </div>
            </div>
          )}
        </div>

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            disabled={despachar.isPending}
            onClick={() => handleOpenChange(false)}
          >
            Cancelar
          </Button>
          <Button
            type="button"
            variant="destructive"
            disabled={!podeExcluir}
            onClick={handleExcluir}
          >
            {despachar.isPending ? 'Excluindo…' : 'Excluir definitivamente'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
