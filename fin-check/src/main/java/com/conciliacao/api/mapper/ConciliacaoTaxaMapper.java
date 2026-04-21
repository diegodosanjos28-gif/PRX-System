package com.conciliacao.api.mapper;

import com.conciliacao.api.dto.response.ConciliacaoTaxaResponse;
import com.conciliacao.api.entity.ConciliacaoTaxa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ConciliacaoTaxaMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "idConciflex", source = "idConciflex")
    @Mapping(target = "codigoEmpresa", source = "codigoEmpresa")
    @Mapping(target = "dataVenda", source = "dataVenda")
    @Mapping(target = "adquirente", source = "adquirente")
    @Mapping(target = "bandeira", source = "bandeira")
    @Mapping(target = "modalidade", source = "modalidade")
    @Mapping(target = "produto", source = "produto")
    @Mapping(target = "valorBruto", source = "valorBruto")
    @Mapping(target = "percentualTaxa", source = "percentualTaxa")
    @Mapping(target = "taxaContratada", source = "taxaContratada")
    @Mapping(target = "quantidade", source = "quantidade")
    @Mapping(target = "taxaPraticadaRs", source = "taxaPraticadaRs")
    @Mapping(target = "taxaContratadaRs", source = "taxaContratadaRs")
    @Mapping(target = "totalTaxaNaoContratadaRs", source = "totalTaxaNaoContratadaRs")
    @Mapping(target = "perdaRs", source = "perdaRs")
    @Mapping(target = "auditada", source = "auditada")
    @Mapping(target = "coletadoEm", source = "coletadoEm")
    ConciliacaoTaxaResponse toResponse(ConciliacaoTaxa entity);

    // 🔥 MapStruct generates this automatically
    List<ConciliacaoTaxaResponse> toResponseList(List<ConciliacaoTaxa> entities);
}
