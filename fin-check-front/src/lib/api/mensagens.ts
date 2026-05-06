import api from './axios';
import { MensagemGerarRequest, MensagemEnviarRequest, MensagemGerarTodosRequest } from '@/lib/types/api';
import { MensagemEnviada, MensagemGeradaResponse, MensagemBulkResultado, PageResponse } from '@/lib/types/entities';

/**
 * Busca o histórico de mensagens enviadas para um cliente.
 */
export const getMensagens = (clienteId: string) =>
  api.get<MensagemEnviada[]>(`/api/mensagens/${clienteId}`).then((r) => r.data);

/**
 * Busca mensagens enviadas para um estabelecimento com paginação.
 */
export const getMensagensEnviadas = (estabelecimentoId: string, page = 0, size = 10) =>
  api
    .get<PageResponse<MensagemEnviada>>('/api/mensagens/enviadas', {
      params: { estabelecimentoId, page, size },
    })
    .then((r) => r.data);

/**
 * Gera a mensagem sem enviar — retorna texto renderizado, resumos financeiros e
 * `templateParametros` (mapa chave→valor que deve ser armazenado no estado e
 * reenviado em {@link enviarMensagem} para preenchimento posicional na Meta API).
 */
export const gerarMensagem = (data: MensagemGerarRequest) =>
  api.post<MensagemGeradaResponse>('/api/mensagens/gerar', data).then((r) => r.data);

/**
 * Gera e envia mensagens para todos os clientes/estabelecimentos ativos sem revisão individual.
 */
export const gerarParaTodos = (data: MensagemGerarTodosRequest) =>
  api.post<MensagemBulkResultado>('/api/mensagens/gerar-todos', data).then((r) => r.data);

/**
 * Envia a mensagem revisada pelo operador via Meta Cloud API.
 *
 * `data.templateParametros` deve conter o mapa retornado por {@link gerarMensagem}
 * para que os parâmetros posicionais ({{1}}, {{2}}, ...) sejam preenchidos corretamente.
 */
export const enviarMensagem = (data: MensagemEnviarRequest) =>
  api.post<MensagemEnviada>('/api/mensagens/enviar', data).then((r) => r.data);
