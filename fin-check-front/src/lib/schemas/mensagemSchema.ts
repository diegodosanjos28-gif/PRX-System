import { z } from 'zod';

export const mensagemGerarSchema = z
  .object({
    enviarParaTodos: z.boolean(),
    clienteId: z.string().uuid().optional(),
    estabelecimentoIds: z.array(z.string().uuid()).optional(),
    dataInicio: z.string().min(1, 'Informe a data inicial'),
    dataFim: z.string().min(1, 'Informe a data final'),
    modo: z.enum(['ia', 'template']),
    templateId: z.number().optional(),
  })
  .superRefine((data, ctx) => {
    if (!data.enviarParaTodos) {
      if (!data.clienteId) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: 'Selecione um cliente',
          path: ['clienteId'],
        });
      }
      if (!data.estabelecimentoIds || data.estabelecimentoIds.length === 0) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: 'Selecione ao menos um estabelecimento',
          path: ['estabelecimentoIds'],
        });
      }
    }
  });

export type MensagemGerarFormData = z.infer<typeof mensagemGerarSchema>;
