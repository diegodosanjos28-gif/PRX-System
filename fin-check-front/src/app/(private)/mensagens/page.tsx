'use client';
import { Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { MensagemGerarForm } from '@/components/mensagens/MensagemGerarForm';
import { MensagemHistorico } from '@/components/mensagens/MensagemHistorico';
import { useMensagens } from '@/lib/hooks/useMensagens';
import { Separator } from '@/components/ui/separator';
import { LoadingSpinner } from '@/components/shared/LoadingSpinner';
import { EmptyState } from '@/components/shared/EmptyState';

function MensagensContent() {
  const params = useSearchParams();
  const clienteId = params.get('clienteId') ?? '';
  const { data: historico, isLoading } = useMensagens(clienteId);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Mensagens WhatsApp</h1>
      <MensagemGerarForm />
      {clienteId && (
        <>
          <Separator />
          <h2 className="font-semibold text-lg">Histórico</h2>
          {isLoading ? <LoadingSpinner /> : !historico?.length ? <EmptyState message="Nenhuma mensagem enviada" /> : <MensagemHistorico mensagens={historico} />}
        </>
      )}
    </div>
  );
}

export default function MensagensPage() {
  return (
    <Suspense fallback={<LoadingSpinner />}>
      <MensagensContent />
    </Suspense>
  );
}
