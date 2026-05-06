import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as api from '@/lib/api/templates';
import { TemplateRequest, TemplateVariavelRequest } from '@/lib/types/api';

export const useTemplates = () =>
  useQuery({ queryKey: ['templates'], queryFn: api.getTemplates });

export const useTemplatesAtivos = () =>
  useQuery({ queryKey: ['templates', 'ativos'], queryFn: api.getTemplatesAtivos });

export const useTemplateVariaveis = () =>
  useQuery({ queryKey: ['template-variaveis'], queryFn: api.getTemplateVariaveis });

export const useTemplateVariaveisGlobais = () =>
  useQuery({ queryKey: ['template-variaveis', 'globais'], queryFn: api.getTemplateVariaveisGlobais });

export const useCreateTemplate = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: TemplateRequest) => api.createTemplate(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['templates'] }),
  });
};

export const useUpdateTemplate = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: TemplateRequest }) => api.updateTemplate(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['templates'] }),
  });
};

export const useDeleteTemplate = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => api.deleteTemplate(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['templates'] }),
  });
};

export const useCreateTemplateVariavel = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: TemplateVariavelRequest) => api.createTemplateVariavel(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['template-variaveis'] }),
  });
};

export const useUpdateTemplateVariavel = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: TemplateVariavelRequest }) =>
      api.updateTemplateVariavel(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['template-variaveis'] }),
  });
};

export const useDeleteTemplateVariavel = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => api.deleteTemplateVariavel(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['template-variaveis'] }),
  });
};
