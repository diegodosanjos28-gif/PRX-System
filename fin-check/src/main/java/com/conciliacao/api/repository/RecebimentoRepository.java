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
}
