'use client';
import { useRouter } from 'next/navigation';
import { toast } from 'sonner';
import { useCreateImplantacao } from '@/lib/hooks/useImplantacoes';
import { ImplantacaoForm } from '@/components/implantacoes/ImplantacaoForm';
import { ImplantacaoClienteRequest } from '@/lib/types/api';

export default function NovaImplantacaoPage() {
  const router = useRouter();
  const { mutate, isPending } = useCreateImplantacao();

  const onSubmit = (data: ImplantacaoClienteRequest) => {
    mutate(data, {
      onSuccess: (created) => {
        toast.success('Implantação criada com sucesso');
        router.push(`/implantacoes/${created.id}`);
      },
      onError: (err: unknown) =>
        toast.error(
          (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
            'Erro ao criar implantação',
        ),
    });
  };

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Nova Implantação</h1>
      <ImplantacaoForm onSubmit={onSubmit} isPending={isPending} />
    </div>
  );
}
