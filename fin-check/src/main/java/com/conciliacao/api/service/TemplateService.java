package com.conciliacao.api.service;

import com.conciliacao.api.dto.request.TemplateRequest;
import com.conciliacao.api.dto.request.TemplateVariavelRequest;
import com.conciliacao.api.dto.response.TemplateResponse;
import com.conciliacao.api.entity.Template;
import com.conciliacao.api.entity.TemplateVariavel;
import com.conciliacao.api.exception.ResourceNotFoundException;
import com.conciliacao.api.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
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
            .metaId(request.metaId())
            .conteudo(request.conteudo())
            .ativo(request.ativo())
            .build();
        template = repository.save(template);

        if (request.variaveis() != null && !request.variaveis().isEmpty()) {
            validarOrdemVariaveis(request.variaveis());
            for (TemplateVariavelRequest v : request.variaveis()) {
                TemplateVariavel variavel = buildVariavel(v, template);
                template.getVariaveis().add(variavel);
            }
            repository.save(template);
        }

        return toResponse(template);
    }

    @Transactional
    public TemplateResponse atualizar(Long id, TemplateRequest request) {
        Template template = buscarEntidade(id);
        template.setNome(request.nome());
        template.setMetaId(request.metaId());
        template.setConteudo(request.conteudo());
        template.setAtivo(request.ativo());

        if (request.variaveis() != null) {
            validarOrdemVariaveis(request.variaveis());
            template.getVariaveis().clear();
            // Flush DELETEs immediately so the unique constraint on (template_id, ordem)
            // is not violated when Hibernate inserts the new rows in the same flush cycle.
            repository.saveAndFlush(template);
            for (TemplateVariavelRequest v : request.variaveis()) {
                template.getVariaveis().add(buildVariavel(v, template));
            }
        }

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

    private TemplateVariavel buildVariavel(TemplateVariavelRequest req, Template template) {
        TemplateVariavel v = new TemplateVariavel();
        v.setChave(req.chave());
        v.setDescricao(req.descricao());
        v.setOrdem(req.ordem());
        v.setSistemaFixo(false);
        v.setTemplate(template);
        return v;
    }

    // Validates that ordem values form a gapless 1..N sequence with no duplicate chaves.
    private void validarOrdemVariaveis(List<TemplateVariavelRequest> variaveis) {
        List<Integer> ordens = variaveis.stream()
            .map(TemplateVariavelRequest::ordem)
            .sorted()
            .toList();
        for (int i = 0; i < ordens.size(); i++) {
            if (ordens.get(i) != i + 1) {
                throw new IllegalArgumentException(
                    "As ordens das variáveis devem ser sequenciais começando em 1. Encontrado: " + ordens
                );
            }
        }
        long distinctChaves = variaveis.stream().map(TemplateVariavelRequest::chave).distinct().count();
        if (distinctChaves != variaveis.size()) {
            throw new IllegalArgumentException("Variáveis do template não podem ter chaves duplicadas");
        }
    }

    private TemplateResponse toResponse(Template t) {
        return new TemplateResponse(
            t.getId(), t.getNome(), t.getMetaId(), t.getConteudo(), t.isAtivo(),
            t.getVariaveis().stream()
                .sorted(Comparator.comparingInt(TemplateVariavel::getOrdem))
                .map(varavelService::toResponse)
                .toList(),
            t.getCreatedAt(), t.getUpdatedAt()
        );
    }
}
