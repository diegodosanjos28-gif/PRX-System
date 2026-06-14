'use client';
import { useEffect } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { useClientes } from '@/lib/hooks/useClientes';
import { ImplantacaoClienteRequest } from '@/lib/types/api';
import { ImplantacaoCliente } from '@/lib/types/entities';
import { DEFAULT_CHECKLIST } from '@/components/implantacoes/ImplantacaoChecklist';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

interface FormValues {
  clienteId: string;
  etapa: string;
  status: string;
  responsavel: string;
  donoContato: string;
  adquirentes: string;
  observacoes: string;
}

interface Props {
  defaultValues?: Partial<FormValues>;
  onSubmit: (data: ImplantacaoClienteRequest) => void;
  isPending: boolean;
  existingProgressJson?: unknown;
}

// Gera checklist padrão se não houver progressJson já salvo
function buildProgressJson(etapa: string, existing: unknown): unknown {
  // Preserva se já tem conteúdo
  if (existing != null) {
    if (typeof existing === 'string' && existing.trim()) return existing;
    if (typeof existing !== 'string') return existing;
  }
  // Curral não tem checklist padrão
  if (etapa === 'curral') return undefined;
  const labels = DEFAULT_CHECKLIST[etapa] ?? [];
  if (labels.length === 0) return undefined;
  return labels.map((label) => ({ label, concluido: false }));
}

function toFormDefaults(impl?: ImplantacaoCliente): Partial<FormValues> {
  if (!impl) return {};
  return {
    clienteId:   impl.clienteId,
    etapa:       impl.etapa,
    status:      impl.status ?? '',
    responsavel: impl.responsavel ?? '',
    donoContato: impl.donoContato ?? '',
    adquirentes: Array.isArray(impl.adquirentes) ? impl.adquirentes.join(', ') : '',
    observacoes: impl.observacoes ?? '',
  };
}

export { toFormDefaults };

export function ImplantacaoForm({ defaultValues, onSubmit, isPending, existingProgressJson }: Props) {
  const { data: clientes = [] } = useClientes();

  const {
    control,
    register,
    watch,
    setValue,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    defaultValues: {
      clienteId:   '',
      etapa:       'curral',
      status:      '',
      responsavel: '',
      donoContato: '',
      adquirentes: '',
      observacoes: '',
      ...defaultValues,
    },
  });

  const etapa = watch('etapa');

  useEffect(() => {
    if (etapa === 'curral') setValue('status', '');
  }, [etapa, setValue]);

  const handleFormSubmit = (data: FormValues) => {
    const adquirentesArray = data.adquirentes
      ? data.adquirentes.split(',').map((s) => s.trim()).filter(Boolean)
      : [];

    const progressJson = buildProgressJson(data.etapa, existingProgressJson);

    onSubmit({
      clienteId:    data.clienteId,
      etapa:        data.etapa,
      status:       data.etapa === 'curral' ? null : data.status || null,
      responsavel:  data.responsavel || undefined,
      donoContato:  data.donoContato || undefined,
      adquirentes:  adquirentesArray,
      observacoes:  data.observacoes || undefined,
      progressJson: progressJson ?? undefined,
    });
  };

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-4 max-w-lg">
      {/* Cliente */}
      <div className="space-y-1">
        <Label>Cliente *</Label>
        <Controller
          control={control}
          name="clienteId"
          rules={{ required: 'Selecione um cliente' }}
          render={({ field }) => (
            <Select value={field.value} onValueChange={field.onChange}>
              <SelectTrigger>
                <SelectValue placeholder="Selecione um cliente" />
              </SelectTrigger>
              <SelectContent>
                {clientes.map((c) => (
                  <SelectItem key={c.id} value={c.id}>
                    {c.razaoSocial}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
        />
        {errors.clienteId && (
          <p className="text-xs text-red-500">{errors.clienteId.message}</p>
        )}
      </div>

      {/* Etapa + Status */}
      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-1">
          <Label>Etapa *</Label>
          <Controller
            control={control}
            name="etapa"
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue placeholder="Etapa" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="curral">Curral</SelectItem>
                  <SelectItem value="pre">Pré</SelectItem>
                  <SelectItem value="corrida">Corrida</SelectItem>
                  <SelectItem value="onboarding">Onboarding</SelectItem>
                </SelectContent>
              </Select>
            )}
          />
        </div>

        {etapa !== 'curral' && (
          <div className="space-y-1">
            <Label>Status *</Label>
            <Controller
              control={control}
              name="status"
              rules={{ required: 'Status obrigatório' }}
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger>
                    <SelectValue placeholder="Status" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="fluindo">Fluindo</SelectItem>
                    <SelectItem value="aguardando">Aguardando</SelectItem>
                    <SelectItem value="travado">Travado</SelectItem>
                  </SelectContent>
                </Select>
              )}
            />
            {errors.status && (
              <p className="text-xs text-red-500">{errors.status.message}</p>
            )}
          </div>
        )}
      </div>

      {/* Responsável + Dono do Contato */}
      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-1">
          <Label>Responsável</Label>
          <Input {...register('responsavel')} placeholder="Nome do responsável interno" />
        </div>
        <div className="space-y-1">
          <Label>Dono do Contato</Label>
          <Input {...register('donoContato')} placeholder="Nome do contato no cliente" />
        </div>
      </div>

      {/* Adquirentes */}
      <div className="space-y-1">
        <Label>Adquirentes</Label>
        <Input
          {...register('adquirentes')}
          placeholder="Stone, Cielo, Rede (separar por vírgula)"
        />
        <p className="text-xs text-muted-foreground">
          Separe múltiplas adquirentes por vírgula
        </p>
      </div>

      {/* Observações */}
      <div className="space-y-1">
        <Label>Observações</Label>
        <Textarea
          {...register('observacoes')}
          rows={3}
          placeholder="Notas internas sobre a implantação"
        />
      </div>

      <Button type="submit" disabled={isPending}>
        {isPending ? 'Salvando...' : 'Salvar'}
      </Button>
    </form>
  );
}
