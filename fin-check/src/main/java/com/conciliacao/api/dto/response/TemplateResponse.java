package com.conciliacao.api.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record TemplateResponse(
    Long id,
    String nome,
    String conteudo,
    boolean ativo,
    List<TemplateVariavelResponse> variaveis,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
