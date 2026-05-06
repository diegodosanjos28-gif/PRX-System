import api from './axios';
import { RecebimentoResumo } from '@/lib/types/entities';

export const getRecebimentos = (estabelecimentoId: string, dataInicio: string, dataFim: string) =>
  api.get<RecebimentoResumo>(`/api/recebimentos/${estabelecimentoId}`, {
    params: { dataInicio, dataFim },
  }).then((r) => r.data);
