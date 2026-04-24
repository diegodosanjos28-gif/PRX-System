package com.conciliacao.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record MensagemResponse(
    UUID id,
    UUID clienteId,
    String clienteNome,
    String conteudo,
    String modoGeracao,
    String metaMessageId,
    String statusEntrega,
    LocalDateTime enviadoEm,
    UUID estabelecimentoId,
    String templateNome
) {}
