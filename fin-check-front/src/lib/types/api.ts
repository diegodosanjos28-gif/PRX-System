export interface LoginRequest { login: string; senha: string; }
export interface JwtResponse { token: string; expiresIn: number; }

export interface ClienteRequest {
  razaoSocial: string;
  nomeFantasia?: string;
  cnpj: string;
  whatsapp: string;
  conciflexLogin: string;
  conciflexSenha: string;
  observacoes?: string;
  relatorioDiarioAtivo: boolean;
}

export interface EstabelecimentoRequest {
  descricao: string;
  identificadorConciflex: string;
}

export interface MensagemGerarRequest {
  clienteId: string;
  estabelecimentoId: string;
  dataInicio: string;
  dataFim: string;
  modo: 'ia' | 'template';
  templateId?: number;
}

export interface MensagemEnviarRequest {
  clienteId: string;
  conteudo: string;
  estabelecimentoId?: string;
  templateId?: number;
  templateNome?: string;
  modoGeracao?: string;
  templateParametros?: Record<string, string>;
  /** Optional Meta access token — overrides the server-configured token when provided. */
  metaAccessToken?: string;
}

export interface MensagemGerarTodosRequest {
  dataInicio: string;
  dataFim: string;
  modo: 'ia' | 'template';
  templateId?: number;
  /** Optional Meta access token — overrides the server-configured token when provided. */
  metaAccessToken?: string;
}

export interface TemplateVariavelRequest {
  chave: string;
  descricao: string;
  ordem?: number;
  templateId?: number;
}

export interface TemplateRequest {
  nome: string;
  metaId?: string;
  conteudo: string;
  ativo: boolean;
  variaveis?: TemplateVariavelRequest[];
}

// ── Implantações ──────────────────────────────────────────────────────────

export interface ImplantacaoClienteRequest {
  clienteId: string;
  etapa: string;
  status: string | null;
  responsavel?: string;
  donoContato?: string;
  adquirentes?: string[];
  observacoes?: string;
  progressJson?: unknown;
  etapaIniciadaEm?: string | null;
  ultimoMovimento?: string | null;
}

export interface ImplantacaoDemandaRequest {
  descricao: string;
  prioridade?: string;
  adquirente?: string;
  tipo?: string;
}

export interface ImplantacaoDemandaPatchRequest {
  descricao?: string;
  concluida?: boolean;
  prioridade?: string;
  adquirente?: string;
  tipo?: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  errors?: { campo: string; mensagem: string }[];
}
