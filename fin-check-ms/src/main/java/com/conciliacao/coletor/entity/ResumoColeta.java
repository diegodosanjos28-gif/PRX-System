package com.conciliacao.coletor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resumo_coleta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumoColeta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    // Collection type: 'conciliacao_taxas' or 'recebimentos' (see ColetorService.TIPO_*)
    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "total_registros")
    private Integer totalRegistros;

    @Column(name = "valor_bruto_total", precision = 18, scale = 4)
    private BigDecimal valorBrutoTotal;

    @Column(name = "valor_liquido_total", precision = 18, scale = 6)
    private BigDecimal valorLiquidoTotal;

    @Column(name = "total_taxas", precision = 15, scale = 4)
    private BigDecimal totalTaxas;

    // Demais campos da raiz da resposta da API serializados como JSON para consulta futura
    @Column(name = "totalizadores_json", columnDefinition = "TEXT")
    private String totalizadoresJson;

    @Column(name = "coletado_em")
    private LocalDateTime coletadoEm;
}
