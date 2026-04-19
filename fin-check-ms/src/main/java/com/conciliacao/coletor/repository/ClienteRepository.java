package com.conciliacao.coletor.repository;

import com.conciliacao.coletor.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    // Busca clientes ativos com seus estabelecimentos para o job de coleta
    @Query("SELECT DISTINCT c FROM Cliente c LEFT JOIN FETCH c.estabelecimentos e " +
           "WHERE c.ativo = true AND (e IS NULL OR e.ativo = true)")
    List<Cliente> findAllAtivosComEstabelecimentosAtivos();
}
