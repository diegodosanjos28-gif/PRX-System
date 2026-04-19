package com.conciliacao.coletor.dto;

import java.util.UUID;

/**
 * Resultado de uma coleta bem-sucedida para um estabelecimento.
 */
public record ColetaResultado(
    UUID estabelecimentoId,
    int registrosConciliacaoTaxas,
    int registrosRecebimentos
) {
    public int totalRegistros() {
        return registrosConciliacaoTaxas + registrosRecebimentos;
    }
}
