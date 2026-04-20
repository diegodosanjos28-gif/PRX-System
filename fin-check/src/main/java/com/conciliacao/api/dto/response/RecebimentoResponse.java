package com.conciliacao.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record RecebimentoResponse(
    UUID id,
    String idConciflex,
    String tipoLancamento,
    LocalDate dataVenda,
    LocalDate dataPrevisao,
    LocalDate dataPagamento,
    String adquirente,
    String bandeira,
    String modalidade,
    String nsu,
    String cartao,
    Integer parcela,
    Integer totalParcelas,
    BigDecimal valorBruto,
    BigDecimal taxaPercentual,
    BigDecimal valorTaxa,
    BigDecimal valorLiquido,
    String statusConciliacao,
    String banco,
    String agencia,
    String meioCaptura,
    LocalDateTime coletadoEm
) {}
