package com.conciliacao.api.mapper;

import com.conciliacao.api.dto.request.ClienteRequest;
import com.conciliacao.api.dto.response.ClienteResponse;
import com.conciliacao.api.entity.Cliente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    // conciflexLogin e conciflexSenha são criptografados no service antes de salvar
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ativo", constant = "true")
    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "estabelecimentos", ignore = true)
    @Mapping(target = "conciflex_login", ignore = true)
    @Mapping(target = "conciflex_senha", ignore = true)
    Cliente toEntity(ClienteRequest request);

    ClienteResponse toResponse(Cliente cliente);
}
