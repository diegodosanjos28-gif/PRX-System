package com.conciliacao.api.controller;

import com.conciliacao.api.dto.request.EstabelecimentoRequest;
import com.conciliacao.api.entity.Estabelecimento;
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
    public ResponseEntity<List<Estabelecimento>> listar(@PathVariable UUID clienteId) {
        return ResponseEntity.ok(estabelecimentoService.listarPorCliente(clienteId));
    }

    @PostMapping("/api/clientes/{clienteId}/estabelecimentos")
    public ResponseEntity<Estabelecimento> criar(@PathVariable UUID clienteId,
                                                  @Valid @RequestBody EstabelecimentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(estabelecimentoService.criar(clienteId, request));
    }

    @PutMapping("/api/estabelecimentos/{id}")
    public ResponseEntity<Estabelecimento> atualizar(@PathVariable UUID id,
                                                      @Valid @RequestBody EstabelecimentoRequest request) {
        return ResponseEntity.ok(estabelecimentoService.atualizar(id, request));
    }

    @DeleteMapping("/api/estabelecimentos/{id}")
    public ResponseEntity<Void> inativar(@PathVariable UUID id) {
        estabelecimentoService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
