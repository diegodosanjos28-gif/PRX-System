import api from './axios';
import { ImplantacaoCliente, ImplantacaoDemanda } from '@/lib/types/entities';
import {
  ImplantacaoClienteRequest,
  ImplantacaoDemandaRequest,
  ImplantacaoDemandaPatchRequest,
} from '@/lib/types/api';

export const getImplantacoes = () =>
  api.get<ImplantacaoCliente[]>('/api/implantacoes').then((r) => r.data);

export const getImplantacao = (id: string) =>
  api.get<ImplantacaoCliente>(`/api/implantacoes/${id}`).then((r) => r.data);

export const createImplantacao = (data: ImplantacaoClienteRequest) =>
  api.post<ImplantacaoCliente>('/api/implantacoes', data).then((r) => r.data);

export const updateImplantacao = (id: string, data: ImplantacaoClienteRequest) =>
  api.put<ImplantacaoCliente>(`/api/implantacoes/${id}`, data).then((r) => r.data);

export const deleteImplantacao = (id: string) =>
  api.delete(`/api/implantacoes/${id}`);

export const addDemanda = (implantacaoId: string, data: ImplantacaoDemandaRequest) =>
  api
    .post<ImplantacaoDemanda>(`/api/implantacoes/${implantacaoId}/demandas`, data)
    .then((r) => r.data);

export const patchDemanda = (
  implantacaoId: string,
  demandaId: string,
  data: ImplantacaoDemandaPatchRequest,
) =>
  api
    .patch<ImplantacaoDemanda>(`/api/implantacoes/${implantacaoId}/demandas/${demandaId}`, data)
    .then((r) => r.data);

export const deleteDemanda = (implantacaoId: string, demandaId: string) =>
  api.delete(`/api/implantacoes/${implantacaoId}/demandas/${demandaId}`);
