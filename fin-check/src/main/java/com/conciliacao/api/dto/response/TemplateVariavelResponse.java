package com.conciliacao.api.dto.response;

public record TemplateVariavelResponse(
    Long id,
    String chave,
    String descricao,
    boolean sistemaFixo,
    int ordem,
    Long templateId
) {}
