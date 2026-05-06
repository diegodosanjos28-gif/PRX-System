'use client';
import { useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { toast } from 'sonner';
import { useCliente } from '@/lib/hooks/useClientes';
import { useEstabelecimentos, useCreateEstabelecimento, useUpdateEstabelecimento } from '@/lib/hooks/useEstabelecimentos';
import { EstabelecimentoTable } from '@/components/estabelecimentos/EstabelecimentoTable';
import { EstabelecimentoForm } from '@/components/estabelecimentos/EstabelecimentoForm';
import { LoadingSpinner } from '@/components/shared/LoadingSpinner';
import { StatusBadge } from '@/components/shared/StatusBadge';
import { Button } from '@/components/ui/button';
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/sheet';
import { Separator } from '@/components/ui/separator';
import { Estabelecimento } from '@/lib/types/entities';
import { EstabelecimentoFormData } from '@/lib/schemas/estabelecimentoSchema';
import Link from 'next/link';

export default function ClienteDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const { data: cliente, isLoading } = useCliente(id);
  const { data: estabelecimentos, isLoading: loadingEst } = useEstabelecimentos(id);
  const { mutate: criar, isPending: criando } = useCreateEstabelecimento(id);
  const { mutate: atualizar, isPending: atualizando } = useUpdateEstabelecimento(id);

  const [sheetOpen, setSheetOpen] = useState(false);
  const [editingEst, setEditingEst] = useState<Estabelecimento | null>(null);

  if (isLoading) return <LoadingSpinner />;
  if (!cliente) return <p>Cliente não encontrado</p>;

  const handleSubmitEst = (data: EstabelecimentoFormData) => {
    if (editingEst) {
      atualizar({ id: editingEst.id, data }, {
        onSuccess: () => { toast.success('Estabelecimento atualizado'); setSheetOpen(false); },
        onError: (err: unknown) => toast.error((err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Erro'),
      });
    } else {
      criar(data, {
        onSuccess: () => { toast.success('Estabelecimento criado'); setSheetOpen(false); },
        onError: (err: unknown) => toast.error((err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Erro'),
      });
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">{cliente.razaoSocial}</h1>
          {cliente.nomeFantasia && <p className="text-muted-foreground">{cliente.nomeFantasia}</p>}
        </div>
        <div className="flex gap-2">
          <StatusBadge label={cliente.ativo ? 'Ativo' : 'Inativo'} variant={cliente.ativo ? 'success' : 'neutral'} />
          <Button variant="outline" asChild><Link href={`/clientes/${id}/editar`}>Editar</Link></Button>
          <Button onClick={() => router.push(`/mensagens?clienteId=${id}`)}>Enviar Mensagem</Button>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-4 text-sm">
        <div><p className="text-muted-foreground">CNPJ</p><p className="font-medium">{cliente.cnpj}</p></div>
        <div><p className="text-muted-foreground">WhatsApp</p><p className="font-medium">{cliente.whatsapp}</p></div>
        {cliente.observacoes && <div><p className="text-muted-foreground">Observações</p><p>{cliente.observacoes}</p></div>}
      </div>

      <Separator />

      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="font-semibold text-lg">Estabelecimentos</h2>
          <Button onClick={() => { setEditingEst(null); setSheetOpen(true); }}>Novo Estabelecimento</Button>
        </div>
        {loadingEst ? <LoadingSpinner /> : (
          <EstabelecimentoTable
            estabelecimentos={estabelecimentos ?? []}
            clienteId={id}
            onEdit={(est) => { setEditingEst(est); setSheetOpen(true); }}
          />
        )}
      </div>

      <Sheet open={sheetOpen} onOpenChange={setSheetOpen}>
        <SheetContent>
          <SheetHeader>
            <SheetTitle>{editingEst ? 'Editar Estabelecimento' : 'Novo Estabelecimento'}</SheetTitle>
          </SheetHeader>
          <div className="mt-4">
            <EstabelecimentoForm
              defaultValues={editingEst ? { descricao: editingEst.descricao, identificadorConciflex: editingEst.identificadorConciflex } : undefined}
              onSubmit={handleSubmitEst}
              isPending={criando || atualizando}
            />
          </div>
        </SheetContent>
      </Sheet>
    </div>
  );
}
