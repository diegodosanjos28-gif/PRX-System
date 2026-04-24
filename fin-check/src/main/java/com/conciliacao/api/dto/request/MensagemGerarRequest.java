package com.conciliacao.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record MensagemGerarRequest(
    @NotNull UUID clienteId,
    @NotNull UUID estabelecimentoId,
    @NotNull LocalDate dataInicio,
    @NotNull LocalDate dataFim,
    @NotBlank String modo,   // "ia" ou "template"
    Long templateId          // obrigatório quando modo="template"
) {}
