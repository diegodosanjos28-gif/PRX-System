package com.conciliacao.api.service;

import com.conciliacao.api.dto.response.RecebimentoPorBandeira;
import com.conciliacao.api.dto.response.RecebimentoResumoResponse;
import com.conciliacao.api.entity.Recebimento;
import com.conciliacao.api.mapper.RecebimentoMapper;
import com.conciliacao.api.repository.RecebimentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecebimentoService {

    private final RecebimentoRepository recebimentoRepository;
    private final EstabelecimentoService estabelecimentoService;
    private final RecebimentoMapper recebimentoMapper;

    @Transactional(readOnly = true)
    public RecebimentoResumoResponse resumo(UUID estabelecimentoId, LocalDate inicio, LocalDate fim) {
        estabelecimentoService.buscarEntidade(estabelecimentoId);

        BigDecimal totalRecebido = recebimentoRepository.sumValorLiquido(estabelecimentoId, inicio, fim);
        BigDecimal totalDescontado = recebimentoRepository.sumValorTaxa(estabelecimentoId, inicio, fim);

        List<Object[]> rows = recebimentoRepository.findAgrupandoPorBandeira(estabelecimentoId, inicio, fim);

        List<RecebimentoPorBandeira> porBandeira = rows.stream()
            .map(row -> new RecebimentoPorBandeira(
                (String) row[0],
                (Long) row[1],
                row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO,
                row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO,
                row[4] != null ? (BigDecimal) row[4] : BigDecimal.ZERO
            ))
            .toList();

        List<Recebimento> recebimentos = recebimentoRepository.findByEstabelecimentoIdAndDataPagamentoBetween(estabelecimentoId, inicio, fim);

        return new RecebimentoResumoResponse(
            totalRecebido != null ? totalRecebido : BigDecimal.ZERO,
            totalDescontado != null ? totalDescontado : BigDecimal.ZERO,
            porBandeira,
            recebimentoMapper.toResponseList(recebimentos)
        );
    }
}
