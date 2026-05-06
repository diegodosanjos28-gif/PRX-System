'use client';
import Link from 'next/link';
import { useClientes } from '@/lib/hooks/useClientes';
import { ClienteTable } from '@/components/clientes/ClienteTable';
import { LoadingSpinner } from '@/components/shared/LoadingSpinner';
import { EmptyState } from '@/components/shared/EmptyState';
import { Button } from '@/components/ui/button';

export default function ClientesPage() {
  const { data, isLoading } = useClientes();
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Clientes</h1>
        <Button asChild><Link href="/clientes/novo">Novo Cliente</Link></Button>
      </div>
      {isLoading ? <LoadingSpinner /> : !data?.length ? <EmptyState /> : <ClienteTable clientes={data} />}
    </div>
  );
}
