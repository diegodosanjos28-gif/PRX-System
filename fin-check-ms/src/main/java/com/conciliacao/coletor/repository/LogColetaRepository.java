package com.conciliacao.coletor.repository;

import com.conciliacao.coletor.entity.LogColeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LogColetaRepository extends JpaRepository<LogColeta, UUID> {
}
