package com.conciliacao.api.service;

import com.conciliacao.api.dto.request.ImplantacaoClienteRequest;
import com.conciliacao.api.dto.request.ImplantacaoDemandaPatchRequest;
import com.conciliacao.api.dto.request.ImplantacaoDemandaRequest;
import com.conciliacao.api.dto.response.ImplantacaoClienteResponse;
import com.conciliacao.api.dto.response.ImplantacaoDemandaResponse;
import com.conciliacao.api.entity.Cliente;
import com.conciliacao.api.entity.ImplantacaoCliente;
import com.conciliacao.api.entity.ImplantacaoDemanda;
import com.conciliacao.api.exception.ResourceNotFoundException;
import com.conciliacao.api.mapper.ImplantacaoMapper;
import com.conciliacao.api.repository.ClienteRepository;
import com.conciliacao.api.repository.ImplantacaoClienteRepository;
import com.conciliacao.api.repository.ImplantacaoDemandaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImplantacaoService {

    private static final List<String> STATUS_VALIDOS = List.of("fluindo", "aguardando", "travado");

    private final ImplantacaoClienteRepository implantacaoRepo;
    private final ImplantacaoDemandaRepository demandaRepo;
    private final ClienteRepository clienteRepo;
    private final ImplantacaoMapper mapper;

    // ── Listagem ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ImplantacaoClienteResponse> listarTodos() {
        List<ImplantacaoCliente> todos = implantacaoRepo.findAllComCliente();

        // Uma query agrega count + maior prioridade de todas as implantações de uma vez
        Map<UUID, long[]> resumoMap = demandaRepo.findResumoAbertasPorImplantacao()
                .stream()
                .collect(Collectors.toMap(
                        r -> (UUID) r[0],
                        r -> new long[]{
                                ((Number) r[1]).longValue(),
                                ((Number) r[2]).longValue()
                        }
                ));

        return todos.stream()
                .map(impl -> {
                    long[] resumo = resumoMap.getOrDefault(impl.getId(), new long[]{0L, 0L});
                    int count           = (int) resumo[0];
                    String prioridade   = rankToPrioridade((int) resumo[1]);
                    return mapper.toListResponseComResumo(impl, count, prioridade);
                })
                .toList();
    }

    private static String rankToPrioridade(int rank) {
        return switch (rank) {
            case 4  -> "critica";
            case 3  -> "alta";
            case 2  -> "media";
            case 1  -> "baixa";
            default -> null;
        };
    }

    // ── Detalhe ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ImplantacaoClienteResponse buscarPorId(UUID id) {
        ImplantacaoCliente impl = implantacaoRepo.findByIdComDemandas(id)
                .orElseThrow(() -> new ResourceNotFoundException("Implantação", id));
        return mapper.toDetailResponse(impl);
    }

    // ── Criar ─────────────────────────────────────────────────────────────────

    @Transactional
    public ImplantacaoClienteResponse criar(ImplantacaoClienteRequest request) {
        validarStatusEtapa(request.etapa(), request.status());

        Cliente cliente = clienteRepo.findById(request.clienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", request.clienteId()));

        ImplantacaoCliente impl = mapper.toEntity(request);
        impl.setCliente(cliente);

        // Preenche timestamps se não informados
        if (impl.getEtapaIniciadaEm() == null) impl.setEtapaIniciadaEm(LocalDateTime.now());
        if (impl.getUltimoMovimento()  == null) impl.setUltimoMovimento(LocalDateTime.now());

        // Garante array vazio se adquirentes não informadas
        if (impl.getAdquirentes() == null) impl.setAdquirentes("[]");

        ImplantacaoCliente salvo = implantacaoRepo.save(impl);
        log.info("Implantação criada: cliente={} id={}", cliente.getRazaoSocial(), salvo.getId());
        return mapper.toListResponse(salvo);
    }

    // ── Atualizar ─────────────────────────────────────────────────────────────

    @Transactional
    public ImplantacaoClienteResponse atualizar(UUID id, ImplantacaoClienteRequest request) {
        validarStatusEtapa(request.etapa(), request.status());

        ImplantacaoCliente impl = implantacaoRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Implantação", id));

        // Troca de cliente: só se vier diferente
        if (!impl.getCliente().getId().equals(request.clienteId())) {
            Cliente novo = clienteRepo.findById(request.clienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente", request.clienteId()));
            impl.setCliente(novo);
        }

        impl.setEtapa(request.etapa());
        impl.setStatus(request.status());
        impl.setResponsavel(request.responsavel());
        impl.setDonoContato(request.donoContato());
        impl.setObservacoes(request.observacoes());

        if (request.adquirentes()    != null) impl.setAdquirentes(request.adquirentes().toString());
        if (request.progressJson()   != null) impl.setProgressJson(request.progressJson().toString());
        if (request.etapaIniciadaEm()!= null) impl.setEtapaIniciadaEm(request.etapaIniciadaEm());

        impl.setUltimoMovimento(
                request.ultimoMovimento() != null ? request.ultimoMovimento() : LocalDateTime.now()
        );

        return mapper.toListResponse(implantacaoRepo.save(impl));
    }

    // ── Deletar ───────────────────────────────────────────────────────────────

    @Transactional
    public void deletar(UUID id) {
        if (!implantacaoRepo.existsById(id)) {
            throw new ResourceNotFoundException("Implantação", id);
        }
        implantacaoRepo.deleteById(id);
        log.info("Implantação removida: id={}", id);
    }

    // ── Demandas ──────────────────────────────────────────────────────────────

    @Transactional
    public ImplantacaoDemandaResponse adicionarDemanda(UUID implantacaoId, ImplantacaoDemandaRequest request) {
        ImplantacaoCliente impl = implantacaoRepo.findById(implantacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Implantação", implantacaoId));

        ImplantacaoDemanda demanda = ImplantacaoDemanda.builder()
                .implantacao(impl)
                .descricao(request.descricao())
                .prioridade(request.prioridade() != null ? request.prioridade() : "media")
                .adquirente(request.adquirente())
                .tipo(request.tipo() != null ? request.tipo() : "pista")
                .build();

        // Registra atividade na implantação
        impl.setUltimoMovimento(LocalDateTime.now());
        implantacaoRepo.save(impl);

        return mapper.toResponse(demandaRepo.save(demanda));
    }

    @Transactional
    public ImplantacaoDemandaResponse atualizarDemanda(UUID implantacaoId, UUID demandaId,
                                                        ImplantacaoDemandaPatchRequest request) {
        if (!implantacaoRepo.existsById(implantacaoId)) {
            throw new ResourceNotFoundException("Implantação", implantacaoId);
        }

        ImplantacaoDemanda demanda = demandaRepo.findById(demandaId)
                .orElseThrow(() -> new ResourceNotFoundException("Demanda", demandaId));

        if (!demanda.getImplantacao().getId().equals(implantacaoId)) {
            throw new ResourceNotFoundException("Demanda não pertence a esta implantação");
        }

        if (request.descricao()  != null) demanda.setDescricao(request.descricao());
        if (request.concluida()  != null) demanda.setConcluida(request.concluida());
        if (request.prioridade() != null) demanda.setPrioridade(request.prioridade());
        if (request.adquirente() != null) demanda.setAdquirente(request.adquirente());
        if (request.tipo()       != null) demanda.setTipo(request.tipo());

        // Registra atividade na implantação
        implantacaoRepo.findById(implantacaoId).ifPresent(impl -> {
            impl.setUltimoMovimento(LocalDateTime.now());
            implantacaoRepo.save(impl);
        });

        return mapper.toResponse(demandaRepo.save(demanda));
    }

    @Transactional
    public void deletarDemanda(UUID implantacaoId, UUID demandaId) {
        if (!implantacaoRepo.existsById(implantacaoId)) {
            throw new ResourceNotFoundException("Implantação", implantacaoId);
        }

        ImplantacaoDemanda demanda = demandaRepo.findById(demandaId)
                .orElseThrow(() -> new ResourceNotFoundException("Demanda", demandaId));

        if (!demanda.getImplantacao().getId().equals(implantacaoId)) {
            throw new ResourceNotFoundException("Demanda não pertence a esta implantação");
        }

        demandaRepo.deleteById(demandaId);
    }

    // ── Validação ─────────────────────────────────────────────────────────────

    private void validarStatusEtapa(String etapa, String status) {
        if ("curral".equals(etapa)) {
            if (status != null) {
                throw new IllegalArgumentException(
                        "Status deve ser nulo quando a etapa é 'curral'");
            }
        } else {
            if (status == null || status.isBlank()) {
                throw new IllegalArgumentException(
                        "Status é obrigatório para a etapa '" + etapa + "'");
            }
            if (!STATUS_VALIDOS.contains(status)) {
                throw new IllegalArgumentException(
                        "Status inválido: '" + status + "'. Valores aceitos: fluindo, aguardando, travado");
            }
        }
    }
}
