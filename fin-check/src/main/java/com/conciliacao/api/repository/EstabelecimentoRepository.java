package com.conciliacao.api.repository;

import com.conciliacao.api.entity.Estabelecimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EstabelecimentoRepository extends JpaRepository<Estabelecimento, UUID> {
    List<Estabelecimento> findByClienteIdAndAtivoTrue(UUID clienteId);
    List<Estabelecimento> findByClienteId(UUID clienteId);
}
