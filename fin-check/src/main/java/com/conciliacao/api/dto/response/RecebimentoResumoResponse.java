package com.conciliacao.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record RecebimentoResumoResponse(
    BigDecimal totalRecebido,
    BigDecimal totalDescontado,
    List<RecebimentoPorBandeira> porBandeira
) {}
