package com.conciliacao.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "template_variaveis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateVariavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String chave;

    @Column(nullable = false)
    private String descricao;

    @Column(name = "sistema_fixo", nullable = false)
    @Builder.Default
    private boolean sistemaFixo = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private Template template;
}
