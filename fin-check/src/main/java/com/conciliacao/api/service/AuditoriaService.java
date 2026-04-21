package com.conciliacao.api.service;

import com.conciliacao.api.dto.response.AuditoriaPorBandeira;
import com.conciliacao.api.dto.response.AuditoriaResumoResponse;
import com.conciliacao.api.dto.response.ConciliacaoTaxaResponse;
import com.conciliacao.api.entity.ConciliacaoTaxa;
import com.conciliacao.api.mapper.ConciliacaoTaxaMapper;
import com.conciliacao.api.repository.ConciliacaoTaxaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final ConciliacaoTaxaRepository conciliacaoTaxaRepository;
    private final EstabelecimentoService estabelecimentoService;
    private final ConciliacaoTaxaMapper conciliacaoTaxaMapper;

    @Transactional(readOnly = true)
    public AuditoriaResumoResponse resumo(UUID estabelecimentoId, LocalDate inicio, LocalDate fim) {
        // Valida que o estabelecimento existe
        estabelecimentoService.buscarEntidade(estabelecimentoId);

        Long total = conciliacaoTaxaRepository
            .countByEstabelecimentoIdAndDataVendaBetween(estabelecimentoId, inicio, fim);

        // Soma das taxas não contratadas positivas = cobrou a mais que o contratado
        BigDecimal cobradoAMais = conciliacaoTaxaRepository
            .sumTaxaNaoContratadaPositiva(estabelecimentoId, inicio, fim);

        // Soma das taxas não contratadas negativas = cobrou a menos (crédito a favor do cliente)
        BigDecimal cobradoAMenos = conciliacaoTaxaRepository
            .sumTaxaNaoContratadaNegativa(estabelecimentoId, inicio, fim);

        // Agrupamento por bandeira com soma das diferenças
        List<Object[]> rows = conciliacaoTaxaRepository
            .findAgrupandoPorBandeira(estabelecimentoId, inicio, fim);

        List<AuditoriaPorBandeira> porBandeira = rows.stream()
            .map(row -> new AuditoriaPorBandeira(
                (String) row[0],
                (Long) row[1],
                row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO
            ))
            .toList();

        List<ConciliacaoTaxa> conciliacoesTaxa = conciliacaoTaxaRepository.findByEstabelecimentoIdAndDataVendaBetween(estabelecimentoId, inicio, fim);

        return new AuditoriaResumoResponse(
            total,
            cobradoAMais != null ? cobradoAMais : BigDecimal.ZERO,
            cobradoAMenos != null ? cobradoAMenos.abs() : BigDecimal.ZERO,
            porBandeira,
            conciliacaoTaxaMapper.toResponseList(conciliacoesTaxa)
        );
    }
}
