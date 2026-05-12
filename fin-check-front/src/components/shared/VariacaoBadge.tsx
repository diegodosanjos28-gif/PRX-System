import { TrendingUp, TrendingDown, Minus } from 'lucide-react';
import { cn } from '@/lib/utils';

interface Variacao {
  variacaoPercentual: number;
  direcao: 'ALTA' | 'QUEDA' | 'ESTAVEL';
}

interface Props {
  variacao: Variacao;
  isCusto?: boolean;
  showLabel?: boolean;
  className?: string;
}

function formatPercent(v: number): string {
  const abs = Math.abs(v).toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
  if (v > 0) return `+${abs}%`;
  if (v < 0) return `-${abs}%`;
  return '0%';
}

export function VariacaoBadge({ variacao, isCusto = false, showLabel = false, className }: Props) {
  const { variacaoPercentual, direcao } = variacao;

  // For cost metrics: QUEDA = good (green), ALTA = bad (red)
  const isPositive = isCusto ? direcao === 'QUEDA' : direcao === 'ALTA';
  const isNeutral = direcao === 'ESTAVEL';

  const colorClass = isNeutral
    ? 'text-muted-foreground'
    : isPositive
      ? 'text-green-500'
      : 'text-red-500';

  const Icon = isNeutral ? Minus : direcao === 'ALTA' ? TrendingUp : TrendingDown;

  return (
    <span className={cn('inline-flex items-center gap-1 text-xs font-medium', colorClass, className)}>
      <Icon className="h-3 w-3" />
      {showLabel && <span>{variacao.variacaoPercentual >= 0 ? 'alta' : 'queda'}</span>}
      {formatPercent(variacaoPercentual)}
    </span>
  );
}
