'use client';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { clienteSchema, ClienteFormData } from '@/lib/schemas/clienteSchema';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { ShieldCheck } from 'lucide-react';

interface Props {
  defaultValues?: Partial<ClienteFormData>;
  onSubmit: (data: ClienteFormData) => void;
  isPending: boolean;
}

export function ClienteForm({ defaultValues, onSubmit, isPending }: Props) {
  const { register, handleSubmit, formState: { errors } } = useForm<ClienteFormData>({
    resolver: zodResolver(clienteSchema),
    defaultValues,
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 max-w-lg">
      <div className="space-y-1">
        <Label>Razão Social *</Label>
        <Input {...register('razaoSocial')} />
        {errors.razaoSocial && <p className="text-xs text-red-500">{errors.razaoSocial.message}</p>}
      </div>

      <div className="space-y-1">
        <Label>Nome Fantasia</Label>
        <Input {...register('nomeFantasia')} />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-1">
          <Label>CNPJ * (14 dígitos)</Label>
          <Input {...register('cnpj')} maxLength={14} placeholder="00000000000000" />
          {errors.cnpj && <p className="text-xs text-red-500">{errors.cnpj.message}</p>}
        </div>
        <div className="space-y-1">
          <Label>WhatsApp *</Label>
          <Input {...register('whatsapp')} placeholder="5547999999999" />
          {errors.whatsapp && <p className="text-xs text-red-500">{errors.whatsapp.message}</p>}
        </div>
      </div>

      <div className="space-y-2 rounded-md border p-4 bg-muted/30">
        <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
          <ShieldCheck className="h-4 w-4" />
          Credenciais Conciflex — armazenadas com criptografia AES-256
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-1">
            <Label>Login Conciflex *</Label>
            <Input {...register('conciflexLogin')} />
            {errors.conciflexLogin && <p className="text-xs text-red-500">{errors.conciflexLogin.message}</p>}
          </div>
          <div className="space-y-1">
            <Label>Senha Conciflex *</Label>
            <Input type="password" {...register('conciflexSenha')} />
            {errors.conciflexSenha && <p className="text-xs text-red-500">{errors.conciflexSenha.message}</p>}
          </div>
        </div>
      </div>

      <div className="space-y-1">
        <Label>Observações</Label>
        <Textarea {...register('observacoes')} rows={3} />
      </div>

      <Button type="submit" disabled={isPending}>
        {isPending ? 'Salvando...' : 'Salvar'}
      </Button>
    </form>
  );
}
