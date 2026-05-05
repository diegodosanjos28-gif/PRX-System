package com.conciliacao.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record TemplateRequest(
    @NotBlank String nome,
    String metaId,
    @NotBlank String conteudo,
    boolean ativo,
    @Valid List<TemplateVariavelRequest> variaveis   // nullable/empty = no variable changes
) {}
