package com.conciliacao.api.service;

import com.conciliacao.api.dto.request.ClienteRequest;
import com.conciliacao.api.dto.response.ClienteResponse;
import com.conciliacao.api.entity.Cliente;
import com.conciliacao.api.exception.ConflictException;
import com.conciliacao.api.exception.ResourceNotFoundException;
import com.conciliacao.api.mapper.ClienteMapper;
import com.conciliacao.api.repository.ClienteExclusaoRepository;
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

    /** Única etapa a partir da qual a exclusão definitiva é permitida. */
    private static final String ETAPA_CURRAL = "curral";

    private final ClienteRepository clienteRepository;
    private final ClienteExclusaoRepository exclusaoRepository;
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
        cliente.setRelatorioDiarioAtivo(request.relatorioDiarioAtivo());

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

    /**
     * Exclui definitivamente o cliente e TODOS os dados exclusivamente dele — a ação
     * "Despachar" do Curral. Irreversível: não há soft delete, backup nem undo.
     *
     * <p>Não confundir com {@link #inativar(UUID)}, que apenas marca
     * {@code ativo = false} e preserva tudo. São operações distintas e ambas continuam
     * existindo.
     *
     * <h2>Restrição ao Curral</h2>
     * Só executa para cliente cuja implantação esteja em {@code etapa = 'curral'}. A
     * validação é feita aqui, no backend, e não apenas na tela: a funcionalidade pertence
     * ao Curral, e chamar a rota diretamente para um cliente em implantação é recusado
     * com 409, sem remover absolutamente nada.
     *
     * <h2>Ordem</h2>
     * Das folhas para a raiz. As FKs dos filhos de {@code estabelecimentos} são
     * {@code NO ACTION} de propósito — se a ordem estiver errada, o banco recusa a
     * operação em vez de deixar órfãos. Nenhuma FK foi alterada para viabilizar isto.
     *
     * <h2>Atomicidade</h2>
     * Tudo sob um único {@code @Transactional}: ou o cliente inteiro desaparece, ou nada
     * é removido. Não existe estado intermediário observável.
     *
     * @throws ResourceNotFoundException cliente inexistente.
     * @throws ConflictException         cliente sem implantação ou fora do Curral.
     */
    @Transactional
    public void excluirDefinitivamente(UUID clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", clienteId));

        String etapa = exclusaoRepository.findEtapaImplantacao(clienteId)
                .orElseThrow(() -> new ConflictException(
                        "Cliente não possui implantação e por isso não está no Curral. "
                      + "A exclusão definitiva só é permitida a partir do Curral."));

        if (!ETAPA_CURRAL.equals(etapa)) {
            throw new ConflictException(
                    "Cliente está na etapa '" + etapa + "' e não no Curral. "
                  + "A exclusão definitiva só é permitida para clientes do Curral.");
        }

        List<UUID> estabelecimentoIds = exclusaoRepository.findEstabelecimentoIds(clienteId);

        // Identificação apenas pela razão social. Credenciais Conciflex (login/senha) nunca
        // entram no log, nem cifradas.
        log.warn("Exclusão definitiva iniciada: clienteId={}, razaoSocial='{}', estabelecimentos={}",
                clienteId, cliente.getRazaoSocial(), estabelecimentoIds.size());

        int mensagens = exclusaoRepository.deleteMensagensPorCliente(clienteId);
        int historico = 0, taxas = 0, recebimentos = 0, logs = 0, resumos = 0;

        if (!estabelecimentoIds.isEmpty()) {
            mensagens   += exclusaoRepository.deleteMensagensPorEstabelecimentos(estabelecimentoIds);
            historico    = exclusaoRepository.deleteConciliacaoTaxasHistorico(estabelecimentoIds);
            taxas        = exclusaoRepository.deleteConciliacaoTaxas(estabelecimentoIds);
            recebimentos = exclusaoRepository.deleteRecebimentos(estabelecimentoIds);
            logs         = exclusaoRepository.deleteLogsColeta(estabelecimentoIds);
            resumos      = exclusaoRepository.deleteResumoColeta(estabelecimentoIds);
        }

        int implantacoes      = exclusaoRepository.deleteImplantacao(clienteId);
        int estabelecimentos  = exclusaoRepository.deleteEstabelecimentos(clienteId);
        int clientes          = exclusaoRepository.deleteCliente(clienteId);

        log.warn("Exclusão definitiva concluída: clienteId={}, razaoSocial='{}' | "
               + "mensagens={}, conciliacao_taxas_historico={}, conciliacao_taxas={}, "
               + "recebimentos={}, logs_coleta={}, resumo_coleta={}, implantacoes={}, "
               + "estabelecimentos={}, cliente={}",
                clienteId, cliente.getRazaoSocial(),
                mensagens, historico, taxas, recebimentos, logs, resumos,
                implantacoes, estabelecimentos, clientes);
    }

    public Cliente buscarEntidade(UUID id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));
    }

    @Transactional(readOnly = true)
    public List<Cliente> listarEntidadesAtivas() {
        return clienteRepository.findAllByAtivoTrue();
    }

    @Transactional(readOnly = true)
    public List<Cliente> listarEntidadesAtivasComRelatorioDiario() {
        return clienteRepository.findAllByAtivoTrueAndRelatorioDiarioAtivoTrue();
    }
}
