package com.conciliacao.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MensagemGerarTodosRequest(
    @NotNull LocalDate dataInicio,
    @NotNull LocalDate dataFim,
    @NotBlank String modo,   // "ia" ou "template"
    Long templateId,         // obrigatório quando modo="template"
    /** Optional Meta access token — overrides the server-configured token when provided. */
    String metaAccessToken
) {}
