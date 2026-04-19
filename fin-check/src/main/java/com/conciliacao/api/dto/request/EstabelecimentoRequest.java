package com.conciliacao.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EstabelecimentoRequest(

    @NotBlank(message = "Descrição é obrigatória")
    @Size(max = 255, message = "Descrição não pode exceder 255 caracteres")
    String descricao,

    @NotBlank(message = "Identificador Conciflex é obrigatório")
    @Size(max = 100, message = "Identificador Conciflex não pode exceder 100 caracteres")
    String identificadorConciflex

) {}
