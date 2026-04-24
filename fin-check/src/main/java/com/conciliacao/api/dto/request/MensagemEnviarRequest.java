package com.conciliacao.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MensagemEnviarRequest(
    @NotNull UUID clienteId,
    @NotBlank String conteudo,
    UUID estabelecimentoId,  // nullable
    Long templateId,         // nullable
    String templateNome,     // nullable, denormalizado para histórico
    String modoGeracao       // "ia" ou "template", default "template"
) {}
