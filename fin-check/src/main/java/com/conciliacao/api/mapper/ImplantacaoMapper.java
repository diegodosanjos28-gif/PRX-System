package com.conciliacao.api.mapper;

import com.conciliacao.api.dto.request.ImplantacaoClienteRequest;
import com.conciliacao.api.dto.response.ImplantacaoClienteResponse;
import com.conciliacao.api.dto.response.ImplantacaoDemandaResponse;
import com.conciliacao.api.entity.ImplantacaoCliente;
import com.conciliacao.api.entity.ImplantacaoDemanda;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ImplantacaoMapper {

    ObjectMapper JSON = new ObjectMapper();

    // ── Listagem: sem demandas (evita N+1) ──────────────────────────────────
    @Mapping(target = "clienteId",          source = "cliente.id")
    @Mapping(target = "clienteRazaoSocial", source = "cliente.razaoSocial")
    @Mapping(target = "clienteNomeFantasia",source = "cliente.nomeFantasia")
    @Mapping(target = "adquirentes",  expression = "java(stringToJsonNode(entity.getAdquirentes()))")
    @Mapping(target = "progressJson", expression = "java(stringToJsonNode(entity.getProgressJson()))")
    @Mapping(target = "demandas",     ignore = true)
    ImplantacaoClienteResponse toListResponse(ImplantacaoCliente entity);

    // ── Detalhe: com demandas (JOIN FETCH garante que já estão carregadas) ──
    @Mapping(target = "clienteId",          source = "cliente.id")
    @Mapping(target = "clienteRazaoSocial", source = "cliente.razaoSocial")
    @Mapping(target = "clienteNomeFantasia",source = "cliente.nomeFantasia")
    @Mapping(target = "adquirentes",  expression = "java(stringToJsonNode(entity.getAdquirentes()))")
    @Mapping(target = "progressJson", expression = "java(stringToJsonNode(entity.getProgressJson()))")
    ImplantacaoClienteResponse toDetailResponse(ImplantacaoCliente entity);

    // ── Demanda ──────────────────────────────────────────────────────────────
    @Mapping(target = "implantacaoId", source = "implantacao.id")
    ImplantacaoDemandaResponse toResponse(ImplantacaoDemanda demanda);

    List<ImplantacaoDemandaResponse> toDemandaResponseList(List<ImplantacaoDemanda> demandas);

    // ── Request → Entity (cliente e timestamps definidos no service) ─────────
    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "cliente",          ignore = true)
    @Mapping(target = "adquirentes",  expression = "java(jsonNodeToString(request.adquirentes()))")
    @Mapping(target = "progressJson", expression = "java(jsonNodeToString(request.progressJson()))")
    @Mapping(target = "dataEntradaCurral",ignore = true)
    @Mapping(target = "demandas",         ignore = true)
    @Mapping(target = "createdAt",        ignore = true)
    @Mapping(target = "updatedAt",        ignore = true)
    ImplantacaoCliente toEntity(ImplantacaoClienteRequest request);

    // ── Conversões JSONB ─────────────────────────────────────────────────────

    default JsonNode stringToJsonNode(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return JSON.readTree(json);
        } catch (Exception e) {
            return JSON.createObjectNode();
        }
    }

    default String jsonNodeToString(JsonNode node) {
        if (node == null) return null;
        return node.toString();
    }
}
