package com.conciliacao.api.repository;

import com.conciliacao.api.entity.ImplantacaoCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImplantacaoClienteRepository extends JpaRepository<ImplantacaoCliente, UUID> {

    // Para listagem: carrega cliente mas deixa demandas lazy (@BatchSize evita N+1)
    @Query("SELECT i FROM ImplantacaoCliente i JOIN FETCH i.cliente ORDER BY i.createdAt DESC")
    List<ImplantacaoCliente> findAllComCliente();

    // Para detalhe: carrega cliente + demandas em uma única query
    @Query("SELECT i FROM ImplantacaoCliente i JOIN FETCH i.cliente LEFT JOIN FETCH i.demandas WHERE i.id = :id")
    Optional<ImplantacaoCliente> findByIdComDemandas(@Param("id") UUID id);

    boolean existsByClienteId(UUID clienteId);
}
