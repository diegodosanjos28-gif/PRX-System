package com.conciliacao.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TemplateRequest(
    @NotBlank String nome,
    @NotBlank String conteudo,
    boolean ativo
) {}
