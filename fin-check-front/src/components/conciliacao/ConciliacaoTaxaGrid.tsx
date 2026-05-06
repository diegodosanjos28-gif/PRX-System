'use client';
import { useState } from 'react';
import { ConciliacaoTaxaRecord } from '@/lib/types/entities';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

const PAGE_SIZE = 20;

function fmt(n?: number | null, decimals = 2) {
  if (n == null) return '—';
  return n.toLocaleString('pt-BR', { minimumFractionDigits: decimals, maximumFractionDigits: decimals });
}

export function ConciliacaoTaxaGrid({ data }: { data: ConciliacaoTaxaRecord[] }) {
  const [page, setPage] = useState(0);
  const totalPages = Math.ceil(data.length / PAGE_SIZE);
  const rows = data.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

  return (
    <div className="space-y-3">
      <p className="text-sm text-muted-foreground">{data.length} registro(s)</p>

      <div className="overflow-x-auto rounded-md border">
        <Table className="text-xs whitespace-nowrap">
          <TableHeader>
            <TableRow className="bg-muted/50">
              <TableHead>Data Venda</TableHead>
              <TableHead>Adquirente</TableHead>
              <TableHead>Bandeira</TableHead>
              <TableHead>Modalidade</TableHead>
              <TableHead>Produto</TableHead>
              <TableHead className="text-right">Qtd</TableHead>
              <TableHead className="text-right">Valor Bruto</TableHead>
              <TableHead className="text-right">% Prat.</TableHead>
              <TableHead className="text-right">% Cont.</TableHead>
              <TableHead className="text-right">Taxa Prat. (R$)</TableHead>
              <TableHead className="text-right">Taxa Cont. (R$)</TableHead>
              <TableHead className="text-right">Diferença (R$)</TableHead>
              <TableHead className="text-right">Perda (R$)</TableHead>
              <TableHead className="text-center">Auditada</TableHead>
              <TableHead>Cód. Empresa</TableHead>
              <TableHead>Coletado Em</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((r) => {
              const hasOvercharge = (r.totalTaxaNaoContratadaRs ?? 0) > 0;
              return (
                <TableRow
                  key={r.id}
                  className={cn(hasOvercharge && 'bg-red-50 hover:bg-red-100')}
                >
                  <TableCell>{r.dataVenda}</TableCell>
                  <TableCell>{r.adquirente}</TableCell>
                  <TableCell>{r.bandeira}</TableCell>
                  <TableCell>{r.modalidade}</TableCell>
                  <TableCell>{r.produto}</TableCell>
                  <TableCell className="text-right">{r.quantidade}</TableCell>
                  <TableCell className="text-right">{fmt(r.valorBruto)}</TableCell>
                  <TableCell className="text-right">{fmt(r.percentualTaxa, 4)}</TableCell>
                  <TableCell className="text-right">{fmt(r.taxaContratada, 4)}</TableCell>
                  <TableCell className="text-right">{fmt(r.taxaPraticadaRs)}</TableCell>
                  <TableCell className="text-right">{fmt(r.taxaContratadaRs)}</TableCell>
                  <TableCell className={cn('text-right font-medium', hasOvercharge && 'text-red-600')}>
                    {fmt(r.totalTaxaNaoContratadaRs)}
                  </TableCell>
                  <TableCell className="text-right">{fmt(r.perdaRs)}</TableCell>
                  <TableCell className="text-center">
                    <span className={cn(
                      'rounded-full px-2 py-0.5 text-xs font-medium',
                      r.auditada === 'S' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                    )}>
                      {r.auditada === 'S' ? 'Sim' : 'Não'}
                    </span>
                  </TableCell>
                  <TableCell>{r.codigoEmpresa}</TableCell>
                  <TableCell className="text-muted-foreground">
                    {r.coletadoEm ? new Date(r.coletadoEm).toLocaleString('pt-BR') : '—'}
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-between">
          <span className="text-sm text-muted-foreground">
            Página {page + 1} de {totalPages}
          </span>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>
              Anterior
            </Button>
            <Button variant="outline" size="sm" disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}>
              Próxima
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
