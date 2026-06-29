package com.conciliacao.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "razao_social", nullable = false)
    private String razaoSocial;

    @Column(name = "nome_fantasia")
    private String nomeFantasia;

    @Column(name = "cnpj", length = 14, nullable = false, unique = true)
    private String cnpj;

    @Column(name = "whatsapp", nullable = false)
    private String whatsapp;

    // Armazenado sempre criptografado (AES-256-GCM via CryptoService)
    @Column(name = "conciflex_login", columnDefinition = "TEXT", nullable = false)
    private String conciflex_login;

    // Armazenado sempre criptografado (AES-256-GCM via CryptoService)
    @Column(name = "conciflex_senha", columnDefinition = "TEXT", nullable = false)
    private String conciflex_senha;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private boolean ativo = true;

    @Column(name = "relatorio_diario_ativo", nullable = false)
    @Builder.Default
    private boolean relatorioDiarioAtivo = true;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Estabelecimento> estabelecimentos = new ArrayList<>();
}
