Prompt gerado — Frontend Next.js
Prompt completo para gerar o frontend da aplicação de conciliação financeira. Copie e cole em uma nova conversa.

Stack & Escopo
Next.js 14 (App Router) TypeScript Tailwind CSS shadcn/ui React Query (TanStack) Zustand Axios Recharts React Hook Form + Zod JWT auth client-side
Prompt completo
Copiar
# PROMPT — Geração do Frontend Next.js
# Sistema de Conciliação Financeira e Auditoria
# Versão: v1.0 | Data: 2026-04-19
# ─────────────────────────────────────────────────────────────────────────────

---

## ⚠️ INSTRUÇÃO PRIMÁRIA

Este documento define completamente o frontend a ser gerado.
Leia tudo antes de responder. Gere arquivos completos, sem omitir imports.
Adicione comentários em português nos trechos de lógica relevante.

---

## CONTEXTO DO SISTEMA

Você é um engenheiro sênior React/Next.js. Vou descrever o frontend completo de
uma ferramenta interna de conciliação financeira. O backend já existe (Spring Boot 3,
PostgreSQL) e expõe uma API REST com autenticação JWT (Bearer token).

O sistema é acessado por operadores internos e permite:
- Gerenciar clientes e estabelecimentos Conciflex
- Visualizar auditorias de taxas de cartão por período
- Visualizar recebimentos por estabelecimento
- Gerar e enviar mensagens de WhatsApp personalizadas (via IA ou template)
- Consultar logs de coleta do microserviço externo

---

## STACK OBRIGATÓRIA

- Next.js 14 com App Router (src/app/)
- TypeScript estrito (strict: true no tsconfig)
- Tailwind CSS + shadcn/ui (componentes base)
- TanStack Query v5 (React Query) para cache e fetch
- Zustand para estado global de autenticação
- Axios com interceptor JWT (Bearer token automático)
- React Hook Form + Zod para todos os formulários
- Recharts para gráficos (auditoria e recebimentos)
- date-fns para manipulação de datas
- lucide-react para ícones

---

## VARIÁVEIS DE AMBIENTE

Crie o arquivo .env.local com:
  NEXT_PUBLIC_API_URL=http://localhost:8080

---

## ESTRUTURA DE PASTAS (siga exatamente)

src/
├── app/
│   ├── layout.tsx                     ← RootLayout com providers
│   ├── page.tsx                       ← Redireciona para /dashboard
│   ├── login/
│   │   └── page.tsx
│   └── (private)/
│       ├── layout.tsx                 ← Layout autenticado (sidebar + header)
│       ├── dashboard/
│       │   └── page.tsx
│       ├── clientes/
│       │   ├── page.tsx               ← Lista de clientes
│       │   ├── novo/page.tsx          ← Formulário de criação
│       │   └── [id]/
│       │       ├── page.tsx           ← Detalhe do cliente
│       │       └── editar/page.tsx
│       ├── estabelecimentos/
│       │   └── [clienteId]/
│       │       └── page.tsx           ← Lista por cliente
│       ├── auditoria/
│       │   └── [estabelecimentoId]/
│       │       └── page.tsx
│       ├── recebimentos/
│       │   └── [estabelecimentoId]/
│       │       └── page.tsx
│       ├── mensagens/
│       │   └── page.tsx               ← Gerar e enviar mensagem
│       └── logs/
│           └── page.tsx
├── components/
│   ├── ui/                            ← shadcn/ui (Button, Input, Card, etc.)
│   ├── layout/
│   │   ├── Sidebar.tsx
│   │   ├── Header.tsx
│   │   └── PageContainer.tsx
│   ├── clientes/
│   │   ├── ClienteForm.tsx
│   │   └── ClienteTable.tsx
│   ├── estabelecimentos/
│   │   ├── EstabelecimentoForm.tsx
│   │   └── EstabelecimentoTable.tsx
│   ├── auditoria/
│   │   ├── AuditoriaResumo.tsx        ← Cards de totais
│   │   └── AuditoriaPorBandeira.tsx   ← Gráfico de barras Recharts
│   ├── recebimentos/
│   │   ├── RecebimentoResumo.tsx
│   │   └── RecebimentoPorBandeira.tsx
│   ├── mensagens/
│   │   ├── MensagemGerarForm.tsx
│   │   └── MensagemHistorico.tsx
│   └── shared/
│       ├── DateRangePicker.tsx
│       ├── StatusBadge.tsx
│       ├── LoadingSpinner.tsx
│       └── EmptyState.tsx
├── lib/
│   ├── api/
│   │   ├── axios.ts                   ← Instância Axios com interceptor JWT
│   │   ├── auth.ts
│   │   ├── clientes.ts
│   │   ├── estabelecimentos.ts
│   │   ├── auditoria.ts
│   │   ├── recebimentos.ts
│   │   ├── mensagens.ts
│   │   └── logs.ts
│   ├── hooks/
│   │   ├── useAuth.ts
│   │   ├── useClientes.ts
│   │   ├── useEstabelecimentos.ts
│   │   ├── useAuditoria.ts
│   │   ├── useRecebimentos.ts
│   │   └── useMensagens.ts
│   ├── stores/
│   │   └── authStore.ts               ← Zustand: token JWT + user info
│   ├── schemas/
│   │   ├── clienteSchema.ts           ← Zod schemas
│   │   ├── estabelecimentoSchema.ts
│   │   └── mensagemSchema.ts
│   └── types/
│       ├── api.ts                     ← Tipos de request/response
│       └── entities.ts                ← Tipos das entidades
├── providers/
│   ├── QueryProvider.tsx              ← TanStack Query Provider
│   └── AuthProvider.tsx              ← Lógica de redirect para /login
└── middleware.ts                      ← Proteção de rotas (verifica JWT no cookie/localStorage)

---

## TIPOS TYPESCRIPT (gere completos em src/lib/types/)

### entities.ts
```typescript
export interface Cliente {
  id: string;
  razaoSocial: string;
  nomeFantasia?: string;
  cnpj: string;
  whatsapp: string;
  ativo: boolean;
  observacoes?: string;
  criadoEm: string;
  estabelecimentos?: Estabelecimento[];
}

export interface Estabelecimento {
  id: string;
  clienteId: string;
  descricao: string;
  identificadorConciflex: string;
  ativo: boolean;
}

export interface AuditoriaResumo {
  totalTransacoes: number;
  totalCobradoAMais: number;
  totalCobradoAMenos: number;
  porBandeira: AuditoriaPorBandeira[];
}

export interface AuditoriaPorBandeira {
  bandeira: string;
  qtd: number;
  diferencaTotal: number;
}

export interface RecebimentoResumo {
  totalRecebido: number;
  totalDescontado: number;
  porBandeira: RecebimentoPorBandeira[];
}

export interface RecebimentoPorBandeira {
  bandeira: string;
  totalRecebido: number;
  totalTaxa: number;
}

export interface MensagemEnviada {
  id: string;
  clienteId: string;
  conteudo: string;
  modoGeracao: 'ia' | 'template';
  metaMessageId?: string;
  statusEntrega: 'sent' | 'delivered' | 'read' | 'failed';
  enviadoEm: string;
}

export interface LogColeta {
  id: string;
  estabelecimentoId: string;
  executadoEm: string;
  status: 'success' | 'login_failed' | 'timeout' | 'error';
  registrosColetados: number;
  mensagemErro?: string;
}
```

### api.ts
```typescript
export interface LoginRequest { login: string; senha: string; }
export interface JwtResponse { token: string; expiresAt: string; }

export interface ClienteRequest {
  razaoSocial: string;
  nomeFantasia?: string;
  cnpj: string;
  whatsapp: string;
  conciflex_login: string;
  conciflex_senha: string;
  observacoes?: string;
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
}

export interface MensagemEnviarRequest {
  clienteId: string;
  conteudo: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
```

---

## AXIOS — CONFIGURAÇÃO (src/lib/api/axios.ts)

- Criar instância com baseURL = process.env.NEXT_PUBLIC_API_URL
- Request interceptor: adicionar Authorization: Bearer {token} automaticamente
  lendo o token do Zustand authStore
- Response interceptor: se 401, limpar store e redirecionar para /login

---

## ZUSTAND — AUTH STORE (src/lib/stores/authStore.ts)

Estado:
  token: string | null
  user: { login: string; role: string } | null
  isAuthenticated: boolean

Ações:
  setAuth(token, user) → salvar token no localStorage e no estado
  clearAuth()          → limpar localStorage e estado
  initAuth()           → ler do localStorage ao carregar a app (hidratação)

---

## MIDDLEWARE DE ROTAS (src/middleware.ts)

- Rotas protegidas: tudo dentro de /(private)
- Se não houver token no cookie ou localStorage → redirect para /login
- Se já autenticado e acessar /login → redirect para /dashboard

---

## REACT QUERY HOOKS (src/lib/hooks/)

Para cada domínio, gere hooks usando useQuery e useMutation:

### useClientes.ts
- useClientes()                                → GET /api/clientes
- useCliente(id)                              → GET /api/clientes/{id}
- useCreateCliente()                          → POST /api/clientes
- useUpdateCliente()                          → PUT /api/clientes/{id}
- useDeleteCliente()                          → DELETE /api/clientes/{id}
  (após mutação, invalidar query 'clientes')

### useEstabelecimentos.ts
- useEstabelecimentos(clienteId)              → GET /api/clientes/{clienteId}/estabelecimentos
- useCreateEstabelecimento(clienteId)         → POST /api/clientes/{clienteId}/estabelecimentos
- useUpdateEstabelecimento()                  → PUT /api/estabelecimentos/{id}
- useDeleteEstabelecimento()                  → DELETE /api/estabelecimentos/{id}

### useAuditoria.ts
- useAuditoria(estabelecimentoId, dataInicio, dataFim)
  → GET /api/auditoria/{estabelecimentoId}?dataInicio=&dataFim=
  (só executa quando os 3 params estão preenchidos: enabled)

### useRecebimentos.ts
- useRecebimentos(estabelecimentoId, dataInicio, dataFim)
  → GET /api/recebimentos/{estabelecimentoId}?dataInicio=&dataFim=

### useMensagens.ts
- useMensagens(clienteId)                     → GET /api/mensagens/{clienteId}
- useGerarMensagem()                          → POST /api/mensagens/gerar
- useEnviarMensagem()                         → POST /api/mensagens/enviar

---

## PÁGINAS — COMPORTAMENTO ESPERADO

### /login
- Formulário com campos login e senha (React Hook Form + Zod)
- Chama POST /api/auth/login, salva token no authStore, redireciona para /dashboard
- Exibir erro inline se credenciais inválidas

### /dashboard
- Cards de resumo: total de clientes ativos, total de estabelecimentos ativos
- Listagem rápida dos últimos 5 logs de coleta (status colorido com badge)
- Botão de atalho para "Novo Cliente"

### /clientes
- Tabela com colunas: Razão Social, CNPJ, WhatsApp, Status (badge), Ações
- Ações: Ver detalhes, Editar, Inativar (confirmação com Dialog)
- Botão "Novo Cliente" leva para /clientes/novo
- Busca local por nome/CNPJ (filtrar no frontend)

### /clientes/novo e /clientes/[id]/editar
- Formulário completo com validação Zod:
  razaoSocial (obrigatório), nomeFantasia, cnpj (14 dígitos, só números),
  whatsapp, conciflex_login, conciflex_senha (input type password), observacoes
- Campos de credenciais Conciflex: mostrar aviso "Armazenado com criptografia AES-256"
- Submit chama useCreateCliente ou useUpdateCliente

### /clientes/[id]
- Exibe dados do cliente
- Seção de Estabelecimentos: tabela com ações (editar, inativar)
- Botão "Novo Estabelecimento" abre um Sheet (painel lateral) com EstabelecimentoForm
- Botão "Ver Auditoria" → navega para /auditoria/{estabelecimentoId}
- Botão "Ver Recebimentos" → navega para /recebimentos/{estabelecimentoId}
- Botão "Enviar Mensagem" → navega para /mensagens com clienteId pré-selecionado

### /auditoria/[estabelecimentoId]
- DateRangePicker para selecionar período (dataInicio, dataFim)
- Cards de resumo: Total de transações, Cobrado a mais (vermelho), Cobrado a menos (amarelo)
- Gráfico de barras (Recharts BarChart) com dados por bandeira:
  eixo X = bandeira, barras = qtd de transações e diferença total
- Tabela abaixo com detalhamento por bandeira

### /recebimentos/[estabelecimentoId]
- DateRangePicker para período
- Cards: Total Recebido, Total Descontado em Taxas
- Gráfico de barras por bandeira: totalRecebido vs totalTaxa

### /mensagens
- Select para escolher Cliente (dropdown com todos os clientes ativos)
- Select para Estabelecimento (carrega ao selecionar cliente)
- DateRangePicker para período de análise
- Radio: modo "ia" | "template"
- Botão "Gerar Mensagem" → chama useGerarMensagem, exibe resultado em Textarea editável
- Botão "Enviar via WhatsApp" (habilitado após gerar) → chama useEnviarMensagem
- Seção "Histórico" abaixo: tabela com mensagens enviadas ao cliente selecionado

### /logs
- Filtros: Estabelecimento (select), Status (select: todos/success/error/timeout)
- Tabela: Estabelecimento, Executado em, Status (badge colorido), Registros, Mensagem de erro

---

## COMPONENTES COMPARTILHADOS

### DateRangePicker.tsx
- Dois inputs type="date" (de / até)
- Exporta: { dataInicio: string, dataFim: string }
- Usar date-fns para formatar (yyyy-MM-dd)

### StatusBadge.tsx
- Props: status (string), variant ('success'|'error'|'warning'|'info'|'neutral')
- Usa shadcn Badge com classes de cor condicionais

### Sidebar.tsx
- Links: Dashboard, Clientes, Mensagens, Logs
- Logo/nome do sistema no topo
- Indicador de usuário logado no rodapé + botão Sair (clearAuth + redirect /login)

---

## LAYOUT AUTENTICADO (src/app/(private)/layout.tsx)

- Sidebar fixa à esquerda (256px)
- Área de conteúdo com Header (breadcrumb + nome do usuário)
- Wrapper com PageContainer (padding padrão, max-width)

---

## ZOD SCHEMAS (src/lib/schemas/)

### clienteSchema.ts
```typescript
export const clienteSchema = z.object({
  razaoSocial: z.string().min(3, 'Mínimo 3 caracteres'),
  nomeFantasia: z.string().optional(),
  cnpj: z.string().length(14, 'CNPJ deve ter 14 dígitos').regex(/^\d+$/, 'Apenas números'),
  whatsapp: z.string().min(10, 'Informe o WhatsApp com DDD'),
  conciflex_login: z.string().min(1, 'Login obrigatório'),
  conciflex_senha: z.string().min(1, 'Senha obrigatória'),
  observacoes: z.string().optional(),
});
```

### mensagemSchema.ts
```typescript
export const mensagemGerarSchema = z.object({
  clienteId: z.string().uuid(),
  estabelecimentoId: z.string().uuid(),
  dataInicio: z.string().min(1, 'Informe a data inicial'),
  dataFim: z.string().min(1, 'Informe a data final'),
  modo: z.enum(['ia', 'template']),
});
```

---

## TRATAMENTO DE ERROS

- Erros de API (ApiError) devem ser exibidos com toast (shadcn Sonner ou react-hot-toast)
- Campos de formulário devem exibir erros inline via React Hook Form
- Estados de loading: usar Skeleton nos cards e tabelas enquanto carrega
- Estado vazio: usar EmptyState com ícone e mensagem contextual

---

## O QUE GERAR

1. package.json com todas as dependências
2. tsconfig.json (strict)
3. tailwind.config.ts com shadcn/ui
4. .env.local.example
5. src/middleware.ts
6. src/providers/ (QueryProvider, AuthProvider)
7. src/lib/api/axios.ts + todos os arquivos de chamadas API
8. src/lib/stores/authStore.ts
9. src/lib/hooks/ (todos os hooks React Query)
10. src/lib/types/ (entities.ts + api.ts)
11. src/lib/schemas/ (todos os schemas Zod)
12. src/app/layout.tsx + src/app/page.tsx
13. src/app/login/page.tsx
14. src/app/(private)/layout.tsx
15. Todas as páginas listadas acima
16. Todos os componentes listados acima
17. src/components/layout/ (Sidebar, Header, PageContainer)

Gere cada arquivo completo, sem omitir imports.
Prefira clareza e completude à brevidade.
Adicione comentários em português nos trechos de lógica relevante.

