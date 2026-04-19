package com.conciliacao.coletor.repository;

import com.conciliacao.coletor.entity.ResumoColeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ResumoColetaRepository extends JpaRepository<ResumoColeta, UUID> {
}
