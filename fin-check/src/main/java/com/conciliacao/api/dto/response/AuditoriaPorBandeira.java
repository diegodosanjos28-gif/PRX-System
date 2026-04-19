package com.conciliacao.api.dto.response;

import java.math.BigDecimal;

public record AuditoriaPorBandeira(
    String bandeira,
    Long quantidade,
    BigDecimal diferencaTotal
) {}
