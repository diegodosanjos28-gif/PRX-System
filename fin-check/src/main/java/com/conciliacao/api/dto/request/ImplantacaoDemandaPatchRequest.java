package com.conciliacao.api.dto.request;

import jakarta.validation.constraints.Pattern;

public record ImplantacaoDemandaPatchRequest(

    String descricao,

    Boolean concluida,

    @Pattern(regexp = "critica|alta|media|baixa",
             message = "Prioridade inválida. Valores aceitos: critica, alta, media, baixa")
    String prioridade,

    String adquirente,

    @Pattern(regexp = "pista|curral",
             message = "Tipo inválido. Valores aceitos: pista, curral")
    String tipo

) {}
