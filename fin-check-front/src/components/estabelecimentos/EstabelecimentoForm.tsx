'use client';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { estabelecimentoSchema, EstabelecimentoFormData } from '@/lib/schemas/estabelecimentoSchema';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

interface Props {
  defaultValues?: Partial<EstabelecimentoFormData>;
  onSubmit: (data: EstabelecimentoFormData) => void;
  isPending: boolean;
}

export function EstabelecimentoForm({ defaultValues, onSubmit, isPending }: Props) {
  const { register, handleSubmit, formState: { errors } } = useForm<EstabelecimentoFormData>({
    resolver: zodResolver(estabelecimentoSchema),
    defaultValues,
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="space-y-1">
        <Label>Descrição *</Label>
        <Input {...register('descricao')} placeholder="Ex: Loja Principal" />
        {errors.descricao && <p className="text-xs text-red-500">{errors.descricao.message}</p>}
      </div>
      <div className="space-y-1">
        <Label>Identificador Conciflex *</Label>
        <Input {...register('identificadorConciflex')} placeholder="Ex: 12345678000100" />
        {errors.identificadorConciflex && <p className="text-xs text-red-500">{errors.identificadorConciflex.message}</p>}
      </div>
      <Button type="submit" disabled={isPending}>
        {isPending ? 'Salvando...' : 'Salvar'}
      </Button>
    </form>
  );
}
