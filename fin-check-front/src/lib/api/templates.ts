import api from './axios';
import { TemplateRequest, TemplateVariavelRequest } from '@/lib/types/api';
import { Template, TemplateVariavel } from '@/lib/types/entities';

export const getTemplates = () =>
  api.get<Template[]>('/api/templates').then((r) => r.data);

export const getTemplatesAtivos = () =>
  api.get<Template[]>('/api/templates/ativos').then((r) => r.data);

export const getTemplate = (id: number) =>
  api.get<Template>(`/api/templates/${id}`).then((r) => r.data);

export const createTemplate = (data: TemplateRequest) =>
  api.post<Template>('/api/templates', data).then((r) => r.data);

export const updateTemplate = (id: number, data: TemplateRequest) =>
  api.put<Template>(`/api/templates/${id}`, data).then((r) => r.data);

export const deleteTemplate = (id: number) =>
  api.delete(`/api/templates/${id}`);

// --- Variáveis ---

export const getTemplateVariaveis = () =>
  api.get<TemplateVariavel[]>('/api/template-variaveis').then((r) => r.data);

export const getTemplateVariaveisGlobais = () =>
  api.get<TemplateVariavel[]>('/api/template-variaveis/globais').then((r) => r.data);

export const createTemplateVariavel = (data: TemplateVariavelRequest) =>
  api.post<TemplateVariavel>('/api/template-variaveis', data).then((r) => r.data);

export const updateTemplateVariavel = (id: number, data: TemplateVariavelRequest) =>
  api.put<TemplateVariavel>(`/api/template-variaveis/${id}`, data).then((r) => r.data);

export const deleteTemplateVariavel = (id: number) =>
  api.delete(`/api/template-variaveis/${id}`);
