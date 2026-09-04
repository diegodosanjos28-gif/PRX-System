package com.conciliacao.coletor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Linha agregada de conciliação de taxas para uma data de venda.
 *
 * <p>A identidade de negócio é a chave lógica
 * {@code (estabelecimento_id, data_venda, codigo_adquirente, cod_bandeira,
 * codigo_modalidade, codigo_produto, auditada)} — refletida pela constraint
 * {@code uq_ct_chave_logica} (migration V25).
 *
 * <p>{@code idConciflex} NÃO é identidade: a API Conciflex gera as linhas
 * agregadas no momento da consulta e atribui um ID novo a cada execução. Ele é
 * mantido apenas como rastreabilidade da última coleta.
 *
 * <p>A constraint real no banco usa {@code NULLS NOT DISTINCT}, semântica que a
 * anotação JPA não expressa. O banco é a fonte de verdade; esta declaração serve
 * para documentação e geração de DDL.
 */
@Entity
@Table(name = "conciliacao_taxas",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_ct_chave_logica",
        columnNames = {
            "estabelecimento_id", "data_venda", "codigo_adquirente", "cod_bandeira",
            "codigo_modalidade", "codigo_produto", "auditada"
        }
    ),
    indexes = {
        @Index(name = "idx_ct_estabelecimento_data", columnList = "estabelecimento_id, data_venda")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConciliacaoTaxa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    // ID único do registro na plataforma Conciflex
    @Column(name = "id_conciflex")
    private String idConciflex;

    @Column(name = "codigo_empresa")
    private String codigoEmpresa;

    @Column(name = "data_venda")
    private LocalDate dataVenda;

    @Column(name = "adquirente")
    private String adquirente;

    @Column(name = "codigo_adquirente")
    private String codigoAdquirente;

    @Column(name = "bandeira")
    private String bandeira;

    @Column(name = "cod_bandeira")
    private String codBandeira;

    @Column(name = "modalidade")
    private String modalidade;

    @Column(name = "codigo_modalidade")
    private String codigoModalidade;

    @Column(name = "produto")
    private String produto;

    @Column(name = "codigo_produto")
    private String codigoProduto;

    @Column(name = "valor_bruto", precision = 15, scale = 2)
    private BigDecimal valorBruto;

    @Column(name = "valor_desconto", precision = 15, scale = 6)
    private BigDecimal valorDesconto;

    // Taxa efetivamente cobrada (%)
    @Column(name = "percentual_taxa", precision = 8, scale = 4)
    private BigDecimal percentualTaxa;

    // Taxa contratual (%)
    @Column(name = "taxa_contratada", precision = 8, scale = 4)
    private BigDecimal taxaContratada;

    @Column(name = "quantidade")
    private Integer quantidade;

    @Column(name = "taxa_praticada_rs", precision = 15, scale = 2)
    private BigDecimal taxaPraticadaRs;

    @Column(name = "taxa_praticada_cadastrada_rs", precision = 15, scale = 2)
    private BigDecimal taxaPraticadaCadastradaRs;

    @Column(name = "taxa_contratada_rs", precision = 15, scale = 2)
    private BigDecimal taxaContratadaRs;

    // Campo principal de auditoria: diferença entre taxa cobrada e contratada em R$
    @Column(name = "total_taxa_nao_contratada_rs", precision = 15, scale = 2)
    private BigDecimal totalTaxaNaoContratadaRs;

    @Column(name = "perda_rs", precision = 15, scale = 2)
    private BigDecimal perdaRs;

    @Column(name = "perda", precision = 15, scale = 4)
    private BigDecimal perda;

    // 'S' ou 'N'
    @Column(name = "auditada", length = 1)
    private String auditada;

    // Campo ESTABELECIMENTO retornado pela API
    @Column(name = "estabelecimento_conciflex")
    private String estabelecimentoConciflex;

    @Column(name = "coletado_em")
    private LocalDateTime coletadoEm;
}
