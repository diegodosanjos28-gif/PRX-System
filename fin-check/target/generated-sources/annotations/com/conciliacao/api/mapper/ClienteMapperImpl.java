package com.conciliacao.api.mapper;

import com.conciliacao.api.dto.request.ClienteRequest;
import com.conciliacao.api.dto.response.ClienteResponse;
import com.conciliacao.api.entity.Cliente;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-18T18:40:04-0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Ubuntu)"
)
@Component
public class ClienteMapperImpl implements ClienteMapper {

    @Override
    public Cliente toEntity(ClienteRequest request) {
        if ( request == null ) {
            return null;
        }

        Cliente.ClienteBuilder cliente = Cliente.builder();

        cliente.razaoSocial( request.razaoSocial() );
        cliente.nomeFantasia( request.nomeFantasia() );
        cliente.cnpj( request.cnpj() );
        cliente.whatsapp( request.whatsapp() );
        cliente.observacoes( request.observacoes() );

        cliente.ativo( true );

        return cliente.build();
    }

    @Override
    public ClienteResponse toResponse(Cliente cliente) {
        if ( cliente == null ) {
            return null;
        }

        UUID id = null;
        String razaoSocial = null;
        String nomeFantasia = null;
        String cnpj = null;
        String whatsapp = null;
        boolean ativo = false;
        String observacoes = null;
        LocalDateTime criadoEm = null;

        id = cliente.getId();
        razaoSocial = cliente.getRazaoSocial();
        nomeFantasia = cliente.getNomeFantasia();
        cnpj = cliente.getCnpj();
        whatsapp = cliente.getWhatsapp();
        ativo = cliente.isAtivo();
        observacoes = cliente.getObservacoes();
        criadoEm = cliente.getCriadoEm();

        ClienteResponse clienteResponse = new ClienteResponse( id, razaoSocial, nomeFantasia, cnpj, whatsapp, ativo, observacoes, criadoEm );

        return clienteResponse;
    }
}
