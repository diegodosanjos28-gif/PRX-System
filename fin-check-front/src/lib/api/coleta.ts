import axios from 'axios';

const coletorApi = axios.create({
  baseURL: process.env.NEXT_PUBLIC_COLETOR_URL ?? 'http://localhost:8081',
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
