package com.conciliacao.api.mapper;

import com.conciliacao.api.dto.request.EstabelecimentoRequest;
import com.conciliacao.api.entity.Estabelecimento;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-18T18:40:04-0300",
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
}
