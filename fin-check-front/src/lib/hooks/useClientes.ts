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
