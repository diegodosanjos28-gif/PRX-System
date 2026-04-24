package com.conciliacao.api.dto.response;

import java.util.List;

public record MensagemBulkResultado(
    int total,
    int enviados,
    int erros,
    List<String> errosDetalhados
) {}
