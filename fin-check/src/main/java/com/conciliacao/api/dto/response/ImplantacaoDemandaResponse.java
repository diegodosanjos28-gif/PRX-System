package com.conciliacao.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ImplantacaoDemandaResponse(
    UUID id,
    UUID implantacaoId,
    String descricao,
    boolean concluida,
    String adquirente,
    String prioridade,
    String tipo,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
