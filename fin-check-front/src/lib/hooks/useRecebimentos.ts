import { useQuery } from '@tanstack/react-query';
import { getRecebimentos } from '@/lib/api/recebimentos';

export const useRecebimentos = (estabelecimentoId: string, dataInicio: string, dataFim: string) =>
  useQuery({
    queryKey: ['recebimentos', estabelecimentoId, dataInicio, dataFim],
    queryFn: () => getRecebimentos(estabelecimentoId, dataInicio, dataFim),
    enabled: !!estabelecimentoId && !!dataInicio && !!dataFim,
  });
