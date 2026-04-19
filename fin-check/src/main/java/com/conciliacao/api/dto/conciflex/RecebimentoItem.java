package com.conciliacao.api.dto.conciflex;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

// valorBruto e valorLiquido são NEGATIVOS em registros de Ajuste a Débito
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
    @JsonProperty("CARTAO")                      String cartao,
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
) {}
