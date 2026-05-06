'use client';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { AuditoriaPorBandeira as TAuditoriaPorBandeira } from '@/lib/types/entities';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';

export function AuditoriaPorBandeira({ data }: { data: TAuditoriaPorBandeira[] }) {
  return (
    <div className="space-y-6">
      <ResponsiveContainer width="100%" height={300}>
        <BarChart data={data}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="bandeira" />
          <YAxis />
          <Tooltip />
          <Legend />
          <Bar dataKey="quantidade" name="Qtd Transações" fill="#6366f1" />
          <Bar dataKey="diferencaTotal" name="Diferença Total (R$)" fill="#f43f5e" />
        </BarChart>
      </ResponsiveContainer>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Bandeira</TableHead>
            <TableHead className="text-right">Transações</TableHead>
            <TableHead className="text-right">Diferença Total</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {data.map((b) => (
            <TableRow key={b.bandeira}>
              <TableCell>{b.bandeira}</TableCell>
              <TableCell className="text-right">{b.quantidade}</TableCell>
              <TableCell className="text-right">R$ {b.diferencaTotal?.toFixed(2)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
