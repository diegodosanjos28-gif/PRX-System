'use client';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { toast } from 'sonner';
import { Edit, Trash2, Eye } from 'lucide-react';
import { Cliente } from '@/lib/types/entities';
import { useDeleteCliente } from '@/lib/hooks/useClientes';
import { StatusBadge } from '@/components/shared/StatusBadge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table';
import {
  Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle,
} from '@/components/ui/dialog';

export function ClienteTable({ clientes }: { clientes: Cliente[] }) {
  const router = useRouter();
  const { mutate: deletar, isPending } = useDeleteCliente();
  const [search, setSearch] = useState('');
  const [deleteId, setDeleteId] = useState<string | null>(null);

  const filtered = clientes.filter((c) =>
    c.razaoSocial.toLowerCase().includes(search.toLowerCase()) ||
    c.cnpj.includes(search)
  );

  const handleDelete = () => {
    if (!deleteId) return;
    deletar(deleteId, {
      onSuccess: () => { toast.success('Cliente inativado'); setDeleteId(null); },
      onError: () => toast.error('Erro ao inativar cliente'),
    });
  };

  return (
    <div className="space-y-4">
      <Input
        placeholder="Buscar por nome ou CNPJ..."
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        className="max-w-sm"
      />
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Razão Social</TableHead>
            <TableHead>CNPJ</TableHead>
            <TableHead>WhatsApp</TableHead>
            <TableHead>Status</TableHead>
            <TableHead>Rel. Diário</TableHead>
            <TableHead className="text-right">Ações</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {filtered.map((c) => (
            <TableRow key={c.id}>
              <TableCell className="font-medium">{c.razaoSocial}</TableCell>
              <TableCell>{c.cnpj}</TableCell>
              <TableCell>{c.whatsapp}</TableCell>
              <TableCell>
                <StatusBadge label={c.ativo ? 'Ativo' : 'Inativo'} variant={c.ativo ? 'success' : 'neutral'} />
              </TableCell>
              <TableCell>
                <StatusBadge
                  label={c.relatorioDiarioAtivo ? 'Sim' : 'Não'}
                  variant={c.relatorioDiarioAtivo ? 'success' : 'neutral'}
                />
              </TableCell>
              <TableCell className="text-right space-x-1">
                <Button variant="ghost" size="icon" onClick={() => router.push(`/clientes/${c.id}`)}>
                  <Eye className="h-4 w-4" />
                </Button>
                <Button variant="ghost" size="icon" onClick={() => router.push(`/clientes/${c.id}/editar`)}>
                  <Edit className="h-4 w-4" />
                </Button>
                <Button variant="ghost" size="icon" onClick={() => setDeleteId(c.id)}>
                  <Trash2 className="h-4 w-4 text-red-500" />
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <Dialog open={!!deleteId} onOpenChange={(o) => !o && setDeleteId(null)}>
        <DialogContent>
          <DialogHeader><DialogTitle>Inativar cliente?</DialogTitle></DialogHeader>
          <p className="text-sm text-muted-foreground">Esta ação irá inativar o cliente. Você pode reativá-lo posteriormente.</p>
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
