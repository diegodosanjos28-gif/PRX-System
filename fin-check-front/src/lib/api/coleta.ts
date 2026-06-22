import axios from 'axios';

// URL relativa — o browser envia para /coletor/* na mesma origem.
// O servidor Next.js (next.config.mjs rewrites) encaminha server-side
// para http://coletor:8081 via DNS interno do Docker.
// Nunca expõe o host do coletor ao browser do usuário.
const coletorApi = axios.create({
  baseURL: '/coletor',
});

export interface ColetaAcceptedResponse {
  status: string;
  mensagem: string;
  timestamp: string;
}

export const iniciarColetaGeral = (dataInicio?: string, dataFim?: string) =>
  coletorApi.post<ColetaAcceptedResponse>('/api/coleta/iniciar', null, {
    params: {
      ...(dataInicio ? { dataInicio } : {}),
      ...(dataFim ? { dataFim } : {}),
    },
  }).then((r) => r.data);

export const iniciarColetaCliente = (clienteId: string, dataInicio?: string, dataFim?: string) =>
  coletorApi.post<ColetaAcceptedResponse>(`/api/coleta/cliente/${clienteId}`, null, {
    params: {
      ...(dataInicio ? { dataInicio } : {}),
      ...(dataFim ? { dataFim } : {}),
    },
  }).then((r) => r.data);
