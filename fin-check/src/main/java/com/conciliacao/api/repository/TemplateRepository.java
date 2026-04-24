package com.conciliacao.api.repository;

import com.conciliacao.api.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemplateRepository extends JpaRepository<Template, Long> {
    List<Template> findByAtivoTrueOrderByNomeAsc();
    boolean existsByNome(String nome);
}
