import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as api from '@/lib/api/clientes';
import { ClienteRequest } from '@/lib/types/api';

export const useClientes = () =>
  useQuery({ queryKey: ['clientes'], queryFn: api.getClientes });

export const useCliente = (id: string) =>
  useQuery({ queryKey: ['clientes', id], queryFn: () => api.getCliente(id), enabled: !!id });

export const useCreateCliente = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: ClienteRequest) => api.createCliente(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['clientes'] }),
  });
};

export const useUpdateCliente = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ClienteRequest }) => api.updateCliente(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['clientes'] }),
  });
};

export const useDeleteCliente = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.deleteCliente(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['clientes'] }),
  });
};

/**
 * Despacha (exclui definitivamente) um cliente do Curral.
 *
 * Invalida as três origens afetadas: `['implantacoes']` alimenta tanto os cavalos
 * quanto o painel "Clientes para Atuar"; `['implantacoes','rocks']` alimenta os ROCKs
 * Concluídos, que somem junto com as demandas do cliente; `['clientes']` alimenta as
 * demais telas.
 */
export const useDespacharCliente = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.despacharCliente(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['implantacoes'] });
      qc.invalidateQueries({ queryKey: ['implantacoes', 'rocks'] });
      qc.invalidateQueries({ queryKey: ['clientes'] });
    },
  });
};
