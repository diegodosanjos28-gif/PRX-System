package com.conciliacao.api.mapper;

import com.conciliacao.api.dto.request.EstabelecimentoRequest;
import com.conciliacao.api.dto.response.EstabelecimentoResponse;
import com.conciliacao.api.entity.Estabelecimento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EstabelecimentoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "ativo", constant = "true")
    Estabelecimento toEntity(EstabelecimentoRequest request);

    @Mapping(target = "clienteId", source = "cliente.id")
    @Mapping(target = "razaoSocialCliente", source = "cliente.razaoSocial")
    EstabelecimentoResponse toResponse(Estabelecimento estabelecimento);
}
