import { useQuery } from '@tanstack/react-query';
import { getAuditoria } from '@/lib/api/auditoria';

export const useAuditoria = (estabelecimentoId: string, dataInicio: string, dataFim: string) =>
  useQuery({
    queryKey: ['auditoria', estabelecimentoId, dataInicio, dataFim],
    queryFn: () => getAuditoria(estabelecimentoId, dataInicio, dataFim),
    enabled: !!estabelecimentoId && !!dataInicio && !!dataFim,
  });
