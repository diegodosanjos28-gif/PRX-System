package com.conciliacao.api.repository;

import com.conciliacao.api.entity.MensagemEnviada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MensagemEnviadaRepository extends JpaRepository<MensagemEnviada, UUID> {
    List<MensagemEnviada> findByClienteIdOrderByEnviadoEmDesc(UUID clienteId);
    Optional<MensagemEnviada> findByMetaMessageId(String metaMessageId);
    Page<MensagemEnviada> findByEstabelecimentoIdOrderByEnviadoEmDesc(UUID estabelecimentoId, Pageable pageable);
}
