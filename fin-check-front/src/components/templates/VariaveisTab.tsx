'use client';
import { useTemplateVariaveis } from '@/lib/hooks/useTemplates';
import { Badge } from '@/components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Lock, Info } from 'lucide-react';

export function VariaveisTab() {
  const { data: variaveis, isLoading } = useTemplateVariaveis();

  const sistemaVariaveis = variaveis?.filter((v) => v.sistemaFixo) ?? [];

  if (isLoading) return <p className="text-sm text-muted-foreground">Carregando...</p>;

  return (
    <div className="space-y-4">
      <div className="flex items-start gap-2 px-3 py-2.5 rounded-lg border bg-blue-50 border-blue-200 text-sm text-blue-800">
        <Info className="h-4 w-4 flex-shrink-0 mt-0.5" />
        <p>
          Estas são as variáveis que o sistema substitui automaticamente ao gerar mensagens.
          Use as chaves abaixo ao configurar os parâmetros de cada template.
        </p>
      </div>

      {sistemaVariaveis.length === 0 ? (
        <p className="text-sm text-muted-foreground">Nenhuma variável do sistema cadastrada.</p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Chave</TableHead>
              <TableHead>Descrição</TableHead>
              <TableHead>Tipo</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {sistemaVariaveis.map((v) => (
              <TableRow key={v.id}>
                <TableCell className="font-mono text-sm">{v.chave}</TableCell>
                <TableCell className="text-sm text-muted-foreground">{v.descricao}</TableCell>
                <TableCell>
                  <Badge variant="secondary" className="gap-1">
                    <Lock className="h-3 w-3" /> Sistema
                  </Badge>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
