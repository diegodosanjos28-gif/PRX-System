'use client';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import {
  useTemplateVariaveis,
  useCreateTemplateVariavel,
  useUpdateTemplateVariavel,
  useDeleteTemplateVariavel,
} from '@/lib/hooks/useTemplates';
import { TemplateVariavel } from '@/lib/types/entities';
import { TemplateVariavelRequest } from '@/lib/types/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { toast } from 'sonner';
import { Pencil, Trash2, Plus, Lock } from 'lucide-react';

type FormData = { chave: string; descricao: string };

function VariavelForm({
  defaultValues,
  onSubmit,
  onCancel,
  submitting,
}: {
  defaultValues?: Partial<FormData>;
  onSubmit: (data: FormData) => void;
  onCancel: () => void;
  submitting: boolean;
}) {
  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({ defaultValues });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="space-y-1">
        <Label>Chave</Label>
        <Input
          {...register('chave', { required: 'Chave é obrigatória' })}
          placeholder="{minhaVariavel}"
          className="font-mono"
        />
        <p className="text-xs text-muted-foreground">Use o formato {'{chave}'} com chaves.</p>
        {errors.chave && <p className="text-xs text-red-500">{errors.chave.message}</p>}
      </div>
      <div className="space-y-1">
        <Label>Descrição</Label>
        <Input {...register('descricao', { required: 'Descrição é obrigatória' })} />
        {errors.descricao && <p className="text-xs text-red-500">{errors.descricao.message}</p>}
      </div>
      <div className="flex gap-2 justify-end">
        <Button type="button" variant="outline" onClick={onCancel}>Cancelar</Button>
        <Button type="submit" disabled={submitting}>{submitting ? 'Salvando...' : 'Salvar'}</Button>
      </div>
    </form>
  );
}

export function VariaveisTab() {
  const { data: variaveis, isLoading } = useTemplateVariaveis();
  const { mutate: criar, isPending: criando } = useCreateTemplateVariavel();
  const { mutate: atualizar, isPending: atualizando } = useUpdateTemplateVariavel();
  const { mutate: excluir } = useDeleteTemplateVariavel();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<TemplateVariavel | null>(null);

  const openCreate = () => { setEditTarget(null); setDialogOpen(true); };
  const openEdit = (v: TemplateVariavel) => { setEditTarget(v); setDialogOpen(true); };
  const closeDialog = () => { setDialogOpen(false); setEditTarget(null); };

  const handleSubmit = (data: FormData) => {
    const request: TemplateVariavelRequest = { chave: data.chave, descricao: data.descricao };
    if (editTarget) {
      atualizar({ id: editTarget.id, data: request }, {
        onSuccess: () => { toast.success('Variável atualizada'); closeDialog(); },
        onError: () => toast.error('Erro ao atualizar variável'),
      });
    } else {
      criar(request, {
        onSuccess: () => { toast.success('Variável criada'); closeDialog(); },
        onError: () => toast.error('Erro ao criar variável'),
      });
    }
  };

  const handleExcluir = (v: TemplateVariavel) => {
    if (v.sistemaFixo) return;
    if (!confirm(`Excluir variável "${v.chave}"?`)) return;
    excluir(v.id, {
      onSuccess: () => toast.success('Variável excluída'),
      onError: (err: unknown) => toast.error((err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Erro ao excluir variável'),
    });
  };

  if (isLoading) return <p className="text-sm text-muted-foreground">Carregando...</p>;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          Variáveis marcadas com <Lock className="inline h-3 w-3" /> são do sistema e não podem ser excluídas.
        </p>
        <Button onClick={openCreate} size="sm" className="gap-2">
          <Plus className="h-4 w-4" /> Nova Variável
        </Button>
      </div>

      {!variaveis?.length ? (
        <p className="text-sm text-muted-foreground">Nenhuma variável cadastrada.</p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Chave</TableHead>
              <TableHead>Descrição</TableHead>
              <TableHead>Tipo</TableHead>
              <TableHead className="text-right">Ações</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {variaveis.map((v) => (
              <TableRow key={v.id}>
                <TableCell className="font-mono text-sm">{v.chave}</TableCell>
                <TableCell className="text-sm">{v.descricao}</TableCell>
                <TableCell>
                  {v.sistemaFixo ? (
                    <Badge variant="secondary" className="gap-1">
                      <Lock className="h-3 w-3" /> Sistema
                    </Badge>
                  ) : (
                    <Badge variant="outline">Personalizada</Badge>
                  )}
                </TableCell>
                <TableCell className="text-right">
                  <div className="flex gap-2 justify-end">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => openEdit(v)}
                      disabled={v.sistemaFixo}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleExcluir(v)}
                      disabled={v.sistemaFixo}
                      className={v.sistemaFixo ? 'opacity-30' : 'text-red-500 hover:text-red-700'}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      <Dialog open={dialogOpen} onOpenChange={closeDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editTarget ? 'Editar Variável' : 'Nova Variável'}</DialogTitle>
          </DialogHeader>
          <VariavelForm
            defaultValues={editTarget ?? undefined}
            onSubmit={handleSubmit}
            onCancel={closeDialog}
            submitting={criando || atualizando}
          />
        </DialogContent>
      </Dialog>
    </div>
  );
}
