import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as api from '@/lib/api/mensagens';
import { MensagemGerarRequest, MensagemEnviarRequest, MensagemGerarTodosRequest } from '@/lib/types/api';

export const useMensagens = (clienteId: string) =>
  useQuery({
    queryKey: ['mensagens', clienteId],
    queryFn:  () => api.getMensagens(clienteId),
    enabled:  !!clienteId,
  });

export const useMensagensEnviadas = (estabelecimentoId: string, page = 0) =>
  useQuery({
    queryKey: ['mensagens-enviadas', estabelecimentoId, page],
    queryFn:  () => api.getMensagensEnviadas(estabelecimentoId, page),
    enabled:  !!estabelecimentoId,
  });

/** Gera a mensagem; o resultado inclui `templateParametros` que deve ser preservado no estado. */
export const useGerarMensagem = () =>
  useMutation({ mutationFn: (data: MensagemGerarRequest) => api.gerarMensagem(data) });

export const useGerarParaTodos = () =>
  useMutation({ mutationFn: (data: MensagemGerarTodosRequest) => api.gerarParaTodos(data) });

/**
 * Envia a mensagem e invalida os caches de histórico relevantes.
 *
 * Certifique-se de incluir `templateParametros` em `data` para que a Meta API
 * receba os parâmetros posicionais corretos.
 */
export const useEnviarMensagem = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: MensagemEnviarRequest) => api.enviarMensagem(data),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['mensagens', vars.clienteId] });
      if (vars.estabelecimentoId) {
        qc.invalidateQueries({ queryKey: ['mensagens-enviadas', vars.estabelecimentoId] });
      }
    },
  });
};
