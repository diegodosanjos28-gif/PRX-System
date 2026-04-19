# PROMPT — Geração do Microserviço Coletor Spring Boot
# Sistema de Conciliação Financeira — Playwright + PostgreSQL
# Versão: v1.0 | Data: 2026-04-19
# ─────────────────────────────────────────────────────────────────────────────

---

## ⚠️ INSTRUÇÃO PRIMÁRIA

Leia este documento inteiro antes de gerar qualquer código.
Gere todos os arquivos completos, sem omitir imports ou anotações.
Adicione comentários em português nos trechos de lógica de negócio relevantes.
Este microserviço é INDEPENDENTE do backend principal mas compartilha o mesmo banco PostgreSQL.

---

## CONTEXTO DO SISTEMA

Você é um engenheiro sênior Java especializado em Spring Boot e automação web.
Este é o **microserviço coletor** de um sistema de conciliação financeira.

Sua única responsabilidade é:
1. Ler todos os clientes e estabelecimentos ativos do banco PostgreSQL
2. Para cada estabelecimento: autenticar no Conciflex via Playwright (browser headless),
   navegar pelo fluxo de login, coletar dados financeiros via chamadas HTTP internas
   e persistir os resultados nas tabelas compartilhadas com o backend principal
3. Registrar o resultado de cada coleta na tabela `logs_coleta`

O microserviço NÃO expõe endpoints de negócio. Expõe apenas:
- `GET /actuator/health` — health check
- `POST /api/coleta/iniciar` — dispara coleta manual para todos os clientes ativos
- `POST /api/coleta/cliente/{clienteId}` — dispara coleta manual para um cliente específico

---

## STACK OBRIGATÓRIA

- Java 17
- Spring Boot 3.x (última estável)
- Spring Data JPA + Hibernate (acesso direto ao PostgreSQL compartilhado)
- PostgreSQL driver
- **Microsoft Playwright for Java** (`com.microsoft.playwright:playwright`) — NÃO usar Selenium
- Lombok
- Jackson (desserialização das respostas JSON da API Conciflex)
- SLF4J + Logback (logs estruturados JSON)
- Spring Scheduling (`@EnableScheduling`) para o cron job
- Maven (pom.xml)
- Docker (Dockerfile multi-stage JDK 17 builder + JRE 17 Alpine runtime)

---

## VARIÁVEIS DE AMBIENTE OBRIGATÓRIAS

```
# Banco de dados (mesmo do backend principal)
DB_URL=jdbc:postgresql://localhost:5432/conciliacao
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Criptografia (mesma chave do backend — para descriptografar credenciais Conciflex)
CRYPTO_SECRET_KEY=chave-aes-256-em-base64

# Configuração de coleta
COLETA_DIAS_RETROATIVOS=1          # Padrão: último dia (D-1). Configurável.
COLETA_CRON=0 0 17 * * *           # Todos os dias às 17h
COLETA_TIMEOUT_SEGUNDOS=60         # Timeout por estabelecimento
COLETA_MAX_RETRIES=3               # Máximo de tentativas por estabelecimento

# Playwright
PLAYWRIGHT_HEADLESS=true           # true em produção, false para debug
```

---

## ESTRUTURA DE PACOTES (siga exatamente)

```
com.conciliacao.coletor
├── ColetorApplication.java
├── config/
│   ├── PlaywrightConfig.java          ← Bean Playwright (BrowserType + Browser reutilizável)
│   └── CryptoConfig.java              ← Mesma lógica do backend: AES-256-GCM
├── entity/                            ← COPIAR/REUSAR entidades do backend (mesmas tabelas)
│   ├── Cliente.java
│   ├── Estabelecimento.java
│   ├── ConciliacaoTaxa.java
│   ├── Recebimento.java
│   ├── ResumoColeta.java
│   └── LogColeta.java
├── repository/
│   ├── ClienteRepository.java
│   ├── EstabelecimentoRepository.java
│   ├── ConciliacaoTaxaRepository.java
│   ├── RecebimentoRepository.java
│   ├── ResumoColetaRepository.java
│   └── LogColetaRepository.java
├── dto/
│   ├── ConciliacaoTaxaApiResponse.java    ← Desserialização da API Conciflex
│   ├── ConciliacaoTaxaItem.java
│   ├── RecebimentoApiResponse.java
│   ├── RecebimentoItem.java
│   └── TotalizadorItem.java
├── service/
│   ├── CryptoService.java                 ← AES-256-GCM decrypt para credenciais
│   ├── PlaywrightSessionService.java      ← Toda a lógica de navegação Playwright
│   ├── ColetorService.java                ← Orquestra o processo completo por estabelecimento
│   └── ColetaAgendadaService.java         ← @Scheduled + lógica de iteração sobre clientes
├── controller/
│   └── ColetaController.java              ← Endpoints manuais (POST /api/coleta/...)
└── exception/
    ├── ColetaException.java
    └── LoginConciflex Exception.java
```

---

## ENTIDADES JPA

As entidades abaixo são **idênticas** às do backend principal. Mapeiam as mesmas tabelas.
O microserviço usa `spring.jpa.ddl-auto=validate` — ele nunca cria nem altera schema.
As migrations Flyway são responsabilidade do backend principal.

### Cliente.java
```java
@Entity
@Table(name = "clientes")
public class Cliente {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid") private UUID id;
    @Column(name = "razao_social") private String razaoSocial;
    @Column(name = "nome_fantasia") private String nomeFantasia;
    @Column(name = "cnpj", length = 14) private String cnpj;
    @Column(name = "whatsapp") private String whatsapp;
    @Column(name = "conciflex_login", columnDefinition = "TEXT") private String conciflex_login; // CRIPTOGRAFADO
    @Column(name = "conciflex_senha", columnDefinition = "TEXT") private String conciflex_senha;  // CRIPTOGRAFADO
    @Column(name = "ativo") private boolean ativo;
    @Column(name = "observacoes", columnDefinition = "TEXT") private String observacoes;
    @CreationTimestamp @Column(name = "criado_em") private LocalDateTime criadoEm;
    @OneToMany(mappedBy = "cliente", fetch = FetchType.LAZY)
    private List<Estabelecimento> estabelecimentos = new ArrayList<>();
}
```

### Estabelecimento.java
```java
@Entity
@Table(name = "estabelecimentos")
public class Estabelecimento {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid") private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id") private Cliente cliente;
    @Column(name = "descricao") private String descricao;
    // IMPORTANTE: este campo contém o texto EXATO que aparece no dropdown do modal
    // "Clientes" do Conciflex (ex: "Churrascaria Picanhas Grill").
    // É usado para localizar e selecionar o option correto no <select> do modal.
    @Column(name = "identificador_conciflex") private String identificadorConciflex;
    @Column(name = "ativo") private boolean ativo;
}
```

### ConciliacaoTaxa.java
```java
// Mapeamento completo — ver entidade do backend. Campos chave para UPSERT:
// id_conciflex + estabelecimento_id (constraint unique para idempotência)
@Entity @Table(name = "conciliacao_taxas",
    uniqueConstraints = @UniqueConstraint(columnNames = {"id_conciflex", "estabelecimento_id"}))
// [campos idênticos ao backend — gere completo com todos os @Column]
```

### Recebimento.java
```java
// Mapeamento completo — ver entidade do backend. Campos chave para UPSERT:
// id_conciflex + estabelecimento_id
@Entity @Table(name = "recebimentos",
    uniqueConstraints = @UniqueConstraint(columnNames = {"id_conciflex", "estabelecimento_id"}))
// [campos idênticos ao backend — gere completo com todos os @Column]
// ATENÇÃO: valorBruto e valorLiquido são NEGATIVOS em registros de Ajuste a Débito (cod=2)
```

### ResumoColeta.java
```java
@Entity @Table(name = "resumo_coleta")
public class ResumoColeta {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne @JoinColumn(name = "estabelecimento_id") private Estabelecimento estabelecimento;
    @Column(name = "tipo") private String tipo; // "conciliacao_taxas" ou "recebimentos"
    @Column(name = "data_inicio") private LocalDate dataInicio;
    @Column(name = "data_fim") private LocalDate dataFim;
    @Column(name = "total_registros") private Integer totalRegistros;
    @Column(name = "valor_bruto_total", precision=18, scale=4) private BigDecimal valorBrutoTotal;
    @Column(name = "valor_liquido_total", precision=18, scale=6) private BigDecimal valorLiquidoTotal;
    @Column(name = "total_taxas", precision=15, scale=4) private BigDecimal totalTaxas;
    @Column(name = "totalizadores_json", columnDefinition="TEXT") private String totalizadoresJson;
    @Column(name = "coletado_em") private LocalDateTime coletadoEm;
}
```

### LogColeta.java
```java
@Entity @Table(name = "logs_coleta")
public class LogColeta {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne @JoinColumn(name = "estabelecimento_id") private Estabelecimento estabelecimento;
    @Column(name = "executado_em") private LocalDateTime executadoEm;
    // 'success' | 'login_failed' | 'timeout' | 'error'
    @Column(name = "status") private String status;
    @Column(name = "registros_coletados") private Integer registrosColetados;
    @Column(name = "mensagem_erro", columnDefinition="TEXT") private String mensagemErro;
}
```

---

## DTOs DE DESSERIALIZAÇÃO DA API CONCIFLEX

### ConciliacaoTaxaApiResponse.java
```java
// Resposta completa de POST /conciliacao-taxas/buscar
// Campos raiz observados na resposta real de produção:
public record ConciliacaoTaxaApiResponse(
    List<ConciliacaoTaxaItem> result,
    @JsonProperty("total_valor_bruto_auditadas")     BigDecimal totalValorBrutoAuditadas,
    @JsonProperty("registros_auditados")             Integer registrosAuditados,
    @JsonProperty("total_valor_bruto_nao_auditadas") BigDecimal totalValorBrutoNaoAuditadas,
    @JsonProperty("registros_nao_auditados")         Integer registrosNaoAuditados,
    @JsonProperty("total_taxa_cadastrada_praticada") BigDecimal totalTaxaCadastradaPraticada,
    @JsonProperty("total_taxas")                     BigDecimal totalTaxas,
    @JsonProperty("taxas_auditadas")                 BigDecimal taxasAuditadas,
    @JsonProperty("taxas_nao_auditadas")             BigDecimal taxasNaoAuditadas,
    @JsonProperty("total_liquido")                   BigDecimal totalLiquido
) {}
```

### ConciliacaoTaxaItem.java
```java
// Cada elemento do array "result". Campos UPPER_CASE conforme API real.
// ATENÇÃO: campos numéricos chegam como String OU Number — usar @JsonDeserialize
// ou configurar ObjectMapper com ALLOW_COERCION_OF_SCALARS
public record ConciliacaoTaxaItem(
    @JsonProperty("ID")                           String id,
    @JsonProperty("CODIGO_EMPRESA")               String codigoEmpresa,
    @JsonProperty("NOME_EMPRESA")                 String nomeEmpresa,
    @JsonProperty("CNPJ")                         String cnpj,
    @JsonProperty("DATA_VENDA")                   LocalDate dataVenda,
    @JsonProperty("ADQUIRENTE")                   String adquirente,
    @JsonProperty("CODIGO_ADQUIRENTE")            String codigoAdquirente,
    @JsonProperty("BANDEIRA")                     String bandeira,
    @JsonProperty("COD_BANDEIRA")                 String codBandeira,
    @JsonProperty("MODALIDADE")                   String modalidade,
    @JsonProperty("CODIGO_MODALIDADE")            String codigoModalidade,
    @JsonProperty("PRODUTO")                      String produto,
    @JsonProperty("CODIGO_PRODUTO")               String codigoProduto,
    @JsonProperty("VALOR_BRUTO")                  BigDecimal valorBruto,
    @JsonProperty("VALOR_DESCONTO")               BigDecimal valorDesconto,
    @JsonProperty("PERCENTUAL_TAXA")              BigDecimal percentualTaxa,
    @JsonProperty("TAXA_CONTRATADA")              BigDecimal taxaContratada,
    @JsonProperty("QUANTIDADE")                   Integer quantidade,
    @JsonProperty("TAXA_PRATICADA_RS")            BigDecimal taxaPraticadaRs,
    @JsonProperty("TAXA_PRATICADA_CADASTRADA_RS") BigDecimal taxaPraticadaCadastradaRs,
    @JsonProperty("TAXA_CONTRATADA_RS")           BigDecimal taxaContratadaRs,
    @JsonProperty("TOTAL_TAXA_NAO_CONTRATADA_RS") BigDecimal totalTaxaNaoContratadaRs,
    @JsonProperty("PERDA_RS")                     BigDecimal perdaRs,
    @JsonProperty("PERDA")                        BigDecimal perda,
    @JsonProperty("AUDITADA")                     String auditada,
    @JsonProperty("ESTABELECIMENTO")              String estabelecimento
    // Campos ignorados: ADQUIRENTE_IMAGEM, BANDEIRA_IMAGEM (não persistidos)
) {}
```

### RecebimentoApiResponse.java
```java
// Resposta completa de POST /recebimentos-operadoras/buscar
// Campos raiz observados na resposta real de produção:
public record RecebimentoApiResponse(
    List<RecebimentoItem> result,
    BigDecimal sum,                                           // total bruto
    @JsonProperty("sum_custo_taxa")       BigDecimal sumCustoTaxa,
    @JsonProperty("sum_valor_liquido")    BigDecimal sumValorLiquido,
    @JsonProperty("sum_taxa_antecipacao") BigDecimal sumTaxaAntecipacao,
    TotalizadorItem antecipacao,
    @JsonProperty("tarifa_por_transacao") TotalizadorItem tarifaPorTransacao,
    @JsonProperty("ajuste_debito")        TotalizadorItem ajusteDebito,
    @JsonProperty("ajuste_credito")       TotalizadorItem ajusteCredito,
    TotalizadorItem cancelamento,
    @JsonProperty("debito_gravame")       TotalizadorItem debitoGravame,
    @JsonProperty("credito_gravame")      TotalizadorItem creditoGravame
) {}

public record TotalizadorItem(Integer total, BigDecimal sum) {}
```

### RecebimentoItem.java
```java
// Cada elemento do array "result". Campos confirmados com dados reais de produção.
// ATENÇÃO: campos null devem ser aceitos sem exceção (use @JsonInclude(ALWAYS))
// ATENÇÃO: valorBruto e valorLiquido NEGATIVOS em Ajuste a Débito (COD_TIPO_LANCAMENTO="2")
public record RecebimentoItem(
    @JsonProperty("ID")                          String id,
    @JsonProperty("COD_TIPO_LANCAMENTO")         String codTipoLancamento,
    @JsonProperty("TIPO_LANCAMENTO")             String tipoLancamento,
    @JsonProperty("COD_TIPO_PAGAMENTO")          String codTipoPagamento,
    @JsonProperty("TIPO_PAGAMENTO")              String tipoPagamento,
    @JsonProperty("NOME_EMPRESA")                String nomeEmpresa,
    @JsonProperty("CNPJ")                        String cnpj,
    @JsonProperty("DATA_VENDA")                  LocalDate dataVenda,
    @JsonProperty("DATA_PREVISAO")               LocalDate dataPrevisao,
    @JsonProperty("DATA_PAGAMENTO")              LocalDate dataPagamento,
    @JsonProperty("DATA_CANCELAMENTO")           LocalDate dataCancelamento,
    @JsonProperty("COD_ADQUIRENTE")              String codAdquirente,
    @JsonProperty("ADQUIRENTE")                  String adquirente,
    @JsonProperty("COD_BANDEIRA")                String codBandeira,
    @JsonProperty("BANDEIRA")                    String bandeira,
    @JsonProperty("MODALIDADE")                  String modalidade,
    @JsonProperty("NSU")                         String nsu,
    @JsonProperty("AUTORIZACAO")                 String autorizacao,
    @JsonProperty("CARTAO")                      String cartao,         // mascarado ex: "506755******6026"
    @JsonProperty("NUMERO_RESUMO_VENDA")         String numeroResumoVenda,
    @JsonProperty("VALOR_BRUTO")                 BigDecimal valorBruto,
    @JsonProperty("TAXA_PERCENTUAL")             BigDecimal taxaPercentual,
    @JsonProperty("VALOR_TAXA")                  BigDecimal valorTaxa,
    @JsonProperty("TARIFA_TRANSACAO")            BigDecimal tarifaTransacao,
    @JsonProperty("TAXA_ANTECIPACAO")            BigDecimal taxaAntecipacao,
    @JsonProperty("VALOR_TAXA_ANTECIPACAO")      BigDecimal valorTaxaAntecipacao,
    @JsonProperty("OUTRAS_DESPESAS")             BigDecimal outrasDespesas,
    @JsonProperty("VALOR_LIQUIDO")               BigDecimal valorLiquido,
    @JsonProperty("VALOR_LIQUIDO_S_ANTECIPACAO") BigDecimal valorLiquidoSAntecipacao,
    @JsonProperty("PARCELA")                     Integer parcela,
    @JsonProperty("TOTAL_PARCELAS")              Integer totalParcelas,
    @JsonProperty("POSSUI_TAXA_MINIMA")          String possuiTaxaMinima,
    @JsonProperty("ESTABELECIMENTO")             String estabelecimento,
    @JsonProperty("BANCO")                       String banco,
    @JsonProperty("AGENCIA")                     String agencia,
    @JsonProperty("CONTA_CORRENTE")              String contaCorrente,
    @JsonProperty("PRODUTO")                     String produto,
    @JsonProperty("MEIOCAPTURA")                 String meioCaptura,
    @JsonProperty("STATUS_CONCILIACAO")          String statusConciliacao,
    @JsonProperty("CODIGO_OPERADORA_AJUSTE")     String codigoOperadoraAjuste,
    @JsonProperty("DESC_AJUSTE")                 String descAjuste,
    @JsonProperty("CLASSIFICACAO_AJUSTE")        String classificacaoAjuste,
    @JsonProperty("AUTORIZADOR")                 String autorizador,
    @JsonProperty("DATA_PROCESSAMENTO")          LocalDate dataProcessamento,
    @JsonProperty("HORA_PROCESSAMENTO")          String horaProcessamento,
    @JsonProperty("NOME_ARQUIVO")                String nomeArquivo,
    @JsonProperty("OBSERVACOES")                 String observacoes
    // Campos ignorados (não mapeados na entidade):
    // ID_VENDA_ERP, TID, VALOR_SUBSIDIO_ESTABELECIMENTO, VALOR_SUBSIDIO_IFOOD,
    // VALOR_COMISSAO_DELIVERY, NUMERO_OPERACAO_ANTECIPACAO, COD_TIPO_AJUSTE_SISTEMA,
    // NUMERO_TERMINAL, SUBSTITUTO, CAMPO1, CAMPO2, CAMPO3, DIVERGENCIA,
    // COD_STATUS_EXTRATO_BANCARIO, STATUS_EXTRATO_BANCARIO, RETORNO_ERP_BAIXA,
    // OBSERVACAO_BAIXA, DATA_CONCILIADO, HORA_CONCILIADO, MCC, CHAVE_UR,
    // id_negociador, nome_fantasia_negociador, imagem_negociador
) {}
```

---

## CRYPTOSERVICE — AES-256-GCM

O CryptoService descriptografa as credenciais Conciflex armazenadas no banco.
A lógica é IDÊNTICA ao backend principal. A chave vem de `${CRYPTO_SECRET_KEY}` (Base64).

```java
@Service
public class CryptoService {
    // Descriptografar: recebe String base64 criptografada → retorna plaintext
    // Algoritmo: AES/GCM/NoPadding, IV de 12 bytes prefixado no ciphertext
    public String decrypt(String encryptedBase64) { ... }
    // O método encrypt não é necessário neste microserviço
}
```

---

## PLAYWRITESESSIONSERVICE — FLUXO COMPLETO DE NAVEGAÇÃO

Este é o componente mais crítico. Implemente com atenção cada passo.

### URL base
`https://login.conciflex.com.br`

### Fluxo de navegação (passo a passo)

```
PASSO 1 — Abrir página de login
  URL: https://login.conciflex.com.br/login
  Aguardar: campo "Usuário" visível (input com label "Usuário *")

PASSO 2 — Preencher credenciais
  Campo usuário: input com placeholder ou label "Usuário" → preencher com login descriptografado
  Campo senha:   input com label "Senha" (type=password)  → preencher com senha descriptografada
  Clicar:        botão "Entrar" (texto "Entrar")

PASSO 3 — Modal "Clientes" (selectbox de estabelecimento)
  Aguardar: modal com título "Clientes" visível
             (elemento: div.modal com h5 ou elemento contendo texto "Escolha o cliente:")
  Selecionar no <select>: o option cujo texto é EXATAMENTE igual ao campo
                           estabelecimento.identificadorConciflex
             (ex: "Churrascaria Picanhas Grill")
  Clicar: botão "ENTRAR" dentro do modal

PASSO 4 — Modal de seleção de módulo (aparece após selecionar o cliente)
  Aguardar: modal com texto "Selecione o módulo desejado para acessar:" visível
  Localizar: card com texto "Cartões"
             (div#card-modulo-cartoes ou div com onclick="selecionarModulo('card-modulo-cartoes', '1')")
  Clicar: no card "Cartões"

PASSO 5 — Aguardar navegação pós-seleção de módulo
  Aguardar: URL mudar OU algum elemento da página de cartões aparecer
             (aguardar networkidle ou timeout de 3 segundos)

PASSO 6 — Extrair cookies de sessão
  Extrair do contexto Playwright os cookies:
    - laravel_session  (obrigatório)
    - XSRF-TOKEN       (obrigatório — valor URL-decoded)
    - cf_clearance     (obrigatório — Cloudflare)
  Armazenar em objeto ConcifixSession { laravelSession, xsrfToken, cfClearance }

PASSO 7 — Extrair _token CSRF do HTML (token do formulário Laravel)
  Após o login, fazer page.evaluate() para buscar:
    document.querySelector('meta[name="csrf-token"]')?.content
  OU navegar até a página de conciliação e extrair do form:
    document.querySelector('input[name="_token"]')?.value
  Este valor é o _token enviado no body das requisições POST.
```

### Objeto de sessão
```java
public record ConcifixSession(
    String laravelSession,
    String xsrfToken,       // valor decodificado (URL decode do cookie XSRF-TOKEN)
    String cfClearance,
    String csrfToken        // valor do campo _token do formulário Laravel
) {}
```

### Chamadas HTTP após capturar a sessão

Após extrair os cookies, as chamadas às APIs do Conciflex são feitas via **RestTemplate** (não Playwright), usando os cookies como headers:

```java
// Headers para todas as chamadas:
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.MULTIPART_FORM_DATA);
headers.set("Cookie",
    "cf_clearance=" + session.cfClearance() + "; " +
    "laravel_session=" + session.laravelSession() + "; " +
    "XSRF-TOKEN=" + URLEncoder.encode(session.xsrfToken(), UTF_8));

// Body (multipart/form-data):
MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
body.add("_token", session.csrfToken());
body.add("data_inicial", dataInicio.toString());   // formato: "2026-04-09"
body.add("data_final",   dataFim.toString());       // formato: "2026-04-18"
```

#### Endpoint 1 — Conciliação de Taxas
```
POST https://login.conciflex.com.br/conciliacao-taxas/buscar
Content-Type: multipart/form-data
Body: _token, data_inicial, data_final
Resposta: ConciliacaoTaxaApiResponse (ver DTO acima)
```

#### Endpoint 2 — Recebimentos
```
POST https://login.conciflex.com.br/recebimentos-operadoras/buscar
Content-Type: multipart/form-data
Body: _token, data_inicial, data_final
Resposta: RecebimentoApiResponse (ver DTO acima)
```

### Configuração ObjectMapper para os campos numéricos mistos
A API Conciflex retorna alguns campos numéricos ora como String, ora como Number:
```
"VALOR_BRUTO": "32.17"       ← String
"PERCENTUAL_TAXA": 3.61      ← Number
"PERDA_RS": 0                ← Integer
```
Configure o ObjectMapper com:
```java
objectMapper.enable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
```

---

## COLETORSERVICE — ORQUESTRAÇÃO POR ESTABELECIMENTO

```java
@Service
public class ColetorService {

    /**
     * Executa o ciclo completo de coleta para um único estabelecimento.
     * 1. Descriptografa credenciais do cliente
     * 2. Abre sessão Playwright e navega pelo fluxo de login
     * 3. Seleciona o estabelecimento correto no modal pelo identificadorConciflex
     * 4. Clica em "Cartões" para ativar a sessão
     * 5. Extrai cookies laravel_session, XSRF-TOKEN, cf_clearance e _token csrf
     * 6. Chama POST /conciliacao-taxas/buscar e persiste com UPSERT
     * 7. Chama POST /recebimentos-operadoras/buscar e persiste com UPSERT
     * 8. Persiste totalizadores em resumo_coleta
     * 9. Registra log_coleta com status 'success' e quantidade de registros
     *
     * Em caso de falha: registra log_coleta com status correto e relança exceção
     * para o ColetaAgendadaService controlar o fluxo (não interromper demais clientes).
     */
    public ColetaResultado coletar(Estabelecimento estabelecimento, LocalDate dataInicio, LocalDate dataFim) {
        // Implementar com try/catch separando LoginConciflex Exception (status=login_failed)
        // de TimeoutException (status=timeout) e Exception genérica (status=error)
    }
}
```

### UPSERT — Idempotência

Use `@Query` nativa para UPSERT baseado em `(id_conciflex, estabelecimento_id)`:

```java
// ConciliacaoTaxaRepository
@Modifying
@Query(value = """
    INSERT INTO conciliacao_taxas (id, estabelecimento_id, id_conciflex, ...)
    VALUES (:id, :estabelecimentoId, :idConciflex, ...)
    ON CONFLICT (id_conciflex, estabelecimento_id)
    DO UPDATE SET
        data_venda = EXCLUDED.data_venda,
        valor_bruto = EXCLUDED.valor_bruto,
        -- todos os campos atualizáveis
        coletado_em = EXCLUDED.coletado_em
    """, nativeQuery = true)
void upsert(@Param("id") UUID id, @Param("estabelecimentoId") UUID estabelecimentoId, ...);
```

O mesmo padrão se aplica a `RecebimentoRepository`.

---

## COLETAAGENDADASERVICE — CRON + RETRY

```java
@Service
@EnableScheduling
public class ColetaAgendadaService {

    /**
     * Job agendado: executa todos os dias no horário configurado em COLETA_CRON.
     * Itera sobre todos os clientes ativos e seus estabelecimentos ativos.
     * Falha em um estabelecimento NÃO interrompe o processamento dos demais.
     * Implementar retry com backoff exponencial: 1s → 2s → 4s (max 3 tentativas).
     */
    @Scheduled(cron = "${COLETA_CRON:0 0 17 * * *}")
    public void executarColetaAgendada() { ... }

    /**
     * Período de coleta: de (hoje - COLETA_DIAS_RETROATIVOS) até ontem (D-1).
     * Exemplo: COLETA_DIAS_RETROATIVOS=1 → coleta apenas o dia anterior.
     * COLETA_DIAS_RETROATIVOS=30 → coleta os últimos 30 dias.
     */
    private LocalDate[] calcularPeriodo() {
        int diasRetroativos = Integer.parseInt(env.getProperty("COLETA_DIAS_RETROATIVOS", "1"));
        LocalDate fim = LocalDate.now().minusDays(1);         // D-1
        LocalDate inicio = fim.minusDays(diasRetroativos - 1);
        return new LocalDate[]{inicio, fim};
    }

    /**
     * Executa coleta com retry e backoff exponencial.
     * Tentativa 1: imediata
     * Tentativa 2: aguarda 1s
     * Tentativa 3: aguarda 2s
     * Após 3 falhas: registra log com status 'error' e continua para próximo.
     */
    private void coletarComRetry(Estabelecimento estabelecimento,
                                  LocalDate inicio, LocalDate fim) { ... }
}
```

---

## COLETACONTROLLER — ENDPOINTS MANUAIS

```java
@RestController
@RequestMapping("/api/coleta")
public class ColetaController {

    /**
     * POST /api/coleta/iniciar
     * Dispara coleta para TODOS os clientes e estabelecimentos ativos.
     * Retorna imediatamente HTTP 202 (Accepted) e executa em background (@Async).
     */
    @PostMapping("/iniciar")
    public ResponseEntity<Map<String, String>> iniciarColetaGeral() { ... }

    /**
     * POST /api/coleta/cliente/{clienteId}
     * Dispara coleta apenas para um cliente específico (todos os seus estabelecimentos).
     * Retorna HTTP 202 imediatamente, executa em background.
     * Retorna HTTP 404 se o cliente não existir ou estiver inativo.
     */
    @PostMapping("/cliente/{clienteId}")
    public ResponseEntity<Map<String, String>> iniciarColetaCliente(
            @PathVariable UUID clienteId) { ... }
}
```

---

## PLAYWRIGHTCONFIG — GERENCIAMENTO DO BROWSER

```java
@Configuration
public class PlaywrightConfig {

    /**
     * Playwright e Browser são criados uma única vez na inicialização.
     * Cada coleta abre um novo BrowserContext (sessão isolada) e fecha após uso.
     * Isso evita vazamento de sessão entre clientes.
     */
    @Bean
    public Playwright playwright() {
        return Playwright.create();
    }

    @Bean
    public Browser browser(Playwright playwright) {
        // Configurações para evitar detecção de bot:
        // - headless configurável via ${PLAYWRIGHT_HEADLESS:true}
        // - userAgent: Mozilla/5.0 Windows Chrome (navegador real)
        // - viewport: 1280x720
        // - locale: pt-BR
        return playwright.chromium().launch(new BrowserType.LaunchOptions()
            .setHeadless(Boolean.parseBoolean(System.getenv().getOrDefault("PLAYWRIGHT_HEADLESS", "true")))
            .setArgs(List.of(
                "--no-sandbox",
                "--disable-setuid-sandbox",
                "--disable-dev-shm-usage",  // essencial em Docker
                "--disable-blink-features=AutomationControlled"  // evita detecção
            )));
    }
}
```

---

## APPLICATION.YML

```yaml
spring:
  application:
    name: conciliacao-coletor
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/conciliacao}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate        # NÃO criar nem alterar schema — Flyway é do backend
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: false
  flyway:
    enabled: false              # Flyway desabilitado — gerenciado pelo backend principal

server:
  port: 8081                    # Porta diferente do backend (8080)

management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: always

coleta:
  dias-retroativos: ${COLETA_DIAS_RETROATIVOS:1}
  cron: ${COLETA_CRON:0 0 17 * * *}
  timeout-segundos: ${COLETA_TIMEOUT_SEGUNDOS:60}
  max-retries: ${COLETA_MAX_RETRIES:3}

crypto:
  secret-key: ${CRYPTO_SECRET_KEY}

playwright:
  headless: ${PLAYWRIGHT_HEADLESS:true}

logging:
  pattern:
    console: '{"timestamp":"%d{ISO8601}","level":"%p","service":"coletor","logger":"%logger{36}","message":"%msg"}%n'
```

---

## POM.XML — DEPENDÊNCIAS OBRIGATÓRIAS

```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- PostgreSQL -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Playwright -->
    <dependency>
        <groupId>com.microsoft.playwright</groupId>
        <artifactId>playwright</artifactId>
        <version>1.44.0</version>  <!-- usar última estável -->
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Jackson -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.datatype</groupId>
        <artifactId>jackson-datatype-jsr310</artifactId>
    </dependency>

    <!-- Tests -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## DOCKERFILE MULTI-STAGE

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: Runtime com Playwright
# Usar imagem com Chromium disponível para Playwright headless
FROM mcr.microsoft.com/playwright/java:v1.44.0-jammy
WORKDIR /app

# Copiar JAR
COPY --from=builder /app/target/*.jar app.jar

# Playwright em Docker precisa de --no-sandbox (já configurado no PlaywrightConfig)
# e da variável PLAYWRIGHT_HEADLESS=true

EXPOSE 8081
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
```

> ATENÇÃO: Use a imagem base `mcr.microsoft.com/playwright/java:v1.44.0-jammy`
> pois ela já contém Chromium, todas as dependências de sistema e fontes necessárias.
> Não use `eclipse-temurin:17-jre-alpine` pois faltarão libs do Chromium.

---

## TRATAMENTO DE ERROS E LOGGING

### Exceções personalizadas

```java
// Lançada quando login/senha rejeitados pelo Conciflex
public class LoginConciflex Exception extends RuntimeException {
    public LoginConciflex Exception(String estabelecimento, String motivo) {
        super("Login falhou para estabelecimento '" + estabelecimento + "': " + motivo);
    }
}

// Lançada quando coleta excede COLETA_TIMEOUT_SEGUNDOS
public class ColetaException extends RuntimeException { ... }
```

### Mapeamento de exceção para status do LogColeta

| Exceção capturada          | Status gravado em logs_coleta |
|---------------------------|-------------------------------|
| `LoginConciflex Exception` | `login_failed`                |
| `TimeoutException` (Playwright) | `timeout`              |
| Qualquer outra `Exception` | `error`                       |
| Nenhuma                    | `success`                     |

### Campos que NÃO devem aparecer em logs
- `cartao` (número mascarado do cartão)
- `conciflex_login`, `conciflex_senha` (mesmo criptografados)
- Cookies de sessão (`laravel_session`, `XSRF-TOKEN`, `cf_clearance`)

---

## DOCKER COMPOSE (trecho de integração)

```yaml
# Adicionar ao docker-compose.yml do projeto principal:
services:
  coletor:
    build: ./coletor
    container_name: conciliacao-coletor
    depends_on:
      - postgres
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/conciliacao
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      CRYPTO_SECRET_KEY: ${CRYPTO_SECRET_KEY}
      COLETA_DIAS_RETROATIVOS: ${COLETA_DIAS_RETROATIVOS:-1}
      COLETA_CRON: ${COLETA_CRON:-0 0 17 * * *}
      PLAYWRIGHT_HEADLESS: "true"
    ports:
      - "8081:8081"
    restart: unless-stopped
    shm_size: '2gb'   # OBRIGATÓRIO para Chromium headless em Docker evitar crash
```

> `shm_size: '2gb'` é crítico. Sem ele, o Chromium crasha com "Tab crashed" ao
> rodar em container Docker porque o `/dev/shm` padrão (64MB) é insuficiente.

---

## O QUE GERAR

1. `pom.xml` completo com todas as dependências
2. `application.yml` com todos os placeholders de env vars
3. `Dockerfile` multi-stage (imagem base Playwright)
4. `ColetorApplication.java` com `@EnableScheduling` e `@EnableAsync`
5. `PlaywrightConfig.java` — beans Playwright e Browser
6. `CryptoService.java` — AES-256-GCM decrypt
7. Todas as entidades JPA (idênticas ao backend, mesmas tabelas)
8. Todos os repositórios com queries de UPSERT nativo
9. Todos os DTOs de desserialização com `@JsonProperty` UPPER_CASE
10. `PlaywrightSessionService.java` — fluxo completo de navegação (passo a passo documentado)
11. `ColetorService.java` — orquestração por estabelecimento com tratamento de erro
12. `ColetaAgendadaService.java` — cron job + retry com backoff + cálculo de período
13. `ColetaController.java` — endpoints manuais com `@Async`
14. Exceções personalizadas
15. Trecho de docker-compose.yml para integração

Gere cada arquivo completo, sem omitir imports ou anotações.
Adicione comentários em português em toda a lógica de negócio relevante.
```
