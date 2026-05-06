package com.conciliacao.api.repository;

import com.conciliacao.api.entity.TemplateVariavel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemplateVariavelRepository extends JpaRepository<TemplateVariavel, Long> {
    List<TemplateVariavel> findAllByOrderByChaveAsc();
    List<TemplateVariavel> findByTemplateIsNullOrderByChaveAsc();
    List<TemplateVariavel> findByTemplateIdOrderByOrdemAsc(Long templateId);
}
