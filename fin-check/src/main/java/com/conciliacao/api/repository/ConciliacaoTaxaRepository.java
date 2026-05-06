package com.conciliacao.api.repository;

import com.conciliacao.api.entity.ConciliacaoTaxa;
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

}
