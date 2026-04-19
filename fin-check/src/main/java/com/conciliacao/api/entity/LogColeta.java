package com.conciliacao.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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

    @CreationTimestamp
    @Column(name = "executado_em", updatable = false)
    private LocalDateTime executadoEm;

    // 'success' | 'login_failed' | 'timeout' | 'error'
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "registros_coletados")
    private Integer registrosColetados;

    @Column(name = "mensagem_erro", columnDefinition = "TEXT")
    private String mensagemErro;
}
