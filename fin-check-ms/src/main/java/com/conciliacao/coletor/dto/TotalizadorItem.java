package com.conciliacao.coletor.dto;

import java.math.BigDecimal;

/**
 * Totalizador de uma categoria de lançamento na resposta da API Conciflex.
 * Ex: antecipacao, ajuste_debito, ajuste_credito, cancelamento, etc.
 */
public record TotalizadorItem(Integer total, BigDecimal sum) {}
