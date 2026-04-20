package com.conciliacao.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ConciliacaoTaxaResponse(
    UUID id,
    String idConciflex,
    String codigoEmpresa,
    LocalDate dataVenda,
    String adquirente,
    String bandeira,
    String modalidade,
    String produto,
    BigDecimal valorBruto,
    BigDecimal percentualTaxa,
    BigDecimal taxaContratada,
    Integer quantidade,
    BigDecimal taxaPraticadaRs,
    BigDecimal taxaContratadaRs,
    BigDecimal totalTaxaNaoContratadaRs,
    BigDecimal perdaRs,
    String auditada,
    LocalDateTime coletadoEm
) {}
