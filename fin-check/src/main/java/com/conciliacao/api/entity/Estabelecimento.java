package com.conciliacao.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "estabelecimentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Estabelecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    @JsonIgnore
    private Cliente cliente;

    @Column(name = "descricao", nullable = false)
    private String descricao;

    // Valor do campo ESTABELECIMENTO retornado pela API Conciflex (pode ser CNPJ ou código interno)
    @Column(name = "identificador_conciflex", nullable = false)
    private String identificadorConciflex;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private boolean ativo = true;
}
