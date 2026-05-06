'use client';
import { useClientes } from '@/lib/hooks/useClientes';
import { useQuery } from '@tanstack/react-query';
import { getLogs } from '@/lib/api/logs';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { StatusBadge } from '@/components/shared/StatusBadge';
import { LoadingSpinner } from '@/components/shared/LoadingSpinner';
import { Button } from '@/components/ui/button';
import Link from 'next/link';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';

const logVariant: Record<string, 'success' | 'error' | 'warning' | 'neutral'> = {
  success: 'success', login_failed: 'error', timeout: 'warning', error: 'error',
};

export default function DashboardPage() {
  const { data: clientes, isLoading: loadingClientes } = useClientes();
  const { data: logs, isLoading: loadingLogs } = useQuery({
    queryKey: ['logs'],
    queryFn: () => getLogs(),
  });

  const ativos = clientes?.filter((c) => c.ativo).length ?? 0;
  const ultimos5Logs = logs?.slice(0, 5) ?? [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Dashboard</h1>
        <Button asChild><Link href="/clientes/novo">Novo Cliente</Link></Button>
      </div>

      <div className="grid grid-cols-2 gap-4 max-w-md">
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm text-muted-foreground">Clientes Ativos</CardTitle></CardHeader>
          <CardContent>{loadingClientes ? '...' : <p className="text-3xl font-bold">{ativos}</p>}</CardContent>
        </Card>
      </div>

      <div>
        <h2 className="font-semibold mb-3">Últimos Logs de Coleta</h2>
        {loadingLogs ? <LoadingSpinner /> : (
          <div className="space-y-2">
            {ultimos5Logs?.map((log) => (
              <div key={log.id} className="flex items-center gap-4 rounded-md border p-3 text-sm">
                <StatusBadge label={log.status} variant={logVariant[log.status] ?? 'neutral'} />
                <span className="text-muted-foreground">
                  {format(new Date(log.executadoEm), 'dd/MM/yyyy HH:mm', { locale: ptBR })}
                </span>
                <span>{log.estabelecimento?.descricao}</span>
                <span>{log.registrosColetados} registros</span>
                {log.mensagemErro && <span className="text-red-500 truncate max-w-xs">{log.mensagemErro}</span>}

              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
