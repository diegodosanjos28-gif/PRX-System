package com.conciliacao.api.service;

import com.conciliacao.api.dto.request.EstabelecimentoRequest;
import com.conciliacao.api.dto.response.EstabelecimentoResponse;
import com.conciliacao.api.entity.Cliente;
import com.conciliacao.api.entity.Estabelecimento;
import com.conciliacao.api.exception.ConflictException;
import com.conciliacao.api.exception.ResourceNotFoundException;
import com.conciliacao.api.mapper.EstabelecimentoMapper;
import com.conciliacao.api.repository.EstabelecimentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EstabelecimentoService {

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final EstabelecimentoMapper estabelecimentoMapper;
    private final ClienteService clienteService;

    @Transactional(readOnly = true)
    public List<EstabelecimentoResponse> listarPorCliente(UUID clienteId) {
        clienteService.buscarEntidade(clienteId);
        return estabelecimentoRepository.findByClienteIdAndAtivoTrue(clienteId)
            .stream()
            .map(estabelecimentoMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public EstabelecimentoResponse buscarPorId(UUID id) {
        return estabelecimentoMapper.toResponse(buscarEntidade(id));
    }

    @Transactional
    public EstabelecimentoResponse criar(UUID clienteId, EstabelecimentoRequest request) {
        Cliente cliente = clienteService.buscarEntidade(clienteId);

        if (estabelecimentoRepository.existsByClienteIdAndIdentificadorConciflex(clienteId, request.identificadorConciflex())) {
            throw new ConflictException(
                "Já existe um estabelecimento com o identificador Conciflex '" + request.identificadorConciflex() + "' para este cliente"
            );
        }

        Estabelecimento est = Estabelecimento.builder()
            .cliente(cliente)
            .descricao(request.descricao())
            .identificadorConciflex(request.identificadorConciflex())
            .build();

        Estabelecimento salvo = estabelecimentoRepository.save(est);
        log.info("Estabelecimento criado: {} para cliente {}", salvo.getId(), clienteId);
        return estabelecimentoMapper.toResponse(salvo);
    }

    @Transactional
    public EstabelecimentoResponse atualizar(UUID id, EstabelecimentoRequest request) {
        Estabelecimento est = buscarEntidade(id);

        if (estabelecimentoRepository.existsByClienteIdAndIdentificadorConciflexAndIdNot(
                est.getCliente().getId(), request.identificadorConciflex(), id)) {
            throw new ConflictException(
                "O identificador Conciflex '" + request.identificadorConciflex() + "' já está em uso por outro estabelecimento deste cliente"
            );
        }

        est.setDescricao(request.descricao());
        est.setIdentificadorConciflex(request.identificadorConciflex());
        return estabelecimentoMapper.toResponse(estabelecimentoRepository.save(est));
    }

    @Transactional
    public void inativar(UUID id) {
        Estabelecimento est = buscarEntidade(id);
        est.setAtivo(false);
        estabelecimentoRepository.save(est);
        log.info("Estabelecimento inativado: {}", id);
    }

    public Estabelecimento buscarEntidade(UUID id) {
        return estabelecimentoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento", id));
    }
}
