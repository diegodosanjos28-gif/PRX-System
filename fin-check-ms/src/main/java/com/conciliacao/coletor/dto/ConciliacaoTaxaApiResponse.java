package com.conciliacao.coletor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resposta completa de POST /conciliacao-taxas/buscar da API Conciflex.
 * Campos raiz confirmados com dados reais de produção.
 */
public record ConciliacaoTaxaApiResponse(
    List<ConciliacaoTaxaItem> result,

    @JsonProperty("total_valor_bruto_auditadas")
    BigDecimal totalValorBrutoAuditadas,

    @JsonProperty("registros_auditados")
    Integer registrosAuditados,

    @JsonProperty("total_valor_bruto_nao_auditadas")
    BigDecimal totalValorBrutoNaoAuditadas,

    @JsonProperty("registros_nao_auditados")
    Integer registrosNaoAuditados,

    @JsonProperty("total_taxa_cadastrada_praticada")
    BigDecimal totalTaxaCadastradaPraticada,

    @JsonProperty("total_taxas")
    BigDecimal totalTaxas,

    @JsonProperty("taxas_auditadas")
    BigDecimal taxasAuditadas,

    @JsonProperty("taxas_nao_auditadas")
    BigDecimal taxasNaoAuditadas,

    @JsonProperty("total_liquido")
    BigDecimal totalLiquido
) {}
