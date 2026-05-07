export interface Cliente {
  id: string;
  razaoSocial: string;
  nomeFantasia?: string;
  cnpj: string;
  whatsapp: string;
  ativo: boolean;
  observacoes?: string;
  conciflexLogin?: string;
  conciflexSenha?: string;
  criadoEm: string;
}

export interface Estabelecimento {
  id: string;
  clienteId: string;
  razaoSocialCliente: string;
  descricao: string;
  identificadorConciflex: string;
  ativo: boolean;
}

// --- Conciliação de Taxas ---

export interface ConciliacaoTaxaRecord {
  id: string;
  idConciflex: string;
  codigoEmpresa: string;
  dataVenda: string;
  adquirente: string;
  bandeira: string;
  modalidade: string;
  produto: string;
  valorBruto: number;
  percentualTaxa: number;
  taxaContratada: number;
  quantidade: number;
  taxaPraticadaRs: number;
  taxaContratadaRs: number;
  totalTaxaNaoContratadaRs: number;
  perdaRs: number;
  auditada: string;
  coletadoEm: string;
}

export interface AuditoriaPorBandeira {
  bandeira: string;
  quantidade: number;
  diferencaTotal: number;
}

export interface AuditoriaResumo {
  totalTransacoes: number;
  totalCobradoAMais: number;
  totalCobradoAMenos: number;
  porBandeira: AuditoriaPorBandeira[];
  conciliacaoTaxaResponse: ConciliacaoTaxaRecord[];
}

// --- Recebimentos ---

export interface RecebimentoRecord {
  id: string;
  idConciflex: string;
  tipoLancamento: string;
  dataVenda: string;
  dataPrevisao: string;
  dataPagamento: string;
  adquirente: string;
  bandeira: string;
  modalidade: string;
  nsu: string;
  cartao: string;
  parcela: number;
  totalParcelas: number;
  valorBruto: number;
  taxaPercentual: number;
  valorTaxa: number;
  valorLiquido: number;
  statusConciliacao: string;
  banco: string;
  agencia: string;
  meioCaptura: string;
  coletadoEm: string;
}

export interface RecebimentoPorBandeira {
  bandeira: string;
  quantidade: number;
  totalBruto: number;
  totalLiquido: number;
  totalTaxa: number;
}

export interface RecebimentoResumo {
  totalRecebido: number;
  totalDescontado: number;
  porBandeira: RecebimentoPorBandeira[];
  recebimentos: RecebimentoRecord[];
}

// --- Mensagens ---

export interface MensagemGeradaResponse {
  resumoAuditoria: AuditoriaResumo;
  resumoRecebimento: RecebimentoResumo;
  mensagem: string;
  templateParametros?: Record<string, string>;
}

export interface MensagemEnviada {
  id: string;
  clienteId: string;
  clienteNome: string;
  conteudo: string;
  modoGeracao: 'ia' | 'template';
  metaMessageId?: string;
  statusEntrega: 'sent' | 'delivered' | 'read' | 'failed';
  enviadoEm: string;
  estabelecimentoId?: string;
  templateNome?: string;
}

export interface MensagemBulkResultado {
  total: number;
  enviados: number;
  erros: number;
  errosDetalhados: string[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// --- Templates ---

export interface TemplateVariavel {
  id: number;
  chave: string;
  descricao: string;
  sistemaFixo: boolean;
  ordem: number;
  templateId?: number;
}

export interface Template {
  id: number;
  nome: string;
  metaId?: string;
  conteudo: string;
  ativo: boolean;
  variaveis: TemplateVariavel[];
  createdAt: string;
  updatedAt: string;
}

export interface LogColeta {
  id: string;
  estabelecimento: Estabelecimento;
  executadoEm: string;
  status: 'success' | 'login_failed' | 'timeout' | 'error';
  registrosColetados: number;
  mensagemErro?: string;
}
