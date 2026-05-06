'use client';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { toast } from 'sonner';
import { Edit, Trash2, BarChart2, DollarSign } from 'lucide-react';
import { Estabelecimento } from '@/lib/types/entities';
import { useDeleteEstabelecimento } from '@/lib/hooks/useEstabelecimentos';
import { StatusBadge } from '@/components/shared/StatusBadge';
import { Button } from '@/components/ui/button';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';

interface Props {
  estabelecimentos: Estabelecimento[];
  clienteId: string;
  onEdit: (est: Estabelecimento) => void;
}

export function EstabelecimentoTable({ estabelecimentos, clienteId, onEdit }: Props) {
  const router = useRouter();
  const { mutate: deletar, isPending } = useDeleteEstabelecimento(clienteId);
  const [deleteId, setDeleteId] = useState<string | null>(null);

  const handleDelete = () => {
    if (!deleteId) return;
    deletar(deleteId, {
      onSuccess: () => { toast.success('Estabelecimento inativado'); setDeleteId(null); },
      onError: () => toast.error('Erro ao inativar estabelecimento'),
    });
  };

  return (
    <div>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Descrição</TableHead>
            <TableHead>Identificador Conciflex</TableHead>
            <TableHead>Status</TableHead>
            <TableHead className="text-right">Ações</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {estabelecimentos.map((e) => (
            <TableRow key={e.id}>
              <TableCell>{e.descricao}</TableCell>
              <TableCell className="font-mono text-sm">{e.identificadorConciflex}</TableCell>
              <TableCell>
                <StatusBadge label={e.ativo ? 'Ativo' : 'Inativo'} variant={e.ativo ? 'success' : 'neutral'} />
              </TableCell>
              <TableCell className="text-right space-x-1">
                <Button variant="ghost" size="icon" title="Auditoria" onClick={() => router.push(`/auditoria/${e.id}`)}>
                  <BarChart2 className="h-4 w-4" />
                </Button>
                <Button variant="ghost" size="icon" title="Recebimentos" onClick={() => router.push(`/recebimentos/${e.id}`)}>
                  <DollarSign className="h-4 w-4" />
                </Button>
                <Button variant="ghost" size="icon" onClick={() => onEdit(e)}>
                  <Edit className="h-4 w-4" />
                </Button>
                <Button variant="ghost" size="icon" onClick={() => setDeleteId(e.id)}>
                  <Trash2 className="h-4 w-4 text-red-500" />
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <Dialog open={!!deleteId} onOpenChange={(o) => !o && setDeleteId(null)}>
        <DialogContent>
          <DialogHeader><DialogTitle>Inativar estabelecimento?</DialogTitle></DialogHeader>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setDeleteId(null)}>Cancelar</Button>
            <Button variant="destructive" onClick={handleDelete} disabled={isPending}>
              {isPending ? 'Inativando...' : 'Inativar'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
