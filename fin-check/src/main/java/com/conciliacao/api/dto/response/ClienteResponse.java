package com.conciliacao.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClienteResponse(
    UUID id,
    String razaoSocial,
    String nomeFantasia,
    String cnpj,
    String whatsapp,
    boolean ativo,
    String observacoes,
    LocalDateTime criadoEm
) {}
