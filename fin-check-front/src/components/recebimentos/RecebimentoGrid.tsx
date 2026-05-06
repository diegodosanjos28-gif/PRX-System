'use client';
import { useState } from 'react';
import { RecebimentoRecord } from '@/lib/types/entities';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

const PAGE_SIZE = 20;

function fmt(n?: number | null, decimals = 2) {
  if (n == null) return '—';
  return n.toLocaleString('pt-BR', { minimumFractionDigits: decimals, maximumFractionDigits: decimals });
}

export function RecebimentoGrid({ data }: { data: RecebimentoRecord[] }) {
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
              <TableHead>Data Pag.</TableHead>
              <TableHead>Data Venda</TableHead>
              <TableHead>Tipo</TableHead>
              <TableHead>Adquirente</TableHead>
              <TableHead>Bandeira</TableHead>
              <TableHead>Modalidade</TableHead>
              <TableHead>NSU</TableHead>
              <TableHead>Cartão</TableHead>
              <TableHead className="text-right">Parc.</TableHead>
              <TableHead className="text-right">Valor Bruto</TableHead>
              <TableHead className="text-right">Taxa %</TableHead>
              <TableHead className="text-right">Taxa (R$)</TableHead>
              <TableHead className="text-right">Valor Líquido</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Banco</TableHead>
              <TableHead>Agência</TableHead>
              <TableHead>Captura</TableHead>
              <TableHead>Coletado Em</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((r) => {
              const isNegative = (r.valorBruto ?? 0) < 0;
              return (
                <TableRow
                  key={r.id}
                  className={cn(isNegative && 'bg-orange-50 hover:bg-orange-100')}
                >
                  <TableCell>{r.dataPagamento}</TableCell>
                  <TableCell>{r.dataVenda}</TableCell>
                  <TableCell>{r.tipoLancamento}</TableCell>
                  <TableCell>{r.adquirente}</TableCell>
                  <TableCell>{r.bandeira}</TableCell>
                  <TableCell>{r.modalidade}</TableCell>
                  <TableCell>{r.nsu || '—'}</TableCell>
                  <TableCell>{r.cartao || '—'}</TableCell>
                  <TableCell className="text-right">
                    {r.parcela}/{r.totalParcelas}
                  </TableCell>
                  <TableCell className={cn('text-right font-medium', isNegative && 'text-orange-600')}>
                    {fmt(r.valorBruto)}
                  </TableCell>
                  <TableCell className="text-right">{fmt(r.taxaPercentual, 5)}</TableCell>
                  <TableCell className="text-right">{fmt(r.valorTaxa, 6)}</TableCell>
                  <TableCell className={cn('text-right font-medium', (r.valorLiquido ?? 0) < 0 && 'text-orange-600')}>
                    {fmt(r.valorLiquido, 6)}
                  </TableCell>
                  <TableCell>
                    <span className={cn(
                      'rounded-full px-2 py-0.5 text-xs font-medium',
                      r.statusConciliacao === 'Conciliada'
                        ? 'bg-green-100 text-green-700'
                        : 'bg-yellow-100 text-yellow-700'
                    )}>
                      {r.statusConciliacao || '—'}
                    </span>
                  </TableCell>
                  <TableCell>{r.banco || '—'}</TableCell>
                  <TableCell>{r.agencia || '—'}</TableCell>
                  <TableCell>{r.meioCaptura || '—'}</TableCell>
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
