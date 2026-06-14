import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as api from '@/lib/api/implantacoes';
import {
  ImplantacaoClienteRequest,
  ImplantacaoDemandaRequest,
  ImplantacaoDemandaPatchRequest,
} from '@/lib/types/api';

export const useImplantacoes = () =>
  useQuery({ queryKey: ['implantacoes'], queryFn: api.getImplantacoes });

export const useImplantacao = (id: string) =>
  useQuery({
    queryKey: ['implantacoes', id],
    queryFn: () => api.getImplantacao(id),
    enabled: !!id,
  });

export const useCreateImplantacao = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: ImplantacaoClienteRequest) => api.createImplantacao(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['implantacoes'] }),
  });
};

export const useUpdateImplantacao = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ImplantacaoClienteRequest }) =>
      api.updateImplantacao(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['implantacoes'] }),
  });
};

export const useDeleteImplantacao = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.deleteImplantacao(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['implantacoes'] }),
  });
};

export const useAddDemanda = (implantacaoId: string) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: ImplantacaoDemandaRequest) => api.addDemanda(implantacaoId, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['implantacoes', implantacaoId] }),
  });
};

export const usePatchDemanda = (implantacaoId: string) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      demandaId,
      data,
    }: {
      demandaId: string;
      data: ImplantacaoDemandaPatchRequest;
    }) => api.patchDemanda(implantacaoId, demandaId, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['implantacoes', implantacaoId] }),
  });
};

export const useDeleteDemanda = (implantacaoId: string) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (demandaId: string) => api.deleteDemanda(implantacaoId, demandaId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['implantacoes', implantacaoId] }),
  });
};
