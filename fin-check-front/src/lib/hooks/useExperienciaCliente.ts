import { useQuery } from '@tanstack/react-query';
import { getExperienciaCliente } from '@/lib/api/experienciaCliente';

export interface ExperienciaClienteParams {
  estabelecimentoId: string;
  dataInicio: string;
  dataFim: string;
  dataInicioComparacao: string;
  dataFimComparacao: string;
}

export const useExperienciaCliente = (params: ExperienciaClienteParams | null) =>
  useQuery({
    queryKey: [
      'experiencia-cliente',
      params?.estabelecimentoId,
      params?.dataInicio,
      params?.dataFim,
      params?.dataInicioComparacao,
      params?.dataFimComparacao,
    ],
    queryFn: () =>
      params
        ? getExperienciaCliente(
            params.estabelecimentoId,
            params.dataInicio,
            params.dataFim,
            params.dataInicioComparacao,
            params.dataFimComparacao
          )
        : null,
    enabled: params !== null && !!params.estabelecimentoId,
  });
