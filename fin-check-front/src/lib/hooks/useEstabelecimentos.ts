import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as api from '@/lib/api/estabelecimentos';
import { EstabelecimentoRequest } from '@/lib/types/api';

export const useEstabelecimentos = (clienteId: string) =>
  useQuery({
    queryKey: ['estabelecimentos', clienteId],
    queryFn: () => api.getEstabelecimentos(clienteId),
    enabled: !!clienteId,
  });

export const useCreateEstabelecimento = (clienteId: string) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: EstabelecimentoRequest) => api.createEstabelecimento(clienteId, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['estabelecimentos', clienteId] }),
  });
};

export const useUpdateEstabelecimento = (clienteId: string) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: EstabelecimentoRequest }) =>
      api.updateEstabelecimento(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['estabelecimentos', clienteId] }),
  });
};

export const useDeleteEstabelecimento = (clienteId: string) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.deleteEstabelecimento(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['estabelecimentos', clienteId] }),
  });
};
