package com.conciliacao.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mensagens_enviadas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensagemEnviada {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "conteudo", columnDefinition = "TEXT", nullable = false)
    private String conteudo;

    // 'ia' ou 'template'
    @Column(name = "modo_geracao", nullable = false)
    private String modoGeracao;

    @Column(name = "meta_message_id")
    private String metaMessageId;

    // 'sent' | 'delivered' | 'read' | 'failed'
    @Column(name = "status_entrega")
    private String statusEntrega;

    @CreationTimestamp
    @Column(name = "enviado_em", updatable = false)
    private LocalDateTime enviadoEm;
}
