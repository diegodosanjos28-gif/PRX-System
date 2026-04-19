package com.conciliacao.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "recebimentos", indexes = {
    @Index(name = "idx_rec_estabelecimento_data", columnList = "estabelecimento_id, data_pagamento")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recebimento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    // Campo ID da API Conciflex
    @Column(name = "id_conciflex")
    private String idConciflex;

    // "1"=Pagamento, "2"=Ajuste Débito, "3"=Ajuste Crédito
    @Column(name = "cod_tipo_lancamento")
    private String codTipoLancamento;

    @Column(name = "tipo_lancamento")
    private String tipoLancamento;

    // "1"=Normal, "2"=Antecipado
    @Column(name = "cod_tipo_pagamento")
    private String codTipoPagamento;

    @Column(name = "tipo_pagamento")
    private String tipoPagamento;

    @Column(name = "data_venda")
    private LocalDate dataVenda;

    @Column(name = "data_previsao")
    private LocalDate dataPrevisao;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Column(name = "data_cancelamento")
    private LocalDate dataCancelamento;

    @Column(name = "adquirente")
    private String adquirente;

    @Column(name = "cod_adquirente")
    private String codAdquirente;

    @Column(name = "bandeira")
    private String bandeira;

    @Column(name = "cod_bandeira")
    private String codBandeira;

    @Column(name = "modalidade")
    private String modalidade;

    @Column(name = "nsu")
    private String nsu;

    @Column(name = "autorizacao")
    private String autorizacao;

    // Número mascarado do cartão
    @Column(name = "cartao")
    private String cartao;

    @Column(name = "numero_resumo_venda")
    private String numeroResumoVenda;

    @Column(name = "produto")
    private String produto;

    // "POS", "OUTROS"
    @Column(name = "meio_captura")
    private String meioCaptura;

    // NEGATIVO em ajustes a débito
    @Column(name = "valor_bruto", precision = 15, scale = 2)
    private BigDecimal valorBruto;

    @Column(name = "taxa_percentual", precision = 10, scale = 5)
    private BigDecimal taxaPercentual;

    // NEGATIVO (custo)
    @Column(name = "valor_taxa", precision = 15, scale = 6)
    private BigDecimal valorTaxa;

    @Column(name = "tarifa_transacao", precision = 15, scale = 2)
    private BigDecimal tarifaTransacao;

    @Column(name = "taxa_antecipacao", precision = 8, scale = 4)
    private BigDecimal taxaAntecipacao;

    @Column(name = "valor_taxa_antecipacao", precision = 15, scale = 6)
    private BigDecimal valorTaxaAntecipacao;

    @Column(name = "outras_despesas", precision = 15, scale = 2)
    private BigDecimal outrasDespesas;

    // Alta precisão, NEGATIVO em ajustes
    @Column(name = "valor_liquido", precision = 18, scale = 6)
    private BigDecimal valorLiquido;

    @Column(name = "valor_liquido_s_antecipacao", precision = 15, scale = 6)
    private BigDecimal valorLiquidoSAntecipacao;

    @Column(name = "parcela")
    private Short parcela;

    @Column(name = "total_parcelas")
    private Short totalParcelas;

    // 'S' ou 'N'
    @Column(name = "possui_taxa_minima", length = 1)
    private String possuiTaxaMinima;

    @Column(name = "estabelecimento_conciflex")
    private String estabelecimentoConciflex;

    @Column(name = "banco")
    private String banco;

    @Column(name = "agencia")
    private String agencia;

    @Column(name = "conta_corrente")
    private String contaCorrente;

    @Column(name = "status_conciliacao")
    private String statusConciliacao;

    @Column(name = "codigo_operadora_ajuste")
    private String codigoOperadoraAjuste;

    @Column(name = "desc_ajuste", columnDefinition = "TEXT")
    private String descAjuste;

    @Column(name = "classificacao_ajuste")
    private String classificacaoAjuste;

    @Column(name = "autorizador")
    private String autorizador;

    @Column(name = "data_processamento")
    private LocalDate dataProcessamento;

    // Armazenado como String (HH:mm:ss)
    @Column(name = "hora_processamento")
    private String horaProcessamento;

    @Column(name = "nome_arquivo")
    private String nomeArquivo;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "coletado_em")
    private LocalDateTime coletadoEm;
}
