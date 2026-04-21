package com.conciliacao.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record AuditoriaResumoResponse(
    Long totalTransacoes,
    BigDecimal totalCobradoAMais,
    BigDecimal totalCobradoAMenos,
    List<AuditoriaPorBandeira> porBandeira,
    List<ConciliacaoTaxaResponse> conciliacaoTaxaResponse
) {}
