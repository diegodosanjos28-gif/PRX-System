import { z } from 'zod';

export const estabelecimentoSchema = z.object({
  descricao: z.string().min(1, 'Descrição é obrigatória'),
  identificadorConciflex: z.string().min(1, 'Identificador Conciflex é obrigatório'),
});

export type EstabelecimentoFormData = z.infer<typeof estabelecimentoSchema>;
