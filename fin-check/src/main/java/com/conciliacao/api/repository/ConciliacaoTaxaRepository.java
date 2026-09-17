package com.conciliacao.api.repository;

import com.conciliacao.api.entity.ConciliacaoTaxa;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ConciliacaoTaxaRepository extends JpaRepository<ConciliacaoTaxa, UUID> {

    List<ConciliacaoTaxa> findByEstabelecimentoIdAndDataVendaBetween(
        UUID estabelecimentoId, LocalDate inicio, LocalDate fim
    );

    @Query("""
        SELECT ct.bandeira,
               COUNT(ct) AS quantidade,
               COALESCE(SUM(ct.totalTaxaNaoContratadaRs), 0) AS diferencaTotal
        FROM ConciliacaoTaxa ct
        WHERE ct.estabelecimento.id = :estabelecimentoId
          AND ct.dataVenda BETWEEN :inicio AND :fim
        GROUP BY ct.bandeira
        ORDER BY diferencaTotal DESC
        """)
    List<Object[]> findAgrupandoPorBandeira(
        @Param("estabelecimentoId") UUID estabelecimentoId,
        @Param("inicio") LocalDate inicio,
        @Param("fim") LocalDate fim
    );

    @Query("""
        SELECT COALESCE(SUM(ct.totalTaxaNaoContratadaRs), 0)
        FROM ConciliacaoTaxa ct
        WHERE ct.estabelecimento.id = :estabelecimentoId
          AND ct.dataVenda BETWEEN :inicio AND :fim
          AND ct.totalTaxaNaoContratadaRs > 0
        """)
    BigDecimal sumTaxaNaoContratadaPositiva(
        @Param("estabelecimentoId") UUID estabelecimentoId,
        @Param("inicio") LocalDate inicio,
        @Param("fim") LocalDate fim
    );

    @Query("""
        SELECT COALESCE(SUM(ct.totalTaxaNaoContratadaRs), 0)
        FROM ConciliacaoTaxa ct
        WHERE ct.estabelecimento.id = :estabelecimentoId
          AND ct.dataVenda BETWEEN :inicio AND :fim
          AND ct.totalTaxaNaoContratadaRs < 0
        """)
    BigDecimal sumTaxaNaoContratadaNegativa(
        @Param("estabelecimentoId") UUID estabelecimentoId,
        @Param("inicio") LocalDate inicio,
        @Param("fim") LocalDate fim
    );

    Long countByEstabelecimentoIdAndDataVendaBetween(
        UUID estabelecimentoId, LocalDate inicio, LocalDate fim
    );

    @Query("""
        SELECT COALESCE(SUM(ct.taxaPraticadaRs), 0)
        FROM ConciliacaoTaxa ct
        WHERE ct.estabelecimento.id = :estabelecimentoId
          AND ct.dataVenda BETWEEN :inicio AND :fim
        """)
    BigDecimal sumTaxaPraticadaRs(
        @Param("estabelecimentoId") UUID estabelecimentoId,
        @Param("inicio") LocalDate inicio,
        @Param("fim") LocalDate fim
    );

    @Query("""
        SELECT COUNT(c.id),
            COALESCE(SUM(c.valorBruto), 0),
            COALESCE(SUM(c.totalTaxaNaoContratadaRs), 0),
            COALESCE(AVG(c.percentualTaxa), 0)
        FROM ConciliacaoTaxa c
        WHERE c.estabelecimento.id = :estabelecimentoId
          AND c.dataVenda BETWEEN :dataInicio AND :dataFim
        """)
    List<Object[]> findTotalizadoresPorPeriodo(
        @Param("estabelecimentoId") UUID estabelecimentoId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    @Query("""
        SELECT c.bandeira,
            COUNT(c.id),
            COALESCE(SUM(c.valorBruto), 0),
            COALESCE(SUM(c.totalTaxaNaoContratadaRs), 0),
            COALESCE(AVG(c.percentualTaxa), 0)
        FROM ConciliacaoTaxa c
        WHERE c.estabelecimento.id = :estabelecimentoId
          AND c.dataVenda BETWEEN :dataInicio AND :dataFim
        GROUP BY c.bandeira
        """)
    List<Object[]> findAgrupadosPorBandeiraComparacao(
        @Param("estabelecimentoId") UUID estabelecimentoId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    @Query("""
        SELECT c.adquirente,
            COUNT(c.id),
            COALESCE(SUM(c.valorBruto), 0),
            COALESCE(SUM(c.totalTaxaNaoContratadaRs), 0),
            COALESCE(AVG(c.percentualTaxa), 0)
        FROM ConciliacaoTaxa c
        WHERE c.estabelecimento.id = :estabelecimentoId
          AND c.dataVenda BETWEEN :dataInicio AND :dataFim
        GROUP BY c.adquirente
        """)
    List<Object[]> findAgrupadosPorAdquirente(
        @Param("estabelecimentoId") UUID estabelecimentoId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    /**
     * Retorna os registros com maior percentualTaxa para todos os estabelecimentos
     * do cliente no período. Usar com PageRequest.of(0, 1) para obter apenas o topo.
     */
    @Query("""
        SELECT ct FROM ConciliacaoTaxa ct
        WHERE ct.estabelecimento.cliente.id = :clienteId
          AND ct.dataVenda BETWEEN :inicio AND :fim
          AND ct.percentualTaxa IS NOT NULL
        ORDER BY ct.percentualTaxa DESC
        """)
    List<ConciliacaoTaxa> findMaioresTaxasByCliente(
        @Param("clienteId") UUID clienteId,
        @Param("inicio") LocalDate inicio,
        @Param("fim") LocalDate fim,
        Pageable pageable
    );

    /**
     * Valor bruto VENDIDO no período, pela data da venda.
     *
     * <p>Base do indicador "Vendas da semana" do relatório semanal. Não confundir com
     * {@code recebimentos}: aquela tabela registra liquidações, indexadas por
     * {@code data_pagamento}, e uma venda pode ser liquidada em datas — e em parcelas —
     * diferentes da data em que ocorreu.
     *
     * <p>Agrega exclusivamente {@code conciliacao_taxas}, sem JOIN com outras tabelas,
     * para não multiplicar as linhas da chave lógica protegida por {@code uq_ct_chave_logica}.
     */
    @Query("""
        SELECT COALESCE(SUM(ct.valorBruto), 0)
        FROM ConciliacaoTaxa ct
        WHERE ct.estabelecimento.id = :estabelecimentoId
          AND ct.dataVenda BETWEEN :inicio AND :fim
        """)
    BigDecimal sumValorBruto(
        @Param("estabelecimentoId") UUID estabelecimentoId,
        @Param("inicio") LocalDate inicio,
        @Param("fim") LocalDate fim
    );

    /**
     * Valor bruto vendido no período agrupado por {@code codigo_modalidade}.
     *
     * <p>Base do mix de pagamento (Pix / Débito / Crédito / Voucher). O agrupamento usa o
     * CÓDIGO e não o nome descritivo: o código é estável e integra a chave lógica da
     * tabela, enquanto o nome varia entre coletas.
     *
     * @return linhas {@code [codigoModalidade (String), somaValorBruto (BigDecimal)]}.
     */
    @Query("""
        SELECT ct.codigoModalidade, COALESCE(SUM(ct.valorBruto), 0)
        FROM ConciliacaoTaxa ct
        WHERE ct.estabelecimento.id = :estabelecimentoId
          AND ct.dataVenda BETWEEN :inicio AND :fim
        GROUP BY ct.codigoModalidade
        """)
    List<Object[]> sumValorBrutoPorModalidade(
        @Param("estabelecimentoId") UUID estabelecimentoId,
        @Param("inicio") LocalDate inicio,
        @Param("fim") LocalDate fim
    );

}
