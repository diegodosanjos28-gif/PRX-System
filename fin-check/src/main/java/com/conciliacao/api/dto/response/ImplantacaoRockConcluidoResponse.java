package com.conciliacao.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ImplantacaoRockConcluidoResponse(
    UUID implantacaoId,
    UUID clienteId,
    String clienteRazaoSocial,
    String clienteNomeFantasia,
    UUID demandaId,
    String descricao,
    String adquirente,
    String prioridade,
    LocalDateTime concluidaEm
) {}
