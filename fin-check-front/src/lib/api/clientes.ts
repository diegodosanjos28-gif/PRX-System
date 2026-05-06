import api from './axios';
import { ClienteRequest } from '@/lib/types/api';
import { Cliente } from '@/lib/types/entities';

export const getClientes = () => api.get<Cliente[]>('/api/clientes').then((r) => r.data);
export const getCliente = (id: string) => api.get<Cliente>(`/api/clientes/${id}`).then((r) => r.data);
export const createCliente = (data: ClienteRequest) => api.post<Cliente>('/api/clientes', data).then((r) => r.data);
export const updateCliente = (id: string, data: ClienteRequest) => api.put<Cliente>(`/api/clientes/${id}`, data).then((r) => r.data);
export const deleteCliente = (id: string) => api.delete(`/api/clientes/${id}`);
