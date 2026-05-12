package com.conciliacao.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ExperienciaClienteResponse(

        String estabelecimentoId,
        String descricaoEstabelecimento,

        String dataInicio,
        String dataFim,
        String dataInicioComparacao,
        String dataFimComparacao,

        List<GrupoDescAjuste> gruposRecebimento,
        BigDecimal totalGeralBruto,
        BigDecimal totalGeralTaxas,
        BigDecimal totalGeralLiquido,
        BigDecimal totalGeralTarifaTransacao,
        Long totalGeralTransacoes,

        InsightsComparativos insights

) {

    public record GrupoDescAjuste(
            String descAjuste,
            Long quantidadeTransacoes,
            BigDecimal valorBrutoTotal,
            BigDecimal totalTaxas,
            BigDecimal valorLiquidoTotal,
            BigDecimal tarifaTransacaoTotal
    ) {}

    public record InsightsComparativos(

            VariacaoMetrica recebimentos_valorBruto,
            VariacaoMetrica recebimentos_totalTaxas,
            VariacaoMetrica recebimentos_valorLiquido,
            VariacaoMetrica recebimentos_tarifaTransacao,
            VariacaoMetrica recebimentos_quantidadeTransacoes,

            List<VariacaoPorGrupo> recebimentos_porBandeira,
            List<VariacaoPorGrupo> recebimentos_porDescAjuste,

            VariacaoMetrica conciliacao_valorBruto,
            VariacaoMetrica conciliacao_totalTaxaNaoContratada,
            VariacaoMetrica conciliacao_percentualTaxaMedio,
            VariacaoMetrica conciliacao_quantidadeTransacoes,

            List<VariacaoPorGrupo> conciliacao_porBandeira,
            List<VariacaoPorGrupo> conciliacao_porAdquirente

    ) {

        public record VariacaoMetrica(
                String label,
                BigDecimal valorPeriodoPrincipal,
                BigDecimal valorPeriodoComparacao,
                BigDecimal variacaoPercentual,
                String direcao
        ) {}

        public record VariacaoPorGrupo(
                String grupo,
                String metrica,
                BigDecimal valorPrincipal,
                BigDecimal valorComparacao,
                BigDecimal variacaoPercentual,
                String direcao
        ) {}
    }
}
