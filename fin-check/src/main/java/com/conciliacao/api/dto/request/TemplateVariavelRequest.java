package com.conciliacao.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TemplateVariavelRequest(
    @NotBlank String chave,
    @NotBlank String descricao,
    Long templateId  // null = global/reusável
) {}
