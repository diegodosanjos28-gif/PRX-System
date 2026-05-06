import api from './axios';
import { AuditoriaResumo } from '@/lib/types/entities';

export const getAuditoria = (estabelecimentoId: string, dataInicio: string, dataFim: string) =>
  api.get<AuditoriaResumo>(`/api/auditoria/${estabelecimentoId}`, {
    params: { dataInicio, dataFim },
  }).then((r) => r.data);
