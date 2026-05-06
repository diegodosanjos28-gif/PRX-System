'use client';
import { useRouter } from 'next/navigation';
import { toast } from 'sonner';
import { useCreateCliente } from '@/lib/hooks/useClientes';
import { ClienteForm } from '@/components/clientes/ClienteForm';
import { ClienteFormData } from '@/lib/schemas/clienteSchema';

export default function NovoClientePage() {
  const router = useRouter();
  const { mutate, isPending } = useCreateCliente();

  const onSubmit = (data: ClienteFormData) => {
    mutate(data, {
      onSuccess: () => { toast.success('Cliente criado com sucesso'); router.push('/clientes'); },
      onError: (err: unknown) => toast.error((err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Erro ao criar cliente'),
    });
  };

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Novo Cliente</h1>
      <ClienteForm onSubmit={onSubmit} isPending={isPending} />
    </div>
  );
}
