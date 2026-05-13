package com.conciliacao.api.repository;

import com.conciliacao.api.entity.Recebimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RecebimentoRepository extends JpaRepository<Recebimento, UUID> {

    List<Recebimento> findByEstabelecimentoIdAndDataPagamentoBetween(
        UUID estabelecimentoId, LocalDate inicio, LocalDate fim
    );

    @Query("""
        SELECT r.bandeira,
               COUNT(r) AS quantidade,
               COALESCE(SUM(r.valorBruto), 0) AS totalBruto,
               COALESCE(SUM(r.valorLiquido), 0) AS totalLiquido,
               COALESCE(SUM(r.valorTaxa), 0) AS totalTaxa
        FROM Recebimento r
        WHERE r.estabelecimento.id = :estabelecimentoId
          AND r.dataPagamento BETWEEN :inicio AND :fim
        GROUP BY r.bandeira
        ORDER BY totalBruto DESC
        """)
    List<Object[]> findAgrupandoPorBandeira(
        @Param("estabelecimentoId") UUID estabelecimentoId,
        @Param("inicio") LocalDate inicio,
        @Param("fim") LocalDate fim
    );

    @Query("""
        SELECT COALESCE(SUM(r.valorLiquido), 0)
        FROM Recebimento r
        WHERE r.estabelecimento.id = :estabelecimentoId
          AND r.dataPagamento BETWEEN :inicio AND :fim
        """)
    BigDecimal sumValorLiquido(
        @Param("estabelecimentoId") UUID estabelecimentoId,
        @Param("inicio") LocalDate inicio,
        @Param("fim") LocalDate fim
    );

    @Query("""
        SELECT COALESCE(SUM(ABS(r.valorTaxa)), 0)
        FROM Recebimento r
        WHERE r.estabelecimento.id = :estabelecimentoId
          AND r.dataPagamento BETWEEN :inicio AND :fim
        """)
    BigDecimal sumValorTaxa(
        @Param("estabelecimentoId") UUID estabelecimentoId,
        @Param("inicio") LocalDate inicio,
        @Param("fim") LocalDate fim
    );

    @Query("""
        SELECT
            COALESCE(r.descAjuste, 'COBRANÇA - TARIFA OPERACIONAL DE REEMBOLSO (TOR)'),
            COUNT(r.id),
            COALESCE(SUM(r.valorBruto), 0),
            COALESCE(SUM(r.valorTaxa), 0),
            COALESCE(SUM(r.valorLiquido), 0),
            COALESCE(SUM(r.tarifaTransacao), 0)
        FROM Recebimento r
        WHERE r.estabelecimento.id = :estabelecimentoId
          AND r.dataVenda BETWEEN :dataInicio AND :dataFim
        GROUP BY COALESCE(r.descAjuste, 'COBRANÇA - TARIFA OPERACIONAL DE REEMBOLSO (TOR)')
        ORDER BY SUM(r.valorBruto) DESC
        """)
    List<Object[]> findAgrupadosPorDescAjuste(
        @Param("estabelecimentoId") UUID estabelecimentoId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    @Query("""
        SELECT r.bandeira,
            COUNT(r.id),
            COALESCE(SUM(r.valorBruto), 0),
            COALESCE(SUM(r.valorTaxa), 0),
            COALESCE(SUM(r.valorLiquido), 0),
            COALESCE(SUM(r.tarifaTransacao), 0)
        FROM Recebimento r
        WHERE r.estabelecimento.id = :estabelecimentoId
          AND r.dataVenda BETWEEN :dataInicio AND :dataFim
        GROUP BY r.bandeira
        """)
    List<Object[]> findAgrupadosPorBandeiraVenda(
        @Param("estabelecimentoId") UUID estabelecimentoId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    @Query("""
        SELECT COUNT(r.id),
            COALESCE(SUM(r.valorBruto), 0),
            COALESCE(SUM(r.valorTaxa), 0),
            COALESCE(SUM(r.valorLiquido), 0),
            COALESCE(SUM(r.tarifaTransacao), 0)
        FROM Recebimento r
        WHERE r.estabelecimento.id = :estabelecimentoId
          AND r.dataVenda BETWEEN :dataInicio AND :dataFim
        """)
    List<Object[]> findTotalizadoresPorPeriodo(
        @Param("estabelecimentoId") UUID estabelecimentoId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );
}
