'use client';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { RecebimentoPorBandeira as TRecebimentoPorBandeira } from '@/lib/types/entities';

export function RecebimentoPorBandeira({ data }: { data: TRecebimentoPorBandeira[] }) {
  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={data}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="bandeira" />
        <YAxis />
        <Tooltip />
        <Legend />
        <Bar dataKey="totalBruto" name="Total Bruto (R$)" fill="#22c55e" />
        <Bar dataKey="totalLiquido" name="Total Líquido (R$)" fill="#3b82f6" />
        <Bar dataKey="totalTaxa" name="Total Taxa (R$)" fill="#f97316" />
      </BarChart>
    </ResponsiveContainer>
  );
}
