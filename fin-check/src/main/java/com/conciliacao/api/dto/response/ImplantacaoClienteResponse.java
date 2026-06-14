package com.conciliacao.api.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ImplantacaoClienteResponse(
    UUID id,
    UUID clienteId,
    String clienteRazaoSocial,
    String clienteNomeFantasia,
    String etapa,
    String status,
    String responsavel,
    String donoContato,
    JsonNode adquirentes,
    LocalDate dataEntradaCurral,
    LocalDateTime etapaIniciadaEm,
    String observacoes,
    JsonNode progressJson,
    LocalDateTime ultimoMovimento,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    // null na listagem, populado no detalhe (GET /{id})
    List<ImplantacaoDemandaResponse> demandas
) {}
