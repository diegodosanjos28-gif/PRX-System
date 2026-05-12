package com.conciliacao.api.service;

import com.conciliacao.api.dto.response.ExperienciaClienteResponse;
import com.conciliacao.api.dto.response.ExperienciaClienteResponse.GrupoDescAjuste;
import com.conciliacao.api.dto.response.ExperienciaClienteResponse.InsightsComparativos;
import com.conciliacao.api.dto.response.ExperienciaClienteResponse.InsightsComparativos.VariacaoMetrica;
import com.conciliacao.api.dto.response.ExperienciaClienteResponse.InsightsComparativos.VariacaoPorGrupo;
import com.conciliacao.api.entity.Estabelecimento;
import com.conciliacao.api.repository.ConciliacaoTaxaRepository;
import com.conciliacao.api.repository.RecebimentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExperienciaClienteService {

    private final RecebimentoRepository recebimentoRepository;
    private final ConciliacaoTaxaRepository conciliacaoTaxaRepository;
    private final EstabelecimentoService estabelecimentoService;

    @Transactional(readOnly = true)
    public ExperienciaClienteResponse calcular(
            UUID estabelecimentoId,
            LocalDate dataInicio, LocalDate dataFim,
            LocalDate dataInicioComp, LocalDate dataFimComp) {

        Estabelecimento est = estabelecimentoService.buscarEntidade(estabelecimentoId);

        // ── Recebimentos ─────────────────────────────────────────────────────────
        List<Object[]> descAjusteP = recebimentoRepository
                .findAgrupadosPorDescAjuste(estabelecimentoId, dataInicio, dataFim);
        List<Object[]> descAjusteC = recebimentoRepository
                .findAgrupadosPorDescAjuste(estabelecimentoId, dataInicioComp, dataFimComp);
        List<Object[]> bandeiraRecP = recebimentoRepository
                .findAgrupadosPorBandeiraVenda(estabelecimentoId, dataInicio, dataFim);
        List<Object[]> bandeiraRecC = recebimentoRepository
                .findAgrupadosPorBandeiraVenda(estabelecimentoId, dataInicioComp, dataFimComp);
        Object[] totRecP = recebimentoRepository
                .findTotalizadoresPorPeriodo(estabelecimentoId, dataInicio, dataFim);
        Object[] totRecC = recebimentoRepository
                .findTotalizadoresPorPeriodo(estabelecimentoId, dataInicioComp, dataFimComp);

        // ── Conciliação de Taxas ──────────────────────────────────────────────────
        Object[] totConcP = conciliacaoTaxaRepository
                .findTotalizadoresPorPeriodo(estabelecimentoId, dataInicio, dataFim);
        Object[] totConcC = conciliacaoTaxaRepository
                .findTotalizadoresPorPeriodo(estabelecimentoId, dataInicioComp, dataFimComp);
        List<Object[]> bandeiraConcP = conciliacaoTaxaRepository
                .findAgrupadosPorBandeiraComparacao(estabelecimentoId, dataInicio, dataFim);
        List<Object[]> bandeiraConcC = conciliacaoTaxaRepository
                .findAgrupadosPorBandeiraComparacao(estabelecimentoId, dataInicioComp, dataFimComp);
        List<Object[]> adqP = conciliacaoTaxaRepository
                .findAgrupadosPorAdquirente(estabelecimentoId, dataInicio, dataFim);
        List<Object[]> adqC = conciliacaoTaxaRepository
                .findAgrupadosPorAdquirente(estabelecimentoId, dataInicioComp, dataFimComp);

        // ── Build GrupoDescAjuste list (período principal) ────────────────────────
        List<GrupoDescAjuste> grupos = descAjusteP.stream()
                .map(row -> new GrupoDescAjuste(
                        (String) row[0],
                        toLong(row[1]),
                        toBD(row[2]),
                        toBD(row[3]),
                        toBD(row[4]),
                        toBD(row[5])
                ))
                .toList();

        BigDecimal totalGeralBruto = grupos.stream()
                .map(GrupoDescAjuste::valorBrutoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGeralTaxas = grupos.stream()
                .map(GrupoDescAjuste::totalTaxas).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGeralLiquido = grupos.stream()
                .map(GrupoDescAjuste::valorLiquidoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGeralTarifa = grupos.stream()
                .map(GrupoDescAjuste::tarifaTransacaoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        Long totalGeralTransacoes = grupos.stream()
                .mapToLong(GrupoDescAjuste::quantidadeTransacoes).sum();

        // ── Build InsightsComparativos ─────────────────────────────────────────────
        InsightsComparativos insights = new InsightsComparativos(
                // Recebimentos — visão geral
                calcularVariacao("Valor Bruto", toBD(totRecP[1]), toBD(totRecC[1])),
                calcularVariacao("Total em Taxas", toBD(totRecP[2]), toBD(totRecC[2])),
                calcularVariacao("Valor Líquido", toBD(totRecP[3]), toBD(totRecC[3])),
                calcularVariacao("Tarifa por Transação", toBD(totRecP[4]), toBD(totRecC[4])),
                calcularVariacao("Quantidade de Transações",
                        BigDecimal.valueOf(toLong(totRecP[0])),
                        BigDecimal.valueOf(toLong(totRecC[0]))),
                // Recebimentos — por bandeira e por desc_ajuste
                buildVariacoes6(bandeiraRecP, bandeiraRecC),
                buildVariacoes6(descAjusteP, descAjusteC),
                // Conciliação — visão geral
                calcularVariacao("Valor Bruto", toBD(totConcP[1]), toBD(totConcC[1])),
                calcularVariacao("Taxa Não Contratada", toBD(totConcP[2]), toBD(totConcC[2])),
                calcularVariacao("% Taxa Médio", toBD(totConcP[3]), toBD(totConcC[3])),
                calcularVariacao("Quantidade de Transações",
                        BigDecimal.valueOf(toLong(totConcP[0])),
                        BigDecimal.valueOf(toLong(totConcC[0]))),
                // Conciliação — por bandeira e adquirente
                buildVariacoes5(bandeiraConcP, bandeiraConcC),
                buildVariacoes5(adqP, adqC)
        );

        return new ExperienciaClienteResponse(
                estabelecimentoId.toString(),
                est.getDescricao(),
                dataInicio.toString(),
                dataFim.toString(),
                dataInicioComp.toString(),
                dataFimComp.toString(),
                grupos,
                totalGeralBruto,
                totalGeralTaxas,
                totalGeralLiquido,
                totalGeralTarifa,
                totalGeralTransacoes,
                insights
        );
    }

    // Recebimento queries: [0]=grupo, [1]=count, [2]=valorBruto, [3]=valorTaxa, [4]=valorLiquido, [5]=tarifaTransacao
    private List<VariacaoPorGrupo> buildVariacoes6(List<Object[]> principal, List<Object[]> comparacao) {
        Map<String, Object[]> compMap = comparacao.stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> r, (a, b) -> a));

        List<VariacaoPorGrupo> result = new ArrayList<>();
        for (Object[] row : principal) {
            String grupo = (String) row[0];
            Object[] comp = compMap.getOrDefault(grupo,
                    new Object[]{grupo, 0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            result.add(buildVPG(grupo, "valorBruto",       toBD(row[2]), toBD(comp[2])));
            result.add(buildVPG(grupo, "totalTaxas",       toBD(row[3]), toBD(comp[3])));
            result.add(buildVPG(grupo, "tarifaTransacao",  toBD(row[5]), toBD(comp[5])));
            result.add(buildVPG(grupo, "valorLiquido",     toBD(row[4]), toBD(comp[4])));
        }
        return result;
    }

    // ConciliacaoTaxa queries: [0]=grupo, [1]=count, [2]=valorBruto, [3]=taxaNaoContratada, [4]=pctTaxa
    private List<VariacaoPorGrupo> buildVariacoes5(List<Object[]> principal, List<Object[]> comparacao) {
        Map<String, Object[]> compMap = comparacao.stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> r, (a, b) -> a));

        List<VariacaoPorGrupo> result = new ArrayList<>();
        for (Object[] row : principal) {
            String grupo = (String) row[0];
            Object[] comp = compMap.getOrDefault(grupo,
                    new Object[]{grupo, 0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            result.add(buildVPG(grupo, "valorBruto",             toBD(row[2]), toBD(comp[2])));
            result.add(buildVPG(grupo, "totalTaxaNaoContratada", toBD(row[3]), toBD(comp[3])));
        }
        return result;
    }

    private VariacaoMetrica calcularVariacao(String label, BigDecimal principal, BigDecimal comparacao) {
        principal = coalesce(principal);
        comparacao = coalesce(comparacao);
        BigDecimal pct = calcularPercentual(principal, comparacao);
        return new VariacaoMetrica(label, principal, comparacao, pct, direcao(pct));
    }

    private VariacaoPorGrupo buildVPG(String grupo, String metrica, BigDecimal principal, BigDecimal comparacao) {
        principal = coalesce(principal);
        comparacao = coalesce(comparacao);
        BigDecimal pct = calcularPercentual(principal, comparacao);
        return new VariacaoPorGrupo(grupo, metrica, principal, comparacao, pct, direcao(pct));
    }

    private BigDecimal calcularPercentual(BigDecimal principal, BigDecimal comparacao) {
        if (comparacao.compareTo(BigDecimal.ZERO) == 0) {
            return principal.compareTo(BigDecimal.ZERO) > 0
                    ? new BigDecimal("100.00")
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return principal.subtract(comparacao)
                .divide(comparacao.abs(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String direcao(BigDecimal pct) {
        int cmp = pct.compareTo(BigDecimal.ZERO);
        return cmp > 0 ? "ALTA" : cmp < 0 ? "QUEDA" : "ESTAVEL";
    }

    private BigDecimal coalesce(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal toBD(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal bd) return bd;
        if (obj instanceof Double d) return BigDecimal.valueOf(d);
        if (obj instanceof Long l) return BigDecimal.valueOf(l);
        if (obj instanceof Integer i) return BigDecimal.valueOf(i);
        return new BigDecimal(obj.toString());
    }

    private Long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Long l) return l;
        if (obj instanceof Integer i) return i.longValue();
        if (obj instanceof BigDecimal bd) return bd.longValue();
        return Long.parseLong(obj.toString());
    }
}
