package com.conciliacao.api.mapper;

import com.conciliacao.api.dto.request.EstabelecimentoRequest;
import com.conciliacao.api.dto.response.EstabelecimentoResponse;
import com.conciliacao.api.entity.Cliente;
import com.conciliacao.api.entity.Estabelecimento;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-19T14:22:39-0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Ubuntu)"
)
@Component
public class EstabelecimentoMapperImpl implements EstabelecimentoMapper {

    @Override
    public Estabelecimento toEntity(EstabelecimentoRequest request) {
        if ( request == null ) {
            return null;
        }

        Estabelecimento.EstabelecimentoBuilder estabelecimento = Estabelecimento.builder();

        estabelecimento.descricao( request.descricao() );
        estabelecimento.identificadorConciflex( request.identificadorConciflex() );

        estabelecimento.ativo( true );

        return estabelecimento.build();
    }

    @Override
    public EstabelecimentoResponse toResponse(Estabelecimento estabelecimento) {
        if ( estabelecimento == null ) {
            return null;
        }

        UUID clienteId = null;
        String razaoSocialCliente = null;
        UUID id = null;
        String descricao = null;
        String identificadorConciflex = null;
        boolean ativo = false;

        clienteId = estabelecimentoClienteId( estabelecimento );
        razaoSocialCliente = estabelecimentoClienteRazaoSocial( estabelecimento );
        id = estabelecimento.getId();
        descricao = estabelecimento.getDescricao();
        identificadorConciflex = estabelecimento.getIdentificadorConciflex();
        ativo = estabelecimento.isAtivo();

        EstabelecimentoResponse estabelecimentoResponse = new EstabelecimentoResponse( id, clienteId, razaoSocialCliente, descricao, identificadorConciflex, ativo );

        return estabelecimentoResponse;
    }

    private UUID estabelecimentoClienteId(Estabelecimento estabelecimento) {
        if ( estabelecimento == null ) {
            return null;
        }
        Cliente cliente = estabelecimento.getCliente();
        if ( cliente == null ) {
            return null;
        }
        UUID id = cliente.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String estabelecimentoClienteRazaoSocial(Estabelecimento estabelecimento) {
        if ( estabelecimento == null ) {
            return null;
        }
        Cliente cliente = estabelecimento.getCliente();
        if ( cliente == null ) {
            return null;
        }
        String razaoSocial = cliente.getRazaoSocial();
        if ( razaoSocial == null ) {
            return null;
        }
        return razaoSocial;
    }
}
