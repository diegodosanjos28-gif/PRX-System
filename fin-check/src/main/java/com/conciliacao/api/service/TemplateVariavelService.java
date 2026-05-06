package com.conciliacao.api.service;

import com.conciliacao.api.dto.request.TemplateVariavelRequest;
import com.conciliacao.api.dto.response.TemplateVariavelResponse;
import com.conciliacao.api.entity.Template;
import com.conciliacao.api.entity.TemplateVariavel;
import com.conciliacao.api.exception.ResourceNotFoundException;
import com.conciliacao.api.repository.TemplateRepository;
import com.conciliacao.api.repository.TemplateVariavelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplateVariavelService {

    private final TemplateVariavelRepository repository;
    private final TemplateRepository templateRepository;

    @Transactional(readOnly = true)
    public List<TemplateVariavelResponse> listarTodas() {
        return repository.findAllByOrderByChaveAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TemplateVariavelResponse> listarGlobais() {
        return repository.findByTemplateIsNullOrderByChaveAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public TemplateVariavelResponse criar(TemplateVariavelRequest request) {
        TemplateVariavel variavel = new TemplateVariavel();
        variavel.setChave(request.chave());
        variavel.setDescricao(request.descricao());
        variavel.setOrdem(request.ordem());
        variavel.setSistemaFixo(false);

        if (request.templateId() != null) {
            Template template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new ResourceNotFoundException("Template", request.templateId()));
            variavel.setTemplate(template);
        }

        return toResponse(repository.save(variavel));
    }

    @Transactional
    public TemplateVariavelResponse atualizar(Long id, TemplateVariavelRequest request) {
        TemplateVariavel variavel = buscarEntidade(id);

        variavel.setChave(request.chave());
        variavel.setDescricao(request.descricao());
        variavel.setOrdem(request.ordem());

        if (request.templateId() != null) {
            Template template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new ResourceNotFoundException("Template", request.templateId()));
            variavel.setTemplate(template);
        } else {
            variavel.setTemplate(null);
        }

        return toResponse(repository.save(variavel));
    }

    @Transactional
    public void excluir(Long id) {
        TemplateVariavel variavel = buscarEntidade(id);
        if (variavel.isSistemaFixo()) {
            throw new IllegalArgumentException("Variáveis do sistema não podem ser excluídas");
        }
        repository.delete(variavel);
    }

    private TemplateVariavel buscarEntidade(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TemplateVariavel", id));
    }

    TemplateVariavelResponse toResponse(TemplateVariavel v) {
        return new TemplateVariavelResponse(
            v.getId(), v.getChave(), v.getDescricao(), v.isSistemaFixo(), v.getOrdem(),
            v.getTemplate() != null ? v.getTemplate().getId() : null
        );
    }
}
