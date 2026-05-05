package com.conciliacao.api.repository;

import com.conciliacao.api.entity.TemplateVariavel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemplateVariavelRepository extends JpaRepository<TemplateVariavel, Long> {
    List<TemplateVariavel> findAllByOrderByChaveAsc();
    List<TemplateVariavel> findByTemplateIsNullOrderByChaveAsc();
    List<TemplateVariavel> findByTemplateIdOrderByOrdemAsc(Long templateId);
    // Global variable (template_id IS NULL) uniqueness checks
    boolean existsByChaveAndTemplateIsNull(String chave);
    boolean existsByChaveAndTemplateIsNullAndIdNot(String chave, Long id);

    // Template-specific variable uniqueness within the same template
    boolean existsByChaveAndTemplateId(String chave, Long templateId);
    boolean existsByChaveAndTemplateIdAndIdNot(String chave, Long templateId, Long id);
}
