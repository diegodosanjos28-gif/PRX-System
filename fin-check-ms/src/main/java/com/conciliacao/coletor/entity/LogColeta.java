package com.conciliacao.coletor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "logs_coleta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogColeta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;

    @Column(name = "executado_em", nullable = false)
    private LocalDateTime executadoEm;

    // Possible values: 'success' | 'login_failed' | 'timeout' | 'error' (see ColetorService.STATUS_*)
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "registros_coletados")
    private Integer registrosColetados;

    // Error message when status != 'success' — must never contain credentials or session cookies
    @Column(name = "mensagem_erro", columnDefinition = "TEXT")
    private String mensagemErro;
}
