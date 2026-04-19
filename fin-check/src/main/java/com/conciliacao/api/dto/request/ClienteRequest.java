package com.conciliacao.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
    @NotBlank String razaoSocial,
    String nomeFantasia,
    @NotBlank @Size(min = 14, max = 14) String cnpj,
    @NotBlank String whatsapp,
    @NotBlank String conciflexLogin,
    @NotBlank String conciflexSenha,
    String observacoes
) {}
