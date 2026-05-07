'use client';
import { useParams, useRouter } from 'next/navigation';
import { toast } from 'sonner';
import { useCliente, useUpdateCliente } from '@/lib/hooks/useClientes';
import { ClienteForm } from '@/components/clientes/ClienteForm';
import { LoadingSpinner } from '@/components/shared/LoadingSpinner';
import { ClienteFormData } from '@/lib/schemas/clienteSchema';

export default function EditarClientePage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const { data: cliente, isLoading } = useCliente(id);
  const { mutate, isPending } = useUpdateCliente();

  if (isLoading) return <LoadingSpinner />;
  if (!cliente) return <p>Cliente não encontrado</p>;

  const onSubmit = (data: ClienteFormData) => {
    mutate({ id, data }, {
      onSuccess: () => { toast.success('Cliente atualizado'); router.push(`/clientes/${id}`); },
      onError: (err: unknown) => toast.error((err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Erro ao atualizar'),
    });
  };

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Editar Cliente</h1>
      <ClienteForm
          defaultValues={{
            razaoSocial: cliente.razaoSocial,
            nomeFantasia: cliente.nomeFantasia,
            cnpj: cliente.cnpj,
            whatsapp: cliente.whatsapp,
            observacoes: cliente.observacoes,
            conciflexLogin: cliente.conciflexLogin,
            conciflexSenha: cliente.conciflexSenha,
          }}
          onSubmit={onSubmit}
          isPending={isPending}
      />
    </div>
  );
}
