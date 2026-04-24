package com.conciliacao.api.service;

import com.conciliacao.api.dto.request.ClienteRequest;
import com.conciliacao.api.dto.response.ClienteResponse;
import com.conciliacao.api.entity.Cliente;
import com.conciliacao.api.exception.ConflictException;
import com.conciliacao.api.exception.ResourceNotFoundException;
import com.conciliacao.api.mapper.ClienteMapper;
import com.conciliacao.api.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;
    private final CryptoService cryptoService;

    @Transactional(readOnly = true)
    public List<ClienteResponse> listarTodos() {
        return clienteRepository.findAllByAtivoTrue()
            .stream()
            .map(clienteMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(UUID id) {

        Cliente cliente = buscarEntidade(id);

        cliente.setConciflex_login(cryptoService.decrypt(cliente.getConciflex_login()));
        cliente.setConciflex_senha(cryptoService.decrypt(cliente.getConciflex_senha()));

        return clienteMapper.toResponse(cliente);

    }

    @Transactional
    public ClienteResponse criar(ClienteRequest request) {
        if (clienteRepository.existsByCnpj(request.cnpj())) {
            throw new ConflictException("Já existe um cliente cadastrado com o CNPJ " + request.cnpj());
        }

        Cliente cliente = clienteMapper.toEntity(request);
        cliente.setConciflex_login(cryptoService.encrypt(request.conciflexLogin()));
        cliente.setConciflex_senha(cryptoService.encrypt(request.conciflexSenha()));

        Cliente salvo = clienteRepository.save(cliente);
        log.info("Cliente criado: {} ({})", salvo.getRazaoSocial(), salvo.getId());
        return clienteMapper.toResponse(salvo);
    }

    @Transactional
    public ClienteResponse atualizar(UUID id, ClienteRequest request) {
        Cliente cliente = buscarEntidade(id);

        if (clienteRepository.existsByCnpjAndIdNot(request.cnpj(), id)) {
            throw new ConflictException("O CNPJ " + request.cnpj() + " já está em uso por outro cliente");
        }

        cliente.setRazaoSocial(request.razaoSocial());
        cliente.setNomeFantasia(request.nomeFantasia());
        cliente.setCnpj(request.cnpj());
        cliente.setWhatsapp(request.whatsapp());
        cliente.setObservacoes(request.observacoes());

        // Atualiza credenciais criptografadas apenas se fornecidas
        if (request.conciflexLogin() != null && !request.conciflexLogin().isBlank()) {
            cliente.setConciflex_login(cryptoService.encrypt(request.conciflexLogin()));
        }
        if (request.conciflexSenha() != null && !request.conciflexSenha().isBlank()) {
            cliente.setConciflex_senha(cryptoService.encrypt(request.conciflexSenha()));
        }

        return clienteMapper.toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public void inativar(UUID id) {
        Cliente cliente = buscarEntidade(id);
        cliente.setAtivo(false);
        clienteRepository.save(cliente);
        log.info("Cliente inativado: {}", id);
    }

    public Cliente buscarEntidade(UUID id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));
    }

    @Transactional(readOnly = true)
    public List<Cliente> listarEntidadesAtivas() {
        return clienteRepository.findAllByAtivoTrue();
    }
}
