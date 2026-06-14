package com.conciliacao.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "implantacoes_cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImplantacaoCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "etapa", nullable = false, length = 20)
    private String etapa;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "responsavel", length = 100)
    private String responsavel;

    @Column(name = "dono_contato", length = 255)
    private String donoContato;

    // Armazenado como JSONB — lista de adquirentes ex: ["Stone","Cielo"]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "adquirentes", columnDefinition = "jsonb", nullable = false)
    private String adquirentes;

    @Column(name = "data_entrada_curral")
    private LocalDate dataEntradaCurral;

    @Column(name = "etapa_iniciada_em")
    private LocalDateTime etapaIniciadaEm;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    // Armazenado como JSONB — checklists hierárquicos por etapa
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "progress_json", columnDefinition = "jsonb")
    private String progressJson;

    @Column(name = "ultimo_movimento")
    private LocalDateTime ultimoMovimento;

    @OneToMany(mappedBy = "implantacao", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    @Builder.Default
    private List<ImplantacaoDemanda> demandas = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
