package com.conciliacao.api.controller;

import com.conciliacao.api.dto.request.ClienteRequest;
import com.conciliacao.api.dto.response.ClienteResponse;
import com.conciliacao.api.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> listar() {
        return ResponseEntity.ok(clienteService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> criar(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponse> atualizar(@PathVariable UUID id,
                                                      @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(clienteService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(@PathVariable UUID id) {
        clienteService.inativar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Exclusão DEFINITIVA do cliente e de todos os dados exclusivamente dele — a ação
     * "Despachar" do Curral. Irreversível.
     *
     * <p>Rota separada de propósito: {@code DELETE /api/clientes/{id}} continua sendo
     * inativação (soft delete). São operações diferentes e o caminho deixa isso explícito.
     *
     * <p>Permitida apenas para cliente com implantação em {@code etapa = 'curral'};
     * qualquer outro caso responde 409 sem remover nada.
     *
     * @return 204 na exclusão; 404 se o cliente não existe; 409 se não está no Curral.
     */
    @DeleteMapping("/{id}/permanente")
    public ResponseEntity<Void> excluirDefinitivamente(@PathVariable UUID id) {
        clienteService.excluirDefinitivamente(id);
        return ResponseEntity.noContent().build();
    }
}
