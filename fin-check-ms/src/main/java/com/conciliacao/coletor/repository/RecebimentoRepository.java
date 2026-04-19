package com.conciliacao.coletor.repository;

import com.conciliacao.coletor.entity.Recebimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface RecebimentoRepository extends JpaRepository<Recebimento, UUID> {

    /**
     * UPSERT idempotente baseado na constraint única (id_conciflex, estabelecimento_id).
     * Suporta registros com valorBruto/valorLiquido negativos (Ajuste a Débito, cod=2).
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO recebimentos (
            id, estabelecimento_id, id_conciflex,
            cod_tipo_lancamento, tipo_lancamento, cod_tipo_pagamento, tipo_pagamento,
            data_venda, data_previsao, data_pagamento, data_cancelamento,
            adquirente, cod_adquirente, bandeira, cod_bandeira, modalidade,
            nsu, autorizacao, cartao, numero_resumo_venda, produto, meio_captura,
            valor_bruto, taxa_percentual, valor_taxa, tarifa_transacao,
            taxa_antecipacao, valor_taxa_antecipacao, outras_despesas,
            valor_liquido, valor_liquido_s_antecipacao,
            parcela, total_parcelas, possui_taxa_minima,
            estabelecimento_conciflex, banco, agencia, conta_corrente,
            status_conciliacao, codigo_operadora_ajuste, desc_ajuste,
            classificacao_ajuste, autorizador, data_processamento, hora_processamento,
            nome_arquivo, observacoes, coletado_em
        ) VALUES (
            :id, :estabelecimentoId, :idConciflex,
            :codTipoLancamento, :tipoLancamento, :codTipoPagamento, :tipoPagamento,
            :dataVenda, :dataPrevisao, :dataPagamento, :dataCancelamento,
            :adquirente, :codAdquirente, :bandeira, :codBandeira, :modalidade,
            :nsu, :autorizacao, :cartao, :numeroResumoVenda, :produto, :meioCaptura,
            :valorBruto, :taxaPercentual, :valorTaxa, :tarifaTransacao,
            :taxaAntecipacao, :valorTaxaAntecipacao, :outrasDespesas,
            :valorLiquido, :valorLiquidoSAntecipacao,
            :parcela, :totalParcelas, :possuiTaxaMinima,
            :estabelecimentoConciflex, :banco, :agencia, :contaCorrente,
            :statusConciliacao, :codigoOperadoraAjuste, :descAjuste,
            :classificacaoAjuste, :autorizador, :dataProcessamento, :horaProcessamento,
            :nomeArquivo, :observacoes, :coletadoEm
        )
        ON CONFLICT (id_conciflex, estabelecimento_id)
        DO UPDATE SET
            cod_tipo_lancamento         = EXCLUDED.cod_tipo_lancamento,
            tipo_lancamento             = EXCLUDED.tipo_lancamento,
            cod_tipo_pagamento          = EXCLUDED.cod_tipo_pagamento,
            tipo_pagamento              = EXCLUDED.tipo_pagamento,
            data_venda                  = EXCLUDED.data_venda,
            data_previsao               = EXCLUDED.data_previsao,
            data_pagamento              = EXCLUDED.data_pagamento,
            data_cancelamento           = EXCLUDED.data_cancelamento,
            adquirente                  = EXCLUDED.adquirente,
            cod_adquirente              = EXCLUDED.cod_adquirente,
            bandeira                    = EXCLUDED.bandeira,
            cod_bandeira                = EXCLUDED.cod_bandeira,
            modalidade                  = EXCLUDED.modalidade,
            nsu                         = EXCLUDED.nsu,
            autorizacao                 = EXCLUDED.autorizacao,
            cartao                      = EXCLUDED.cartao,
            numero_resumo_venda         = EXCLUDED.numero_resumo_venda,
            produto                     = EXCLUDED.produto,
            meio_captura                = EXCLUDED.meio_captura,
            valor_bruto                 = EXCLUDED.valor_bruto,
            taxa_percentual             = EXCLUDED.taxa_percentual,
            valor_taxa                  = EXCLUDED.valor_taxa,
            tarifa_transacao            = EXCLUDED.tarifa_transacao,
            taxa_antecipacao            = EXCLUDED.taxa_antecipacao,
            valor_taxa_antecipacao      = EXCLUDED.valor_taxa_antecipacao,
            outras_despesas             = EXCLUDED.outras_despesas,
            valor_liquido               = EXCLUDED.valor_liquido,
            valor_liquido_s_antecipacao = EXCLUDED.valor_liquido_s_antecipacao,
            parcela                     = EXCLUDED.parcela,
            total_parcelas              = EXCLUDED.total_parcelas,
            possui_taxa_minima          = EXCLUDED.possui_taxa_minima,
            estabelecimento_conciflex   = EXCLUDED.estabelecimento_conciflex,
            banco                       = EXCLUDED.banco,
            agencia                     = EXCLUDED.agencia,
            conta_corrente              = EXCLUDED.conta_corrente,
            status_conciliacao          = EXCLUDED.status_conciliacao,
            codigo_operadora_ajuste     = EXCLUDED.codigo_operadora_ajuste,
            desc_ajuste                 = EXCLUDED.desc_ajuste,
            classificacao_ajuste        = EXCLUDED.classificacao_ajuste,
            autorizador                 = EXCLUDED.autorizador,
            data_processamento          = EXCLUDED.data_processamento,
            hora_processamento          = EXCLUDED.hora_processamento,
            nome_arquivo                = EXCLUDED.nome_arquivo,
            observacoes                 = EXCLUDED.observacoes,
            coletado_em                 = EXCLUDED.coletado_em
        """, nativeQuery = true)
    void upsert(
        @Param("id")                    UUID id,
        @Param("estabelecimentoId")     UUID estabelecimentoId,
        @Param("idConciflex")           String idConciflex,
        @Param("codTipoLancamento")     String codTipoLancamento,
        @Param("tipoLancamento")        String tipoLancamento,
        @Param("codTipoPagamento")      String codTipoPagamento,
        @Param("tipoPagamento")         String tipoPagamento,
        @Param("dataVenda")             LocalDate dataVenda,
        @Param("dataPrevisao")          LocalDate dataPrevisao,
        @Param("dataPagamento")         LocalDate dataPagamento,
        @Param("dataCancelamento")      LocalDate dataCancelamento,
        @Param("adquirente")            String adquirente,
        @Param("codAdquirente")         String codAdquirente,
        @Param("bandeira")              String bandeira,
        @Param("codBandeira")           String codBandeira,
        @Param("modalidade")            String modalidade,
        @Param("nsu")                   String nsu,
        @Param("autorizacao")           String autorizacao,
        @Param("cartao")                String cartao,
        @Param("numeroResumoVenda")     String numeroResumoVenda,
        @Param("produto")               String produto,
        @Param("meioCaptura")           String meioCaptura,
        @Param("valorBruto")            BigDecimal valorBruto,
        @Param("taxaPercentual")        BigDecimal taxaPercentual,
        @Param("valorTaxa")             BigDecimal valorTaxa,
        @Param("tarifaTransacao")       BigDecimal tarifaTransacao,
        @Param("taxaAntecipacao")       BigDecimal taxaAntecipacao,
        @Param("valorTaxaAntecipacao")  BigDecimal valorTaxaAntecipacao,
        @Param("outrasDespesas")        BigDecimal outrasDespesas,
        @Param("valorLiquido")          BigDecimal valorLiquido,
        @Param("valorLiquidoSAntecipacao") BigDecimal valorLiquidoSAntecipacao,
        @Param("parcela")               Integer parcela,
        @Param("totalParcelas")         Integer totalParcelas,
        @Param("possuiTaxaMinima")      String possuiTaxaMinima,
        @Param("estabelecimentoConciflex") String estabelecimentoConciflex,
        @Param("banco")                 String banco,
        @Param("agencia")               String agencia,
        @Param("contaCorrente")         String contaCorrente,
        @Param("statusConciliacao")     String statusConciliacao,
        @Param("codigoOperadoraAjuste") String codigoOperadoraAjuste,
        @Param("descAjuste")            String descAjuste,
        @Param("classificacaoAjuste")   String classificacaoAjuste,
        @Param("autorizador")           String autorizador,
        @Param("dataProcessamento")     LocalDate dataProcessamento,
        @Param("horaProcessamento")     String horaProcessamento,
        @Param("nomeArquivo")           String nomeArquivo,
        @Param("observacoes")           String observacoes,
        @Param("coletadoEm")            LocalDateTime coletadoEm
    );
}
