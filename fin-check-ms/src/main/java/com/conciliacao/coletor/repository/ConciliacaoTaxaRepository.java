package com.conciliacao.coletor.repository;

import com.conciliacao.coletor.entity.ConciliacaoTaxa;
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
public interface ConciliacaoTaxaRepository extends JpaRepository<ConciliacaoTaxa, UUID> {

    /**
     * UPSERT idempotente baseado na constraint única (id_conciflex, estabelecimento_id).
     * Se o registro já existir (mesma coleta de dia anterior repetida), atualiza todos os campos.
     * Isso garante que reexecuções do job não duplicam dados.
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO conciliacao_taxas (
            id, estabelecimento_id, id_conciflex, codigo_empresa,
            data_venda, adquirente, codigo_adquirente, bandeira, cod_bandeira,
            modalidade, codigo_modalidade, produto, codigo_produto,
            valor_bruto, valor_desconto, percentual_taxa, taxa_contratada,
            quantidade, taxa_praticada_rs, taxa_praticada_cadastrada_rs,
            taxa_contratada_rs, total_taxa_nao_contratada_rs, perda_rs, perda,
            auditada, estabelecimento_conciflex, coletado_em
        ) VALUES (
            :id, :estabelecimentoId, :idConciflex, :codigoEmpresa,
            :dataVenda, :adquirente, :codigoAdquirente, :bandeira, :codBandeira,
            :modalidade, :codigoModalidade, :produto, :codigoProduto,
            :valorBruto, :valorDesconto, :percentualTaxa, :taxaContratada,
            :quantidade, :taxaPraticadaRs, :taxaPraticadaCadastradaRs,
            :taxaContratadaRs, :totalTaxaNaoContratadaRs, :perdaRs, :perda,
            :auditada, :estabelecimentoConciflex, :coletadoEm
        )
        ON CONFLICT (id_conciflex, estabelecimento_id)
        DO UPDATE SET
            codigo_empresa               = EXCLUDED.codigo_empresa,
            data_venda                   = EXCLUDED.data_venda,
            adquirente                   = EXCLUDED.adquirente,
            codigo_adquirente            = EXCLUDED.codigo_adquirente,
            bandeira                     = EXCLUDED.bandeira,
            cod_bandeira                 = EXCLUDED.cod_bandeira,
            modalidade                   = EXCLUDED.modalidade,
            codigo_modalidade            = EXCLUDED.codigo_modalidade,
            produto                      = EXCLUDED.produto,
            codigo_produto               = EXCLUDED.codigo_produto,
            valor_bruto                  = EXCLUDED.valor_bruto,
            valor_desconto               = EXCLUDED.valor_desconto,
            percentual_taxa              = EXCLUDED.percentual_taxa,
            taxa_contratada              = EXCLUDED.taxa_contratada,
            quantidade                   = EXCLUDED.quantidade,
            taxa_praticada_rs            = EXCLUDED.taxa_praticada_rs,
            taxa_praticada_cadastrada_rs = EXCLUDED.taxa_praticada_cadastrada_rs,
            taxa_contratada_rs           = EXCLUDED.taxa_contratada_rs,
            total_taxa_nao_contratada_rs = EXCLUDED.total_taxa_nao_contratada_rs,
            perda_rs                     = EXCLUDED.perda_rs,
            perda                        = EXCLUDED.perda,
            auditada                     = EXCLUDED.auditada,
            estabelecimento_conciflex    = EXCLUDED.estabelecimento_conciflex,
            coletado_em                  = EXCLUDED.coletado_em
        """, nativeQuery = true)
    void upsert(
        @Param("id")                        UUID id,
        @Param("estabelecimentoId")         UUID estabelecimentoId,
        @Param("idConciflex")               String idConciflex,
        @Param("codigoEmpresa")             String codigoEmpresa,
        @Param("dataVenda")                 LocalDate dataVenda,
        @Param("adquirente")                String adquirente,
        @Param("codigoAdquirente")          String codigoAdquirente,
        @Param("bandeira")                  String bandeira,
        @Param("codBandeira")               String codBandeira,
        @Param("modalidade")                String modalidade,
        @Param("codigoModalidade")          String codigoModalidade,
        @Param("produto")                   String produto,
        @Param("codigoProduto")             String codigoProduto,
        @Param("valorBruto")                BigDecimal valorBruto,
        @Param("valorDesconto")             BigDecimal valorDesconto,
        @Param("percentualTaxa")            BigDecimal percentualTaxa,
        @Param("taxaContratada")            BigDecimal taxaContratada,
        @Param("quantidade")                Integer quantidade,
        @Param("taxaPraticadaRs")           BigDecimal taxaPraticadaRs,
        @Param("taxaPraticadaCadastradaRs") BigDecimal taxaPraticadaCadastradaRs,
        @Param("taxaContratadaRs")          BigDecimal taxaContratadaRs,
        @Param("totalTaxaNaoContratadaRs")  BigDecimal totalTaxaNaoContratadaRs,
        @Param("perdaRs")                   BigDecimal perdaRs,
        @Param("perda")                     BigDecimal perda,
        @Param("auditada")                  String auditada,
        @Param("estabelecimentoConciflex")  String estabelecimentoConciflex,
        @Param("coletadoEm")                LocalDateTime coletadoEm
    );
}
