import api from './axios';
import { EstabelecimentoRequest } from '@/lib/types/api';
import { Estabelecimento } from '@/lib/types/entities';

export const getEstabelecimentos = (clienteId: string) =>
  api.get<Estabelecimento[]>(`/api/clientes/${clienteId}/estabelecimentos`).then((r) => r.data);

export const createEstabelecimento = (clienteId: string, data: EstabelecimentoRequest) =>
  api.post<Estabelecimento>(`/api/clientes/${clienteId}/estabelecimentos`, data).then((r) => r.data);

export const updateEstabelecimento = (id: string, data: EstabelecimentoRequest) =>
  api.put<Estabelecimento>(`/api/estabelecimentos/${id}`, data).then((r) => r.data);

export const deleteEstabelecimento = (id: string) =>
  api.delete(`/api/estabelecimentos/${id}`);
