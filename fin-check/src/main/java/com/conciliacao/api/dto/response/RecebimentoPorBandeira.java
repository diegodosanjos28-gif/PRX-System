package com.conciliacao.api.dto.response;

import java.math.BigDecimal;

public record RecebimentoPorBandeira(
    String bandeira,
    Long quantidade,
    BigDecimal totalBruto,
    BigDecimal totalLiquido,
    BigDecimal totalTaxa
) {}
