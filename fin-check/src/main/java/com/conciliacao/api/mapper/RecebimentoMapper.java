package com.conciliacao.api.mapper;

import com.conciliacao.api.dto.response.RecebimentoResponse;
import com.conciliacao.api.entity.Recebimento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RecebimentoMapper {

    @Mapping(target = "parcela", source = "parcela")
    @Mapping(target = "totalParcelas", source = "totalParcelas")
    RecebimentoResponse toResponse(Recebimento entity);

    List<RecebimentoResponse> toResponseList(List<Recebimento> entities);

    // 🔧 MapStruct will use these automatically for type conversion
    default Integer map(Short value) {
        return value == null ? null : value.intValue();
    }
}
