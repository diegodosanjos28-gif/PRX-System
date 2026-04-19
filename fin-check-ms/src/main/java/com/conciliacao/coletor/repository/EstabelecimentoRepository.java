package com.conciliacao.coletor.repository;

import com.conciliacao.coletor.entity.Estabelecimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EstabelecimentoRepository extends JpaRepository<Estabelecimento, UUID> {

    // Busca estabelecimentos ativos de um cliente, com o cliente já carregado (evita N+1)
    @Query("SELECT e FROM Estabelecimento e JOIN FETCH e.cliente c " +
           "WHERE c.id = :clienteId AND e.ativo = true AND c.ativo = true")
    List<Estabelecimento> findAtivosComClienteByClienteId(@Param("clienteId") UUID clienteId);
}
