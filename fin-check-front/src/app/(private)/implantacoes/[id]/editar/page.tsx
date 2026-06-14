'use client';
import { useParams, useRouter } from 'next/navigation';
import { toast } from 'sonner';
import { useImplantacao, useUpdateImplantacao } from '@/lib/hooks/useImplantacoes';
import { ImplantacaoForm, toFormDefaults } from '@/components/implantacoes/ImplantacaoForm';
import { LoadingSpinner } from '@/components/shared/LoadingSpinner';
import { ImplantacaoClienteRequest } from '@/lib/types/api';

export default function EditarImplantacaoPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();

  const { data: impl, isLoading } = useImplantacao(id);
  const { mutate, isPending } = useUpdateImplantacao();

  if (isLoading) return <LoadingSpinner />;
  if (!impl) return <p className="text-muted-foreground">Implantação não encontrada.</p>;

  const onSubmit = (data: ImplantacaoClienteRequest) => {
    mutate(
      { id, data },
      {
        onSuccess: () => {
          toast.success('Implantação atualizada com sucesso');
          router.push(`/implantacoes/${id}`);
        },
        onError: (err: unknown) =>
          toast.error(
            (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
              'Erro ao atualizar implantação',
          ),
      },
    );
  };

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Editar Implantação</h1>
      <p className="text-muted-foreground">{impl.clienteRazaoSocial}</p>
      <ImplantacaoForm
        defaultValues={toFormDefaults(impl)}
        onSubmit={onSubmit}
        isPending={isPending}
        existingProgressJson={impl.progressJson}
      />
    </div>
  );
}
