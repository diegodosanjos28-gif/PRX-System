package com.conciliacao.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EstabelecimentoRequest(
    @NotBlank String descricao,
    @NotBlank String identificadorConciflex
) {}
