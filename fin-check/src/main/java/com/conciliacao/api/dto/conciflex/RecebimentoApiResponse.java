package com.conciliacao.api.dto.conciflex;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record RecebimentoApiResponse(
    List<RecebimentoItem> result,
    BigDecimal sum,
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
