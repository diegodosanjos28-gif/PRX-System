package com.conciliacao.api.service;

import com.conciliacao.api.dto.request.TemplateRequest;
import com.conciliacao.api.dto.response.TemplateResponse;
import com.conciliacao.api.entity.Template;
import com.conciliacao.api.exception.ResourceNotFoundException;
import com.conciliacao.api.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository repository;
    private final TemplateVariavelService varavelService;

    @Transactional(readOnly = true)
    public List<TemplateResponse> listarTodos() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> listarAtivos() {
        return repository.findByAtivoTrueOrderByNomeAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TemplateResponse buscarPorId(Long id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public TemplateResponse criar(TemplateRequest request) {
        Template template = Template.builder()
            .nome(request.nome())
            .conteudo(request.conteudo())
            .ativo(request.ativo())
            .build();
        return toResponse(repository.save(template));
    }

    @Transactional
    public TemplateResponse atualizar(Long id, TemplateRequest request) {
        Template template = buscarEntidade(id);
        template.setNome(request.nome());
        template.setConteudo(request.conteudo());
        template.setAtivo(request.ativo());
        return toResponse(repository.save(template));
    }

    @Transactional
    public void excluir(Long id) {
        Template template = buscarEntidade(id);
        repository.delete(template);
    }

    public Template buscarEntidade(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Template", id));
    }

    private TemplateResponse toResponse(Template t) {
        return new TemplateResponse(
            t.getId(), t.getNome(), t.getConteudo(), t.isAtivo(),
            t.getVariaveis().stream().map(varavelService::toResponse).toList(),
            t.getCreatedAt(), t.getUpdatedAt()
        );
    }
}
