'use client';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { toast } from 'sonner';
import { getLogs } from '@/lib/api/logs';
import { StatusBadge } from '@/components/shared/StatusBadge';
import { LoadingSpinner } from '@/components/shared/LoadingSpinner';
import { EmptyState } from '@/components/shared/EmptyState';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';

type LogStatus = 'success' | 'login_failed' | 'timeout' | 'error';

const STATUS_LABEL: Record<LogStatus, string> = {
  success:      'Sucesso',
  login_failed: 'Falha de Login',
  timeout:      'Timeout',
  error:        'Erro',
};

const STATUS_VARIANT: Record<LogStatus, 'success' | 'error' | 'warning' | 'neutral'> = {
  success:      'success',
  login_failed: 'error',
  timeout:      'warning',
  error:        'error',
};

const ALL = 'all';

export default function LogsPage() {
  const [status, setStatus] = useState(ALL);

  const { data: logs, isLoading, isError } = useQuery({
    queryKey: ['logs', status],
    queryFn: () => getLogs({ status: status === ALL ? undefined : status }),
    meta: {
      onError: () => toast.error('Erro ao carregar logs de coleta'),
    },
  });

  if (isError) {
    toast.error('Erro ao carregar logs de coleta');
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Logs de Coleta</h1>
      </div>

      <Select value={status} onValueChange={setStatus}>
        <SelectTrigger className="w-52">
          <SelectValue placeholder="Filtrar por status" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={ALL}>Todos os status</SelectItem>
          <SelectItem value="success">Sucesso</SelectItem>
          <SelectItem value="error">Erro</SelectItem>
          <SelectItem value="login_failed">Falha de Login</SelectItem>
          <SelectItem value="timeout">Timeout</SelectItem>
        </SelectContent>
      </Select>

      {isLoading ? (
        <LoadingSpinner />
      ) : !logs?.length ? (
        <EmptyState />
      ) : (
        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Status</TableHead>
                <TableHead>Data / Hora</TableHead>
                <TableHead>Cliente</TableHead>
                <TableHead>Estabelecimento</TableHead>
                <TableHead className="text-right">Registros</TableHead>
                <TableHead>Mensagem de Erro</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {logs.map((log) => {
                const variant = STATUS_VARIANT[log.status as LogStatus] ?? 'neutral';
                const label   = STATUS_LABEL[log.status as LogStatus]   ?? log.status;
                return (
                  <TableRow key={log.id}>
                    <TableCell>
                      <StatusBadge label={label} variant={variant} />
                    </TableCell>
                    <TableCell className="whitespace-nowrap text-sm">
                      {format(new Date(log.executadoEm), 'dd/MM/yyyy HH:mm', { locale: ptBR })}
                    </TableCell>
                    <TableCell className="text-sm">
                      {log.estabelecimento?.razaoSocialCliente ?? '—'}
                    </TableCell>
                    <TableCell className="text-sm">
                      {log.estabelecimento?.descricao ?? '—'}
                    </TableCell>
                    <TableCell className="text-right text-sm">
                      {log.registrosColetados ?? 0}
                    </TableCell>
                    <TableCell className="max-w-xs truncate text-sm text-red-500">
                      {log.mensagemErro ?? '—'}
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
}
