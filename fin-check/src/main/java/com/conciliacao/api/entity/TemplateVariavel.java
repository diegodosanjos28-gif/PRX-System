package com.conciliacao.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "template_variaveis",
    uniqueConstraints = @UniqueConstraint(name = "uq_tv_template_ordem", columnNames = {"template_id", "ordem"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateVariavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String chave;

    @Column(nullable = false)
    private String descricao;

    @Column(name = "sistema_fixo", nullable = false)
    @Builder.Default
    private boolean sistemaFixo = false;

    @Column(nullable = false)
    private int ordem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private Template template;
}
