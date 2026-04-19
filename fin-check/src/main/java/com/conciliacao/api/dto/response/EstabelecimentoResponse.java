package com.conciliacao.api.dto.response;

import java.util.UUID;

public record EstabelecimentoResponse(
    UUID id,
    UUID clienteId,
    String razaoSocialCliente,
    String descricao,
    String identificadorConciflex,
    boolean ativo
) {}
