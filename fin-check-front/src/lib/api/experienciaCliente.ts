import api from './axios';
import { ExperienciaClienteData } from '@/lib/types/entities';

export const getExperienciaCliente = (
  estabelecimentoId: string,
  dataInicio: string,
  dataFim: string,
  dataInicioComparacao: string,
  dataFimComparacao: string
) =>
  api
    .get<ExperienciaClienteData>(`/api/experiencia-cliente/${estabelecimentoId}`, {
      params: { dataInicio, dataFim, dataInicioComparacao, dataFimComparacao },
    })
    .then((r) => r.data);
