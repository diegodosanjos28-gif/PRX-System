import { MensagemEnviada } from '@/lib/types/entities';
import { StatusBadge } from '@/components/shared/StatusBadge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';

const statusVariant: Record<string, 'success' | 'error' | 'info' | 'neutral'> = {
  sent: 'info', delivered: 'success', read: 'success', failed: 'error',
};

const statusLabel: Record<string, string> = {
  sent: 'Enviado', delivered: 'Entregue', read: 'Lido', failed: 'Falhou',
};

export function MensagemHistorico({ mensagens }: { mensagens: MensagemEnviada[] }) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Data</TableHead>
          <TableHead>Modo</TableHead>
          <TableHead>Status</TableHead>
          <TableHead>Mensagem</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {mensagens.map((m) => (
          <TableRow key={m.id}>
            <TableCell className="whitespace-nowrap">
              {format(new Date(m.enviadoEm), 'dd/MM/yyyy HH:mm', { locale: ptBR })}
            </TableCell>
            <TableCell>{m.modoGeracao === 'ia' ? 'IA' : 'Template'}</TableCell>
            <TableCell>
              <StatusBadge label={statusLabel[m.statusEntrega] ?? m.statusEntrega} variant={statusVariant[m.statusEntrega] ?? 'neutral'} />
            </TableCell>
            <TableCell className="max-w-xs truncate text-sm text-muted-foreground">{m.conteudo}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
