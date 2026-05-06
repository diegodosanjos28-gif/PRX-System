import api from './axios';
import { LogColeta } from '@/lib/types/entities';

export const getLogs = (params?: { estabelecimentoId?: string; status?: string }) => api.get<LogColeta[]>('/api/logs/coleta', { params }).then((r) => r.data);

