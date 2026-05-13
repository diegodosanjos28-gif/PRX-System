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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExperienciaClienteService {

    // Totalizadores de recebimentos: [count, valorBruto, valorTaxa, valorLiquido, tarifaTransacao]
    private static final int REC_TOT_COLS = 5;
    // Totalizadores de conciliação: [count, valorBruto, taxaNaoContratada, pctTaxa]
    private static final int CONC_TOT_COLS = 4;
    // Agrupamentos de recebimento: [grupo, count, valorBruto, valorTaxa, valorLiquido, tarifaTransacao]
    private static final int REC_GRP_COLS = 6;
    // Agrupamentos de conciliação: [grupo, count, valorBruto, taxaNaoContratada, pctTaxa]
    private static final int CONC_GRP_COLS = 5;

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
        List<Object[]> descAjusteP = validarLinhas(
                recebimentoRepository.findAgrupadosPorDescAjuste(estabelecimentoId, dataInicio, dataFim),
                REC_GRP_COLS, "findAgrupadosPorDescAjuste[principal]");
        List<Object[]> descAjusteC = validarLinhas(
                recebimentoRepository.findAgrupadosPorDescAjuste(estabelecimentoId, dataInicioComp, dataFimComp),
                REC_GRP_COLS, "findAgrupadosPorDescAjuste[comparacao]");

        List<Object[]> bandeiraRecP = validarLinhas(
                recebimentoRepository.findAgrupadosPorBandeiraVenda(estabelecimentoId, dataInicio, dataFim),
                REC_GRP_COLS, "findAgrupadosPorBandeiraVenda[principal]");
        List<Object[]> bandeiraRecC = validarLinhas(
                recebimentoRepository.findAgrupadosPorBandeiraVenda(estabelecimentoId, dataInicioComp, dataFimComp),
                REC_GRP_COLS, "findAgrupadosPorBandeiraVenda[comparacao]");

        Object[] totRecP = primeiraLinha(
                recebimentoRepository.findTotalizadoresPorPeriodo(estabelecimentoId, dataInicio, dataFim),
                REC_TOT_COLS);
        Object[] totRecC = primeiraLinha(
                recebimentoRepository.findTotalizadoresPorPeriodo(estabelecimentoId, dataInicioComp, dataFimComp),
                REC_TOT_COLS);

        // ── Conciliação de Taxas ──────────────────────────────────────────────────
        Object[] totConcP = primeiraLinha(
                conciliacaoTaxaRepository.findTotalizadoresPorPeriodo(estabelecimentoId, dataInicio, dataFim),
                CONC_TOT_COLS);
        Object[] totConcC = primeiraLinha(
                conciliacaoTaxaRepository.findTotalizadoresPorPeriodo(estabelecimentoId, dataInicioComp, dataFimComp),
                CONC_TOT_COLS);

        List<Object[]> bandeiraConcP = validarLinhas(
                conciliacaoTaxaRepository.findAgrupadosPorBandeiraComparacao(estabelecimentoId, dataInicio, dataFim),
                CONC_GRP_COLS, "findAgrupadosPorBandeiraComparacao[principal]");
        List<Object[]> bandeiraConcC = validarLinhas(
                conciliacaoTaxaRepository.findAgrupadosPorBandeiraComparacao(estabelecimentoId, dataInicioComp, dataFimComp),
                CONC_GRP_COLS, "findAgrupadosPorBandeiraComparacao[comparacao]");

        List<Object[]> adqP = validarLinhas(
                conciliacaoTaxaRepository.findAgrupadosPorAdquirente(estabelecimentoId, dataInicio, dataFim),
                CONC_GRP_COLS, "findAgrupadosPorAdquirente[principal]");
        List<Object[]> adqC = validarLinhas(
                conciliacaoTaxaRepository.findAgrupadosPorAdquirente(estabelecimentoId, dataInicioComp, dataFimComp),
                CONC_GRP_COLS, "findAgrupadosPorAdquirente[comparacao]");

        // ── GrupoDescAjuste (período principal) ───────────────────────────────────
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

        BigDecimal totalGeralBruto      = grupos.stream().map(GrupoDescAjuste::valorBrutoTotal)     .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGeralTaxas      = grupos.stream().map(GrupoDescAjuste::totalTaxas)          .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGeralLiquido    = grupos.stream().map(GrupoDescAjuste::valorLiquidoTotal)    .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGeralTarifa     = grupos.stream().map(GrupoDescAjuste::tarifaTransacaoTotal) .reduce(BigDecimal.ZERO, BigDecimal::add);
        long       totalGeralTransacoes = grupos.stream().mapToLong(GrupoDescAjuste::quantidadeTransacoes).sum();

        // ── InsightsComparativos ──────────────────────────────────────────────────
        // totRecP: [0]=count, [1]=valorBruto, [2]=valorTaxa, [3]=valorLiquido, [4]=tarifaTransacao
        // totConcP: [0]=count, [1]=valorBruto, [2]=taxaNaoContratada, [3]=pctTaxa
        InsightsComparativos insights = new InsightsComparativos(
                calcularVariacao("Valor Bruto",             toBD(totRecP[1]),  toBD(totRecC[1])),
                calcularVariacao("Total em Taxas",          toBD(totRecP[2]),  toBD(totRecC[2])),
                calcularVariacao("Valor Líquido",           toBD(totRecP[3]),  toBD(totRecC[3])),
                calcularVariacao("Tarifa por Transação",    toBD(totRecP[4]),  toBD(totRecC[4])),
                calcularVariacao("Quantidade de Transações",
                        BigDecimal.valueOf(toLong(totRecP[0])),
                        BigDecimal.valueOf(toLong(totRecC[0]))),
                buildVariacoes6(bandeiraRecP, bandeiraRecC),
                buildVariacoes6(descAjusteP,  descAjusteC),
                calcularVariacao("Valor Bruto",             toBD(totConcP[1]), toBD(totConcC[1])),
                calcularVariacao("Taxa Não Contratada",     toBD(totConcP[2]), toBD(totConcC[2])),
                calcularVariacao("% Taxa Médio",            toBD(totConcP[3]), toBD(totConcC[3])),
                calcularVariacao("Quantidade de Transações",
                        BigDecimal.valueOf(toLong(totConcP[0])),
                        BigDecimal.valueOf(toLong(totConcC[0]))),
                buildVariacoes5(bandeiraConcP, bandeiraConcC),
                buildVariacoes5(adqP,          adqC)
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

    // ── Row builders ─────────────────────────────────────────────────────────────

    // [0]=grupo,[1]=count,[2]=valorBruto,[3]=valorTaxa,[4]=valorLiquido,[5]=tarifaTransacao
    private List<VariacaoPorGrupo> buildVariacoes6(List<Object[]> principal, List<Object[]> comparacao) {
        Map<String, Object[]> compMap = comparacao.stream()
                .filter(r -> r.length >= REC_GRP_COLS)
                .collect(Collectors.toMap(r -> grupoKey(r[0]), r -> r, (a, b) -> a));

        List<VariacaoPorGrupo> result = new ArrayList<>();
        for (Object[] row : principal) {
            if (row.length < REC_GRP_COLS) continue;
            String grupo = grupoKey(row[0]);
            Object[] comp = compMap.getOrDefault(grupo, emptyRow(REC_GRP_COLS));
            result.add(buildVPG(grupo, "valorBruto",      toBD(row[2]), toBD(comp[2])));
            result.add(buildVPG(grupo, "totalTaxas",      toBD(row[3]), toBD(comp[3])));
            result.add(buildVPG(grupo, "tarifaTransacao", toBD(row[5]), toBD(comp[5])));
            result.add(buildVPG(grupo, "valorLiquido",    toBD(row[4]), toBD(comp[4])));
        }
        return result;
    }

    // [0]=grupo,[1]=count,[2]=valorBruto,[3]=taxaNaoContratada,[4]=pctTaxa
    private List<VariacaoPorGrupo> buildVariacoes5(List<Object[]> principal, List<Object[]> comparacao) {
        Map<String, Object[]> compMap = comparacao.stream()
                .filter(r -> r.length >= CONC_GRP_COLS)
                .collect(Collectors.toMap(r -> grupoKey(r[0]), r -> r, (a, b) -> a));

        List<VariacaoPorGrupo> result = new ArrayList<>();
        for (Object[] row : principal) {
            if (row.length < CONC_GRP_COLS) continue;
            String grupo = grupoKey(row[0]);
            Object[] comp = compMap.getOrDefault(grupo, emptyRow(CONC_GRP_COLS));
            result.add(buildVPG(grupo, "valorBruto",             toBD(row[2]), toBD(comp[2])));
            result.add(buildVPG(grupo, "totalTaxaNaoContratada", toBD(row[3]), toBD(comp[3])));
        }
        return result;
    }

    // ── Variation calculations ────────────────────────────────────────────────────

    private VariacaoMetrica calcularVariacao(String label, BigDecimal principal, BigDecimal comparacao) {
        principal  = nvl(principal);
        comparacao = nvl(comparacao);
        BigDecimal pct = percentual(principal, comparacao);
        return new VariacaoMetrica(label, principal, comparacao, pct, direcao(pct));
    }

    private VariacaoPorGrupo buildVPG(String grupo, String metrica, BigDecimal principal, BigDecimal comparacao) {
        principal  = nvl(principal);
        comparacao = nvl(comparacao);
        BigDecimal pct = percentual(principal, comparacao);
        return new VariacaoPorGrupo(grupo, metrica, principal, comparacao, pct, direcao(pct));
    }

    private BigDecimal percentual(BigDecimal principal, BigDecimal comparacao) {
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

    // ── Safe data access ──────────────────────────────────────────────────────────

    /**
     * Returns the first row from a List<Object[]> aggregate query result.
     * Falls back to a zeroed array when the result is empty or the row is too short.
     */
    private Object[] primeiraLinha(List<Object[]> rows, int expectedCols) {
        if (rows == null || rows.isEmpty()) return emptyRow(expectedCols);
        Object[] row = rows.get(0);
        if (row == null || row.length < expectedCols) return emptyRow(expectedCols);
        return row;
    }

    /**
     * Validates that every row in the list has at least expectedCols columns.
     * Filters out malformed rows silently.
     */
    private List<Object[]> validarLinhas(List<Object[]> rows, int expectedCols, String queryName) {
        if (rows == null) return List.of();
        return rows.stream()
                .filter(row -> row != null && row.length >= expectedCols)
                .toList();
    }

    private Object[] emptyRow(int cols) {
        Object[] row = new Object[cols];
        row[0] = 0L;
        Arrays.fill(row, 1, cols, BigDecimal.ZERO);
        return row;
    }

    private String grupoKey(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    // ── Type converters ───────────────────────────────────────────────────────────

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal toBD(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal bd) return bd;
        if (obj instanceof Double d)     return BigDecimal.valueOf(d);
        if (obj instanceof Long l)       return BigDecimal.valueOf(l);
        if (obj instanceof Integer i)    return BigDecimal.valueOf(i);
        try { return new BigDecimal(obj.toString()); }
        catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    private Long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Long l)       return l;
        if (obj instanceof Integer i)    return i.longValue();
        if (obj instanceof BigDecimal bd) return bd.longValue();
        try { return Long.parseLong(obj.toString()); }
        catch (NumberFormatException e)  { return 0L; }
    }
}
