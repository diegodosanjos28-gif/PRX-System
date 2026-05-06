'use client';
import { useState } from 'react';
import { useClientes } from '@/lib/hooks/useClientes';
import { useEstabelecimentos } from '@/lib/hooks/useEstabelecimentos';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Label } from '@/components/ui/label';

interface Props {
  onSelect: (estabelecimentoId: string) => void;
}

export function EstabelecimentoSelector({ onSelect }: Props) {
  const [clienteId, setClienteId] = useState('');
  const [estabelecimentoId, setEstabelecimentoId] = useState('');

  const { data: clientes, isLoading: loadingClientes } = useClientes();
  const { data: estabelecimentos, isLoading: loadingEstab } = useEstabelecimentos(clienteId);

  const handleClienteChange = (value: string) => {
    setClienteId(value);
    setEstabelecimentoId('');
    onSelect('');
  };

  const handleEstabelecimentoChange = (value: string) => {
    setEstabelecimentoId(value);
    onSelect(value);
  };

  const activeClientes = clientes?.filter((c) => c.ativo) ?? [];
  const activeEstabelecimentos = estabelecimentos?.filter((e) => e.ativo) ?? [];

  return (
    <div className="flex gap-4 items-end flex-wrap">
      <div className="space-y-1.5">
        <Label>Cliente</Label>
        <Select value={clienteId} onValueChange={handleClienteChange} disabled={loadingClientes}>
          <SelectTrigger className="w-64">
            <SelectValue placeholder="Selecione o cliente" />
          </SelectTrigger>
          <SelectContent>
            {activeClientes.map((c) => (
              <SelectItem key={c.id} value={c.id}>
                {c.nomeFantasia ?? c.razaoSocial}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-1.5">
        <Label>Estabelecimento</Label>
        <Select
          value={estabelecimentoId}
          onValueChange={handleEstabelecimentoChange}
          disabled={!clienteId || loadingEstab}
        >
          <SelectTrigger className="w-64">
            <SelectValue
              placeholder={clienteId ? 'Selecione o estabelecimento' : 'Selecione o cliente primeiro'}
            />
          </SelectTrigger>
          <SelectContent>
            {activeEstabelecimentos.map((e) => (
              <SelectItem key={e.id} value={e.id}>
                {e.descricao}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
    </div>
  );
}
