package com.conciliacao.api.service;

import com.conciliacao.api.dto.request.EstabelecimentoRequest;
import com.conciliacao.api.entity.Cliente;
import com.conciliacao.api.entity.Estabelecimento;
import com.conciliacao.api.exception.ResourceNotFoundException;
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
    private final ClienteService clienteService;

    @Transactional(readOnly = true)
    public List<Estabelecimento> listarPorCliente(UUID clienteId) {
        clienteService.buscarEntidade(clienteId);
        return estabelecimentoRepository.findByClienteIdAndAtivoTrue(clienteId);
    }

    @Transactional
    public Estabelecimento criar(UUID clienteId, EstabelecimentoRequest request) {
        Cliente cliente = clienteService.buscarEntidade(clienteId);

        Estabelecimento est = Estabelecimento.builder()
            .cliente(cliente)
            .descricao(request.descricao())
            .identificadorConciflex(request.identificadorConciflex())
            .build();

        Estabelecimento salvo = estabelecimentoRepository.save(est);
        log.info("Estabelecimento criado: {} para cliente {}", salvo.getId(), clienteId);
        return salvo;
    }

    @Transactional
    public Estabelecimento atualizar(UUID id, EstabelecimentoRequest request) {
        Estabelecimento est = buscarEntidade(id);
        est.setDescricao(request.descricao());
        est.setIdentificadorConciflex(request.identificadorConciflex());
        return estabelecimentoRepository.save(est);
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
