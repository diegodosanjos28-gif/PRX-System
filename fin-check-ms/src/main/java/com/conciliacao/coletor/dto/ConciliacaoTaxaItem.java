package com.conciliacao.coletor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cada elemento do array "result" de POST /conciliacao-taxas/buscar.
 * Campos UPPER_CASE conforme API real.
 * Atenção: campos numéricos podem chegar como String OU Number — o ObjectMapper
 * está configurado com ALLOW_COERCION_OF_SCALARS para lidar com isso.
 * Campos de imagem (ADQUIRENTE_IMAGEM, BANDEIRA_IMAGEM) ignorados — não persistidos.
 */
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
) {}
