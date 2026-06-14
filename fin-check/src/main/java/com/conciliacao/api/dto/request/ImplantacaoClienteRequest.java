package com.conciliacao.api.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.UUID;

public record ImplantacaoClienteRequest(

    @NotNull(message = "clienteId é obrigatório")
    UUID clienteId,

    @NotBlank(message = "Etapa é obrigatória")
    @Pattern(regexp = "pre|corrida|onboarding|curral",
             message = "Etapa inválida. Valores aceitos: pre, corrida, onboarding, curral")
    String etapa,

    // Obrigatório quando etapa != curral — validado no service
    // Null é aceito aqui para que @Pattern não quebre quando etapa = curral
    @Pattern(regexp = "fluindo|aguardando|travado",
             message = "Status inválido. Valores aceitos: fluindo, aguardando, travado")
    String status,

    String responsavel,

    String donoContato,

    // Array JSON de adquirentes, ex: ["Stone","Cielo"]
    JsonNode adquirentes,

    String observacoes,

    // JSONB de progresso dos checklists por etapa
    JsonNode progressJson,

    LocalDateTime etapaIniciadaEm,

    LocalDateTime ultimoMovimento

) {}
