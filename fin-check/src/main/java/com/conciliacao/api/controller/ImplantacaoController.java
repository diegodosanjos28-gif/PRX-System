package com.conciliacao.api.controller;

import com.conciliacao.api.dto.request.ImplantacaoClienteRequest;
import com.conciliacao.api.dto.request.ImplantacaoDemandaPatchRequest;
import com.conciliacao.api.dto.request.ImplantacaoDemandaRequest;
import com.conciliacao.api.dto.response.ImplantacaoClienteResponse;
import com.conciliacao.api.dto.response.ImplantacaoDemandaResponse;
import com.conciliacao.api.service.ImplantacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/implantacoes")
@RequiredArgsConstructor
public class ImplantacaoController {

    private final ImplantacaoService implantacaoService;

    // ── Implantações ──────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<ImplantacaoClienteResponse>> listar() {
        return ResponseEntity.ok(implantacaoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImplantacaoClienteResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(implantacaoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ImplantacaoClienteResponse> criar(
            @Valid @RequestBody ImplantacaoClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(implantacaoService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ImplantacaoClienteResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ImplantacaoClienteRequest request) {
        return ResponseEntity.ok(implantacaoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        implantacaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    // ── Demandas ──────────────────────────────────────────────────────────────

    @PostMapping("/{id}/demandas")
    public ResponseEntity<ImplantacaoDemandaResponse> adicionarDemanda(
            @PathVariable UUID id,
            @Valid @RequestBody ImplantacaoDemandaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(implantacaoService.adicionarDemanda(id, request));
    }

    @PatchMapping("/{id}/demandas/{demandaId}")
    public ResponseEntity<ImplantacaoDemandaResponse> atualizarDemanda(
            @PathVariable UUID id,
            @PathVariable UUID demandaId,
            @Valid @RequestBody ImplantacaoDemandaPatchRequest request) {
        return ResponseEntity.ok(implantacaoService.atualizarDemanda(id, demandaId, request));
    }

    @DeleteMapping("/{id}/demandas/{demandaId}")
    public ResponseEntity<Void> deletarDemanda(
            @PathVariable UUID id,
            @PathVariable UUID demandaId) {
        implantacaoService.deletarDemanda(id, demandaId);
        return ResponseEntity.noContent().build();
    }
}
