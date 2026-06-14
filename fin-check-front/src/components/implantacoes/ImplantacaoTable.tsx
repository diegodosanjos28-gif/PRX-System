'use client';
import { useState } from 'react';
import Link from 'next/link';
import { Eye, Pencil, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { ImplantacaoCliente } from '@/lib/types/entities';
import { useDeleteImplantacao } from '@/lib/hooks/useImplantacoes';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

const ETAPA_CONFIG: Record<string, { label: string; className: string }> = {
  curral:     { label: 'Curral',     className: 'bg-gray-500/10 text-gray-500 border-gray-200' },
  pre:        { label: 'Pré',        className: 'bg-blue-500/10 text-blue-600 border-blue-200' },
  corrida:    { label: 'Corrida',    className: 'bg-orange-500/10 text-orange-600 border-orange-200' },
  onboarding: { label: 'Onboarding', className: 'bg-purple-500/10 text-purple-600 border-purple-200' },
};

const STATUS_CONFIG: Record<string, { label: string; className: string }> = {
  fluindo:   { label: 'Fluindo',    className: 'bg-green-500/10 text-green-600 border-green-200' },
  aguardando:{ label: 'Aguardando', className: 'bg-yellow-500/10 text-yellow-600 border-yellow-200' },
  travado:   { label: 'Travado',    className: 'bg-red-500/10 text-red-600 border-red-200' },
};

function fmtDate(val: string | null) {
  if (!val) return '—';
  return new Date(val).toLocaleDateString('pt-BR');
}

interface Props {
  implantacoes: ImplantacaoCliente[];
}

export function ImplantacaoTable({ implantacoes }: Props) {
  const { mutate: deletar, isPending: deletando } = useDeleteImplantacao();
  const [search, setSearch] = useState('');
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const filtered = implantacoes.filter((i) => {
    const q = search.toLowerCase();
    return (
      i.clienteRazaoSocial.toLowerCase().includes(q) ||
      (i.clienteNomeFantasia ?? '').toLowerCase().includes(q)
    );
  });

  const handleDelete = () => {
    if (!deletingId) return;
    deletar(deletingId, {
      onSuccess: () => {
        toast.success('Implantação removida');
        setDeletingId(null);
      },
      onError: (err: unknown) =>
        toast.error(
          (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
            'Erro ao remover implantação',
        ),
    });
  };

  return (
    <>
      <div className="mb-3">
        <Input
          placeholder="Buscar por cliente..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-72"
        />
      </div>

      <div className="overflow-x-auto rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Cliente</TableHead>
              <TableHead>Etapa</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Responsável</TableHead>
              <TableHead>Último Movimento</TableHead>
              <TableHead className="w-28" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {filtered.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={6}
                  className="text-center text-muted-foreground py-8"
                >
                  Nenhuma implantação encontrada.
                </TableCell>
              </TableRow>
            ) : (
              filtered.map((impl) => {
                const etapa = ETAPA_CONFIG[impl.etapa] ?? {
                  label: impl.etapa,
                  className: '',
                };
                const status = impl.status ? STATUS_CONFIG[impl.status] : null;

                return (
                  <TableRow key={impl.id}>
                    <TableCell>
                      <p className="font-medium">{impl.clienteRazaoSocial}</p>
                      {impl.clienteNomeFantasia && (
                        <p className="text-xs text-muted-foreground">
                          {impl.clienteNomeFantasia}
                        </p>
                      )}
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline" className={etapa.className}>
                        {etapa.label}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      {status ? (
                        <Badge variant="outline" className={status.className}>
                          {status.label}
                        </Badge>
                      ) : (
                        <span className="text-muted-foreground">—</span>
                      )}
                    </TableCell>
                    <TableCell>{impl.responsavel ?? '—'}</TableCell>
                    <TableCell>{fmtDate(impl.ultimoMovimento)}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-1">
                        <Button variant="ghost" size="icon" asChild>
                          <Link href={`/implantacoes/${impl.id}`}>
                            <Eye className="h-4 w-4" />
                          </Link>
                        </Button>
                        <Button variant="ghost" size="icon" asChild>
                          <Link href={`/implantacoes/${impl.id}/editar`}>
                            <Pencil className="h-4 w-4" />
                          </Link>
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="text-destructive hover:text-destructive"
                          onClick={() => setDeletingId(impl.id)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })
            )}
          </TableBody>
        </Table>
      </div>

      <Dialog open={!!deletingId} onOpenChange={(open) => !open && setDeletingId(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Remover implantação</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            Essa ação é irreversível. Todas as demandas vinculadas também serão removidas.
          </p>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeletingId(null)}>
              Cancelar
            </Button>
            <Button variant="destructive" onClick={handleDelete} disabled={deletando}>
              {deletando ? 'Removendo...' : 'Remover'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
