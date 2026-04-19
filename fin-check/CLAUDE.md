# PROMPT — Geração do Back-end Spring Boot v1
# Sistema de Conciliação Financeira e Auditoria
# ─────────────────────────────────────────────────────────────────────────────

---

## ⚠️ INSTRUÇÃO PRIMÁRIA — MEMÓRIA DE CONTEXTO DO PROJETO

Este documento é a **fonte de verdade e memória persistente** deste projeto.

**Ao receber este arquivo, você deve:**

1. **Ler e internalizar todo o conteúdo** antes de responder qualquer coisa. Este documento
   define o estado atual completo do sistema — arquitetura, entidades, endpoints, regras de
   negócio, stack e decisões técnicas.

2. **Tratar este documento como sua memória sobre o projeto.** Qualquer informação aqui
   presente substitui o que você poderia assumir por padrão ou por contextos anteriores.

3. **Manter este documento sempre atualizado.** Sempre que houver uma alteração aprovada
   — novo requisito, mudança de entidade, novo endpoint, decisão de arquitetura, ajuste de
   regra de negócio — você deve:
   - Aplicar a mudança no sistema (código, migration, etc.)
   - **Refletir a alteração neste documento**, atualizando a seção correspondente
   - Incrementar a versão no cabeçalho (ex: v1 → v1.1)
   - Registrar a mudança no changelog abaixo

4. **Nunca contradizer este documento** sem que o usuário tenha solicitado explicitamente
   uma mudança. Se identificar uma inconsistência, sinalize antes de agir.

5. **Ao iniciar qualquer nova sessão**, peça ao usuário que cole este documento atualizado
   como contexto. Sem ele, sua memória sobre o projeto está incompleta.

---

## CHANGELOG

| Versão | Data       | Alteração                                      |
|--------|------------|------------------------------------------------|
| v1.0   | 2026-04-18 | Versão inicial — estrutura completa do back-end |
| v1.1   | 2026-04-18 | Entidades e DTOs atualizados com estrutura real da API Conciflex (validada com dados de produção). Adicionada entidade ResumoColeta. DTOs de desserialização completos com @JsonProperty para todos os campos UPPER_CASE da API. |

---

## CONTEXTO DO SISTEMA

Você é um engenheiro sênior Java especializado em Spring Boot. Vou descrever um sistema
completo e quero que você gere o back-end completo da aplicação API principal (não o
microserviço coletor — esse é separado).

O sistema é uma ferramenta de gerenciamento interno de conciliação financeira. Ele:
- Gerencia clientes e seus estabelecimentos Conciflex
- Armazena dados financeiros coletados por um microserviço externo
- Processa auditorias de taxas de cartão
- Gera mensagens personalizadas via IA (Anthropic API) ou template fixo
- Envia mensagens via WhatsApp (Meta Cloud API)

---

## STACK OBRIGATÓRIA

- Java 17
- Spring Boot 3.x (última estável)
- Spring Security com JWT (Bearer token)
- Spring Data JPA + Hibernate
- PostgreSQL (driver + dialect)
- Flyway para migrations
- Maven (pom.xml)
- Lombok
- MapStruct para DTOs
- RestTemplate ou WebClient para chamadas externas (Anthropic e Meta)
- SLF4J + Logback para logs estruturados
- Docker (Dockerfile multi-stage com JDK 17 builder + JRE 17 runtime slim)

---

## ESTRUTURA DE PACOTES (siga exatamente)

```
com.conciliacao.api
├── config/
│   ├── SecurityConfig.java
│   ├── JwtConfig.java
│   └── CryptoConfig.java
├── controller/
│   ├── AuthController.java
│   ├── ClienteController.java
│   ├── EstabelecimentoController.java
│   ├── AuditoriaController.java
│   ├── RecebimentoController.java
│   ├── MensagemController.java
│   └── WebhookMetaController.java
├── service/
│   ├── AuthService.java
│   ├── ClienteService.java
│   ├── EstabelecimentoService.java
│   ├── AuditoriaService.java
│   ├── RecebimentoService.java
│   ├── MensagemService.java
│   ├── AnthropicService.java
│   ├── WhatsAppService.java
│   └── CryptoService.java
├── repository/
│   ├── ClienteRepository.java
│   ├── EstabelecimentoRepository.java
│   ├── ConciliacaoTaxaRepository.java
│   ├── RecebimentoRepository.java
│   ├── MensagemEnviadaRepository.java
│   └── LogColetaRepository.java
├── entity/
│   ├── Cliente.java
│   ├── Estabelecimento.java
│   ├── ConciliacaoTaxa.java
│   ├── Recebimento.java
│   ├── ResumoColeta.java
│   ├── MensagemEnviada.java
│   └── LogColeta.java
├── dto/
│   ├── conciflex/                          ← DTOs de desserialização da API Conciflex
│   │   ├── ConciliacaoTaxaApiResponse.java
│   │   ├── ConciliacaoTaxaItem.java
│   │   ├── RecebimentoApiResponse.java
│   │   ├── RecebimentoItem.java
│   │   └── TotalizadorItem.java
│   ├── request/
│   │   ├── ClienteRequest.java
│   │   ├── EstabelecimentoRequest.java
│   │   ├── MensagemGerarRequest.java
│   │   └── LoginRequest.java
│   └── response/
│       ├── ClienteResponse.java
│       ├── AuditoriaResumoResponse.java
│       ├── AuditoriaPorBandeira.java
│       ├── RecebimentoResumoResponse.java
│       ├── RecebimentoPorBandeira.java
│       ├── MensagemResponse.java
│       └── JwtResponse.java
├── mapper/
│   ├── ClienteMapper.java
│   └── EstabelecimentoMapper.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── IntegrationException.java
└── ApiApplication.java
```

---

## ENTIDADES JPA (gere todas com anotações completas)

> IMPORTANTE: Os campos abaixo foram modelados a partir da estrutura REAL das APIs do Conciflex
> (validada com dados de produção). Respeite os tipos exatos definidos.

### Cliente
```
id              UUID            PK, generated
razaoSocial     String          not null
nomeFantasia    String          nullable
cnpj            String(14)      not null, unique
whatsapp        String          not null
conciflex_login String (TEXT)   not null  ← armazenar SEMPRE criptografado (AES-256)
conciflex_senha String (TEXT)   not null  ← armazenar SEMPRE criptografado (AES-256)
ativo           boolean         default true
observacoes     String (TEXT)   nullable
criadoEm        LocalDateTime   auto
estabelecimentos List<Estabelecimento>  @OneToMany(cascade=ALL, orphanRemoval=true)
```

### Estabelecimento
```
id                      UUID    PK
cliente                 Cliente @ManyToOne
descricao               String  not null
identificadorConciflex  String  not null   ← valor do campo ESTABELECIMENTO retornado pela API
                                             (pode ser CNPJ ou código interno Conciflex)
ativo                   boolean default true
```

### ConciliacaoTaxa
```
// Mapeamento exato da API POST /conciliacao-taxas/buscar
id                          UUID             PK
estabelecimento             Estabelecimento  @ManyToOne
idConciflex                 String           ← campo ID da API (ex: "1117294795")
codigoEmpresa               String
dataVenda                   LocalDate
adquirente                  String           ← ex: "Ticket - 135", "Alelo - 267"
codigoAdquirente            String
bandeira                    String           ← ex: "Ticket - 193", "Alelo - 167"
codBandeira                 String
modalidade                  String           ← ex: "Voucher"
codigoModalidade            String
produto                     String           ← ex: "Flex", "Refeição PAT", "Multi Benefícios"
codigoProduto               String
valorBruto                  BigDecimal(15,2)
valorDesconto               BigDecimal(15,6) ← alta precisão
percentualTaxa              BigDecimal(8,4)  ← taxa efetivamente cobrada (%)
taxaContratada              BigDecimal(8,4)  ← taxa contratual (%)
quantidade                  Integer
taxaPraticadaRs             BigDecimal(15,2)
taxaPraticadaCadastradaRs   BigDecimal(15,2)
taxaContratadaRs            BigDecimal(15,2)
totalTaxaNaoContratadaRs    BigDecimal(15,2) ← CAMPO PRINCIPAL DE AUDITORIA
perdaRs                     BigDecimal(15,2)
perda                       BigDecimal(15,4)
auditada                    String(1)        ← 'S' ou 'N'
estabelecimentoConciflex    String           ← campo ESTABELECIMENTO da API
coletadoEm                  LocalDateTime
```

### Recebimento
```
// Mapeamento exato da API POST /recebimentos-operadoras/buscar
// ATENÇÃO: Contém TIPOS MISTOS — Pagamento (cod=1) e Ajuste a Débito (cod=2)
// Ajustes a débito têm valorBruto e valorLiquido NEGATIVOS
id                        UUID             PK
estabelecimento           Estabelecimento  @ManyToOne
idConciflex               String           ← campo ID da API
codTipoLancamento         String           ← "1"=Pagamento, "2"=Ajuste Débito, "3"=Ajuste Crédito
tipoLancamento            String           ← "Pagamento", "Ajuste a Débito"
codTipoPagamento          String           ← "1"=Normal, "2"=Antecipado
tipoPagamento             String
dataVenda                 LocalDate
dataPrevisao              LocalDate
dataPagamento             LocalDate
dataCancelamento          LocalDate        nullable
adquirente                String
codAdquirente             String
bandeira                  String
codBandeira               String
modalidade                String
nsu                       String           nullable
autorizacao               String           nullable
cartao                    String           nullable (mascarado)
numeroResumoVenda         String
produto                   String           nullable
meioCaptura               String           nullable ← "POS", "OUTROS"
valorBruto                BigDecimal(15,2) ← NEGATIVO em ajustes
taxaPercentual            BigDecimal(10,5)
valorTaxa                 BigDecimal(15,6) ← NEGATIVO (custo)
tarifaTransacao           BigDecimal(15,2)
taxaAntecipacao           BigDecimal(8,4)
valorTaxaAntecipacao      BigDecimal(15,6)
outrasDespesas            BigDecimal(15,2)
valorLiquido              BigDecimal(18,6) ← alta precisão, NEGATIVO em ajustes
valorLiquidoSAntecipacao  BigDecimal(15,6)
parcela                   Short
totalParcelas             Short
possuiTaxaMinima          String(1)        ← 'S' ou 'N'
estabelecimentoConciflex  String
banco                     String           ← ex: "748 - Sicredi", "237 - Bradesco"
agencia                   String
contaCorrente             String
statusConciliacao         String
codigoOperadoraAjuste     String           nullable
descAjuste                String (TEXT)    nullable
classificacaoAjuste       String           nullable
autorizador               String           nullable
dataProcessamento         LocalDate
horaProcessamento         String           ← armazenar como String (HH:mm:ss)
nomeArquivo               String
observacoes               String (TEXT)    nullable
coletadoEm                LocalDateTime
```

### ResumoColeta
```
// Totalizadores retornados no nível raiz da resposta da API (não por transação)
id                  UUID             PK
estabelecimento     Estabelecimento  @ManyToOne
tipo                String           ← "conciliacao_taxas" ou "recebimentos"
dataInicio          LocalDate
dataFim             LocalDate
totalRegistros      Integer
valorBrutoTotal     BigDecimal(18,4)
valorLiquidoTotal   BigDecimal(18,6)
totalTaxas          BigDecimal(15,4)
totalizadoresJson   String (TEXT)    ← demais campos da raiz serializados como JSON
coletadoEm          LocalDateTime
```

### MensagemEnviada
```
id              UUID
cliente         Cliente @ManyToOne
conteudo        String (TEXT)
modoGeracao     String   ('ia' | 'template')
metaMessageId   String   nullable
statusEntrega   String   ('sent'|'delivered'|'read'|'failed')
enviadoEm       LocalDateTime
```

### LogColeta
```
id                   UUID
estabelecimento      Estabelecimento @ManyToOne
executadoEm          LocalDateTime
status               String   ('success'|'login_failed'|'timeout'|'error')
registrosColetados   Integer
mensagemErro         String (TEXT) nullable
```

---

## DTOs DE DESSERIALIZAÇÃO DA API CONCIFLEX (pacote dto/conciflex/)

Gere os records Java abaixo para desserializar as respostas das duas APIs do Conciflex.
Use @JsonProperty para mapear os campos em UPPER_CASE do JSON para camelCase Java.

### ConciliacaoTaxaApiResponse.java
```java
public record ConciliacaoTaxaApiResponse(
    List<ConciliacaoTaxaItem>  result,
    @JsonProperty("total_valor_bruto_auditadas")    BigDecimal totalValorBrutoAuditadas,
    @JsonProperty("registros_auditados")            Integer registrosAuditados,
    @JsonProperty("total_valor_bruto_nao_auditadas") BigDecimal totalValorBrutoNaoAuditadas,
    @JsonProperty("registros_nao_auditados")        Integer registrosNaoAuditados,
    @JsonProperty("total_taxa_cadastrada_praticada") BigDecimal totalTaxaCadastradaPraticada,
    @JsonProperty("total_taxas")                    BigDecimal totalTaxas,
    @JsonProperty("taxas_auditadas")                BigDecimal taxasAuditadas,
    @JsonProperty("taxas_nao_auditadas")            BigDecimal taxasNaoAuditadas,
    @JsonProperty("total_liquido")                  BigDecimal totalLiquido
) {}
```

### ConciliacaoTaxaItem.java
```java
// Cada item do array "result" da API /conciliacao-taxas/buscar
public record ConciliacaoTaxaItem(
    @JsonProperty("ID")                            String id,
    @JsonProperty("CODIGO_EMPRESA")                String codigoEmpresa,
    @JsonProperty("NOME_EMPRESA")                  String nomeEmpresa,
    @JsonProperty("CNPJ")                          String cnpj,
    @JsonProperty("DATA_VENDA")                    LocalDate dataVenda,
    @JsonProperty("ADQUIRENTE")                    String adquirente,
    @JsonProperty("CODIGO_ADQUIRENTE")             String codigoAdquirente,
    @JsonProperty("BANDEIRA")                      String bandeira,
    @JsonProperty("COD_BANDEIRA")                  String codBandeira,
    @JsonProperty("MODALIDADE")                    String modalidade,
    @JsonProperty("CODIGO_MODALIDADE")             String codigoModalidade,
    @JsonProperty("PRODUTO")                       String produto,
    @JsonProperty("CODIGO_PRODUTO")                String codigoProduto,
    @JsonProperty("VALOR_BRUTO")                   BigDecimal valorBruto,
    @JsonProperty("VALOR_DESCONTO")                BigDecimal valorDesconto,
    @JsonProperty("PERCENTUAL_TAXA")               BigDecimal percentualTaxa,
    @JsonProperty("TAXA_CONTRATADA")               BigDecimal taxaContratada,
    @JsonProperty("QUANTIDADE")                    Integer quantidade,
    @JsonProperty("TAXA_PRATICADA_RS")             BigDecimal taxaPraticadaRs,
    @JsonProperty("TAXA_PRATICADA_CADASTRADA_RS")  BigDecimal taxaPraticadaCadastradaRs,
    @JsonProperty("TAXA_CONTRATADA_RS")            BigDecimal taxaContratadaRs,
    @JsonProperty("TOTAL_TAXA_NAO_CONTRATADA_RS")  BigDecimal totalTaxaNaoContratadaRs,
    @JsonProperty("PERDA_RS")                      BigDecimal perdaRs,
    @JsonProperty("PERDA")                         BigDecimal perda,
    @JsonProperty("AUDITADA")                      String auditada,
    @JsonProperty("ESTABELECIMENTO")               String estabelecimento
) {}
```

### RecebimentoApiResponse.java
```java
public record RecebimentoApiResponse(
    List<RecebimentoItem>  result,
    BigDecimal             sum,
    @JsonProperty("sum_custo_taxa")       BigDecimal sumCustoTaxa,
    @JsonProperty("sum_valor_liquido")    BigDecimal sumValorLiquido,
    @JsonProperty("sum_taxa_antecipacao") BigDecimal sumTaxaAntecipacao,
    TotalizadorItem        antecipacao,
    @JsonProperty("tarifa_por_transacao") TotalizadorItem tarifaPorTransacao,
    @JsonProperty("ajuste_debito")        TotalizadorItem ajusteDebito,
    @JsonProperty("ajuste_credito")       TotalizadorItem ajusteCredito,
    TotalizadorItem        cancelamento,
    @JsonProperty("debito_gravame")       TotalizadorItem debitoGravame,
    @JsonProperty("credito_gravame")      TotalizadorItem creditoGravame
) {}

public record TotalizadorItem(Integer total, BigDecimal sum) {}
```

### RecebimentoItem.java
```java
// Cada item do array "result" da API /recebimentos-operadoras/buscar
// ATENÇÃO: valorBruto e valorLiquido são NEGATIVOS em registros de Ajuste a Débito
public record RecebimentoItem(
    @JsonProperty("ID")                        String id,
    @JsonProperty("COD_TIPO_LANCAMENTO")       String codTipoLancamento,
    @JsonProperty("TIPO_LANCAMENTO")           String tipoLancamento,
    @JsonProperty("COD_TIPO_PAGAMENTO")        String codTipoPagamento,
    @JsonProperty("TIPO_PAGAMENTO")            String tipoPagamento,
    @JsonProperty("NOME_EMPRESA")              String nomeEmpresa,
    @JsonProperty("CNPJ")                      String cnpj,
    @JsonProperty("DATA_VENDA")                LocalDate dataVenda,
    @JsonProperty("DATA_PREVISAO")             LocalDate dataPrevisao,
    @JsonProperty("DATA_PAGAMENTO")            LocalDate dataPagamento,
    @JsonProperty("DATA_CANCELAMENTO")         LocalDate dataCancelamento,
    @JsonProperty("COD_ADQUIRENTE")            String codAdquirente,
    @JsonProperty("ADQUIRENTE")                String adquirente,
    @JsonProperty("COD_BANDEIRA")              String codBandeira,
    @JsonProperty("BANDEIRA")                  String bandeira,
    @JsonProperty("MODALIDADE")                String modalidade,
    @JsonProperty("NSU")                       String nsu,
    @JsonProperty("AUTORIZACAO")               String autorizacao,
    @JsonProperty("CARTAO")                    String cartao,
    @JsonProperty("NUMERO_RESUMO_VENDA")       String numeroResumoVenda,
    @JsonProperty("VALOR_BRUTO")               BigDecimal valorBruto,
    @JsonProperty("TAXA_PERCENTUAL")           BigDecimal taxaPercentual,
    @JsonProperty("VALOR_TAXA")                BigDecimal valorTaxa,
    @JsonProperty("TARIFA_TRANSACAO")          BigDecimal tarifaTransacao,
    @JsonProperty("TAXA_ANTECIPACAO")          BigDecimal taxaAntecipacao,
    @JsonProperty("VALOR_TAXA_ANTECIPACAO")    BigDecimal valorTaxaAntecipacao,
    @JsonProperty("OUTRAS_DESPESAS")           BigDecimal outrasDespesas,
    @JsonProperty("VALOR_LIQUIDO")             BigDecimal valorLiquido,
    @JsonProperty("VALOR_LIQUIDO_S_ANTECIPACAO") BigDecimal valorLiquidoSAntecipacao,
    @JsonProperty("PARCELA")                   Integer parcela,
    @JsonProperty("TOTAL_PARCELAS")            Integer totalParcelas,
    @JsonProperty("POSSUI_TAXA_MINIMA")        String possuiTaxaMinima,
    @JsonProperty("ESTABELECIMENTO")           String estabelecimento,
    @JsonProperty("BANCO")                     String banco,
    @JsonProperty("AGENCIA")                   String agencia,
    @JsonProperty("CONTA_CORRENTE")            String contaCorrente,
    @JsonProperty("PRODUTO")                   String produto,
    @JsonProperty("MEIOCAPTURA")               String meioCaptura,
    @JsonProperty("STATUS_CONCILIACAO")        String statusConciliacao,
    @JsonProperty("CODIGO_OPERADORA_AJUSTE")   String codigoOperadoraAjuste,
    @JsonProperty("DESC_AJUSTE")               String descAjuste,
    @JsonProperty("CLASSIFICACAO_AJUSTE")      String classificacaoAjuste,
    @JsonProperty("AUTORIZADOR")               String autorizador,
    @JsonProperty("DATA_PROCESSAMENTO")        LocalDate dataProcessamento,
    @JsonProperty("HORA_PROCESSAMENTO")        String horaProcessamento,
    @JsonProperty("NOME_ARQUIVO")              String nomeArquivo,
    @JsonProperty("OBSERVACOES")               String observacoes
) {}
```

---

## ENDPOINTS REST (gere todos)

### Auth
POST   /api/auth/login          → body: {login, senha} → retorna JWT

### Clientes
GET    /api/clientes                        → lista todos
GET    /api/clientes/{id}                   → busca por ID
POST   /api/clientes                        → cria (criptografa credenciais antes de salvar)
PUT    /api/clientes/{id}                   → atualiza
DELETE /api/clientes/{id}                   → inativa (soft delete, ativo=false)

### Estabelecimentos
GET    /api/clientes/{clienteId}/estabelecimentos        → lista por cliente
POST   /api/clientes/{clienteId}/estabelecimentos        → cria
PUT    /api/estabelecimentos/{id}                        → atualiza
DELETE /api/estabelecimentos/{id}                        → inativa

### Auditoria
GET    /api/auditoria/{estabelecimentoId}?dataInicio=&dataFim=
→ Retorna: total de transações, total cobrado a mais, total cobrado a menos,
           resumo agrupado por bandeira [{bandeira, qtd, diferencaTotal}]

### Recebimentos
GET    /api/recebimentos/{estabelecimentoId}?dataInicio=&dataFim=
→ Retorna: totalRecebido, totalDescontado, detalhe por bandeira

### Mensagens
POST   /api/mensagens/gerar
  body: { clienteId, estabelecimentoId, dataInicio, dataFim, modo: "ia"|"template" }
  → Retorna mensagem gerada (não envia ainda)

POST   /api/mensagens/enviar
  body: { clienteId, conteudo }
  → Envia via Meta Cloud API e salva no histórico

GET    /api/mensagens/{clienteId}           → histórico de mensagens do cliente

### Webhook Meta
POST   /api/webhook/meta                   → recebe callbacks de status (verificação GET também)

### Logs
GET    /api/logs/coleta?estabelecimentoId=&status=    → consulta logs do coletor

---

## SERVIÇOS — COMPORTAMENTO ESPERADO

### CryptoService
- Usar AES-256-GCM com chave derivada de uma variável de ambiente (CRYPTO_SECRET_KEY)
- Métodos: encrypt(String plain) → String base64, decrypt(String encrypted) → String plain
- Usado EXCLUSIVAMENTE para credenciais Conciflex

### AnthropicService
- Chamar POST https://api.anthropic.com/v1/messages
- Header: x-api-key: ${ANTHROPIC_API_KEY}, anthropic-version: 2023-06-01
- Model: claude-sonnet-4-20250514, max_tokens: 1500
- Montar prompt com os dados de auditoria e recebimentos do período
- Retornar o texto gerado

### WhatsAppService
- Chamar POST https://graph.facebook.com/v19.0/${META_PHONE_NUMBER_ID}/messages
- Header: Authorization: Bearer ${META_ACCESS_TOKEN}
- Body: { messaging_product: "whatsapp", to: numero, type: "text", text: { body: conteudo } }
- Retornar o wamid (message id da Meta)
- Salvar na tabela mensagens_enviadas com status inicial "sent"

### AuditoriaService
- Consultar ConciliacaoTaxaRepository por estabelecimento + período
- Calcular: somaDiferencaPositiva (cobrou a mais), somaDiferencaNegativa (cobrou a menos)
- Agrupar por bandeira com sum(diferencaTaxa) e count
- Retornar AuditoriaResumoResponse

### MensagemService
- Se modo = "ia": montar prompt com dados de auditoria + recebimentos e chamar AnthropicService
- Se modo = "template": substituir variáveis em template fixo configurável (pode ser String em properties)
- Não salvar ainda — apenas retornar o texto para o operador revisar

---

## SEGURANÇA

- Spring Security com filtro JWT em todas as rotas exceto POST /api/auth/login e POST /api/webhook/meta
- Usuário ADM único — pode ser seed no Flyway (login/senha configuráveis por env vars)
- JWT com claims: sub (login), role (ADM), exp (configurável, default 8h)
- Webhook Meta: validar assinatura X-Hub-Signature-256 com META_WEBHOOK_SECRET

---

## MIGRATIONS FLYWAY (gere os arquivos SQL)

Gere os arquivos na ordem:
V1__create_clientes.sql
V2__create_estabelecimentos.sql
V3__create_conciliacao_taxas.sql
V4__create_recebimentos.sql
V5__create_mensagens_enviadas.sql
V6__create_logs_coleta.sql
V7__seed_admin_user.sql   ← tabela users com um registro ADM (senha bcrypt de env var)

---

## DOCKER

### Dockerfile (multi-stage)
```
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### application.yml (gere com placeholders de env vars)
Incluir:
- datasource: url, username, password via env
- jpa: ddl-auto=validate (Flyway cuida das migrations)
- flyway: enabled=true
- jwt.secret, jwt.expiration
- crypto.secret-key
- anthropic.api-key
- meta.access-token, meta.phone-number-id, meta.webhook-secret

---

## TRATAMENTO DE ERROS

GlobalExceptionHandler com @RestControllerAdvice:
- ResourceNotFoundException → 404
- IllegalArgumentException → 400
- IntegrationException (falhas Anthropic/Meta) → 502
- Exception genérica → 500
Sempre retornar body: { timestamp, status, error, message, path }

---

## O QUE GERAR

1. pom.xml completo
2. application.yml
3. Dockerfile
4. Todas as entidades JPA
5. Todos os repositórios (interfaces JpaRepository com queries customizadas onde necessário)
6. Todos os services com a lógica descrita
7. Todos os controllers com os endpoints descritos
8. DTOs request/response
9. Mappers MapStruct
10. SecurityConfig + JwtFilter
11. CryptoService (AES-256-GCM)
12. GlobalExceptionHandler
13. Migrations Flyway (V1 a V7)
14. ApiApplication.java

Gere cada arquivo completo, sem omitir imports ou anotações.
Prefira clareza e completude à brevidade.
Adicione comentários em português nos trechos de lógica de negócio relevantes.
