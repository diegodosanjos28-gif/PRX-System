package com.conciliacao.coletor.entity;

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
public class Estabelecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "descricao", nullable = false)
    private String descricao;

    // Exact text shown in the Conciflex "Clientes" modal dropdown.
    // Must match the <option> label precisely so PlaywrightSessionService can select it.
    @Column(name = "identificador_conciflex", nullable = false)
    private String identificadorConciflex;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private boolean ativo = true;
}
