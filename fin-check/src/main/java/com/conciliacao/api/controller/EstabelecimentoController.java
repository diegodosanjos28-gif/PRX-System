package com.conciliacao.api.controller;

import com.conciliacao.api.dto.request.EstabelecimentoRequest;
import com.conciliacao.api.dto.response.EstabelecimentoResponse;
import com.conciliacao.api.service.EstabelecimentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class EstabelecimentoController {

    private final EstabelecimentoService estabelecimentoService;

    @GetMapping("/api/clientes/{clienteId}/estabelecimentos")
    public ResponseEntity<List<EstabelecimentoResponse>> listar(@PathVariable UUID clienteId) {
        return ResponseEntity.ok(estabelecimentoService.listarPorCliente(clienteId));
    }

    @GetMapping("/api/estabelecimentos/{id}")
    public ResponseEntity<EstabelecimentoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(estabelecimentoService.buscarPorId(id));
    }

    @PostMapping("/api/clientes/{clienteId}/estabelecimentos")
    public ResponseEntity<EstabelecimentoResponse> criar(@PathVariable UUID clienteId,
                                                         @Valid @RequestBody EstabelecimentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(estabelecimentoService.criar(clienteId, request));
    }

    @PutMapping("/api/estabelecimentos/{id}")
    public ResponseEntity<EstabelecimentoResponse> atualizar(@PathVariable UUID id,
                                                              @Valid @RequestBody EstabelecimentoRequest request) {
        return ResponseEntity.ok(estabelecimentoService.atualizar(id, request));
    }

    @DeleteMapping("/api/estabelecimentos/{id}")
    public ResponseEntity<Void> inativar(@PathVariable UUID id) {
        estabelecimentoService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
