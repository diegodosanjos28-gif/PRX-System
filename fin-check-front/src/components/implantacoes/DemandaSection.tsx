'use client';
import { useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { toast } from 'sonner';
import { CheckCircle2, Circle, Trash2, Plus } from 'lucide-react';
import { ImplantacaoDemanda } from '@/lib/types/entities';
import {
  useAddDemanda,
  usePatchDemanda,
  useDeleteDemanda,
} from '@/lib/hooks/useImplantacoes';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { EmptyState } from '@/components/shared/EmptyState';
import { cn } from '@/lib/utils';

interface DemandaFormValues {
  descricao: string;
  prioridade: string;
  adquirente: string;
  tipo: string;
}

const PRIO_CONFIG: Record<string, { label: string; className: string }> = {
  baixa:  { label: 'Baixa',   className: 'bg-gray-500/10 text-gray-500 border-gray-200' },
  media:  { label: 'Média',   className: 'bg-blue-500/10 text-blue-600 border-blue-200' },
  alta:   { label: 'Alta',    className: 'bg-orange-500/10 text-orange-600 border-orange-200' },
  critica:{ label: 'Crítica', className: 'bg-red-500/10 text-red-600 border-red-200' },
};

interface Props {
  implantacaoId: string;
  demandas: ImplantacaoDemanda[];
}

export function DemandaSection({ implantacaoId, demandas }: Props) {
  const [showForm, setShowForm] = useState(false);
  const [deletingDemanda, setDeletingDemanda] = useState<ImplantacaoDemanda | null>(null);

  const { mutate: adicionar, isPending: adicionando } = useAddDemanda(implantacaoId);
  const { mutate: atualizar } = usePatchDemanda(implantacaoId);
  const { mutate: deletar, isPending: deletando } = useDeleteDemanda(implantacaoId);

  const { control, register, handleSubmit, reset, formState: { errors } } =
    useForm<DemandaFormValues>({
      defaultValues: {
        descricao:   '',
        prioridade:  'media',
        adquirente:  '',
        tipo:        'pista',
      },
    });

  const handleAdd = (data: DemandaFormValues) => {
    adicionar(
      {
        descricao:  data.descricao,
        prioridade: data.prioridade,
        adquirente: data.adquirente || undefined,
        tipo:       data.tipo,
      },
      {
        onSuccess: () => {
          toast.success('Demanda adicionada');
          reset();
          setShowForm(false);
        },
        onError: () => toast.error('Erro ao adicionar demanda'),
      },
    );
  };

  const toggleConcluida = (demanda: ImplantacaoDemanda) => {
    atualizar(
      { demandaId: demanda.id, data: { concluida: !demanda.concluida } },
      { onError: () => toast.error('Erro ao atualizar demanda') },
    );
  };

  const handleDelete = (demandaId: string) => {
    deletar(demandaId, {
      onSuccess: () => {
        setDeletingDemanda(null);
        toast.success('Demanda removida');
      },
      onError: () => toast.error('Erro ao remover demanda'),
    });
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="font-semibold text-lg">Demandas ({demandas.length})</h2>
        <Button
          size="sm"
          variant="outline"
          onClick={() => setShowForm(!showForm)}
        >
          <Plus className="h-4 w-4 mr-1" />
          Nova Demanda
        </Button>
      </div>

      {showForm && (
        <form
          onSubmit={handleSubmit(handleAdd)}
          className="border rounded-lg p-4 space-y-3 bg-muted/30"
        >
          <div className="space-y-1">
            <Label>Descrição *</Label>
            <Input
              {...register('descricao', { required: 'Descrição obrigatória' })}
              placeholder="Descreva a demanda..."
            />
            {errors.descricao && (
              <p className="text-xs text-red-500">{errors.descricao.message}</p>
            )}
          </div>

          <div className="grid grid-cols-3 gap-3">
            <div className="space-y-1">
              <Label>Prioridade</Label>
              <Controller
                control={control}
                name="prioridade"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="baixa">Baixa</SelectItem>
                      <SelectItem value="media">Média</SelectItem>
                      <SelectItem value="alta">Alta</SelectItem>
                      <SelectItem value="critica">Crítica</SelectItem>
                    </SelectContent>
                  </Select>
                )}
              />
            </div>

            <div className="space-y-1">
              <Label>Tipo</Label>
              <Controller
                control={control}
                name="tipo"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="pista">Pista</SelectItem>
                      <SelectItem value="curral">Curral</SelectItem>
                    </SelectContent>
                  </Select>
                )}
              />
            </div>

            <div className="space-y-1">
              <Label>Adquirente</Label>
              <Input {...register('adquirente')} placeholder="Ex: Stone" />
            </div>
          </div>

          <div className="flex gap-2">
            <Button type="submit" size="sm" disabled={adicionando}>
              {adicionando ? 'Adicionando...' : 'Adicionar'}
            </Button>
            <Button
              type="button"
              size="sm"
              variant="ghost"
              onClick={() => {
                reset();
                setShowForm(false);
              }}
            >
              Cancelar
            </Button>
          </div>
        </form>
      )}

      {demandas.length === 0 ? (
        <EmptyState />
      ) : (
        <div className="space-y-2">
          {demandas.map((d) => {
            const prio = PRIO_CONFIG[d.prioridade] ?? {
              label: d.prioridade,
              className: '',
            };
            return (
              <div
                key={d.id}
                className={cn(
                  'flex items-start gap-3 rounded-lg border p-3 transition-opacity',
                  d.concluida && 'opacity-60',
                )}
              >
                <button
                  type="button"
                  onClick={() => toggleConcluida(d)}
                  className="mt-0.5 flex-shrink-0 text-muted-foreground hover:text-foreground transition-colors"
                  title={d.concluida ? 'Reabrir' : 'Concluir'}
                >
                  {d.concluida ? (
                    <CheckCircle2 className="h-5 w-5 text-green-600" />
                  ) : (
                    <Circle className="h-5 w-5" />
                  )}
                </button>

                <div className="flex-1 min-w-0">
                  <p
                    className={cn(
                      'text-sm font-medium',
                      d.concluida && 'line-through text-muted-foreground',
                    )}
                  >
                    {d.descricao}
                  </p>
                  <div className="flex items-center gap-2 mt-1 flex-wrap">
                    <Badge variant="outline" className={cn('text-xs', prio.className)}>
                      {prio.label}
                    </Badge>
                    <span className="text-xs text-muted-foreground capitalize">
                      {d.tipo}
                    </span>
                    {d.adquirente && (
                      <span className="text-xs text-muted-foreground">{d.adquirente}</span>
                    )}
                  </div>
                </div>

                <Button
                  variant="ghost"
                  size="icon"
                  className="h-7 w-7 flex-shrink-0 text-destructive hover:text-destructive"
                  onClick={() => setDeletingDemanda(d)}
                  title="Remover demanda"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </Button>
              </div>
            );
          })}
        </div>
      )}

      <Dialog
        open={deletingDemanda !== null}
        onOpenChange={(open) => !open && setDeletingDemanda(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Excluir demanda?</DialogTitle>
            <DialogDescription>
              A demanda{' '}
              <strong className="font-medium text-foreground">
                &ldquo;{deletingDemanda?.descricao}&rdquo;
              </strong>{' '}
              será removida permanentemente. Esta ação não pode ser desfeita.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setDeletingDemanda(null)}
              disabled={deletando}
            >
              Cancelar
            </Button>
            <Button
              variant="destructive"
              onClick={() => deletingDemanda && handleDelete(deletingDemanda.id)}
              disabled={deletando}
            >
              {deletando ? 'Excluindo…' : 'Excluir demanda'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
