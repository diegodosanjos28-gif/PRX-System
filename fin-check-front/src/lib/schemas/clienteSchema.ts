import { z } from 'zod';

export const clienteSchema = z.object({
  razaoSocial: z.string().min(3, 'Mínimo 3 caracteres'),
  nomeFantasia: z.string().optional(),
  cnpj: z.string().length(14, 'CNPJ deve ter 14 dígitos').regex(/^\d+$/, 'Apenas números'),
  whatsapp: z.string().min(10, 'Informe o WhatsApp com DDD'),
  conciflexLogin: z.string().min(1, 'Login obrigatório'),
  conciflexSenha: z.string().min(1, 'Senha obrigatória'),
  observacoes: z.string().optional(),
  relatorioDiarioAtivo: z.boolean(),
});

export type ClienteFormData = z.infer<typeof clienteSchema>;
