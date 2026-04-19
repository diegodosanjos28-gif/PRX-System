package com.conciliacao.api.repository;

import com.conciliacao.api.entity.LogColeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LogColetaRepository extends JpaRepository<LogColeta, UUID> {
    List<LogColeta> findByEstabelecimentoIdOrderByExecutadoEmDesc(UUID estabelecimentoId);
    List<LogColeta> findByEstabelecimentoIdAndStatusOrderByExecutadoEmDesc(UUID estabelecimentoId, String status);
    List<LogColeta> findByStatusOrderByExecutadoEmDesc(String status);
}
