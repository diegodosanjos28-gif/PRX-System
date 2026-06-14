package com.conciliacao.api.repository;

import com.conciliacao.api.entity.ImplantacaoDemanda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ImplantacaoDemandaRepository extends JpaRepository<ImplantacaoDemanda, UUID> {

    List<ImplantacaoDemanda> findByImplantacaoId(UUID implantacaoId);
}
