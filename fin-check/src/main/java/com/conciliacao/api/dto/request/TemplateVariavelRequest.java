package com.conciliacao.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record TemplateVariavelRequest(
    @NotBlank String chave,
    @NotBlank String descricao,
    Long templateId,        // null = global/reusável
    @Min(1) int ordem       // 1-based positional index for Meta template parameters
) {}
