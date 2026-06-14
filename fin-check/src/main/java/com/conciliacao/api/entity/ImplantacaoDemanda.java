package com.conciliacao.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "implantacao_demandas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImplantacaoDemanda {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "implantacao_id", nullable = false)
    @JsonIgnore
    private ImplantacaoCliente implantacao;

    @Column(name = "descricao", columnDefinition = "TEXT", nullable = false)
    private String descricao;

    @Column(name = "concluida", nullable = false)
    @Builder.Default
    private boolean concluida = false;

    @Column(name = "adquirente", length = 100)
    private String adquirente;

    // critica | alta | media | baixa
    @Column(name = "prioridade", nullable = false, length = 20)
    @Builder.Default
    private String prioridade = "media";

    // pista | curral
    @Column(name = "tipo", nullable = false, length = 20)
    @Builder.Default
    private String tipo = "pista";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
