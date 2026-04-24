package com.conciliacao.api.controller;

import com.conciliacao.api.dto.request.TemplateRequest;
import com.conciliacao.api.dto.response.TemplateResponse;
import com.conciliacao.api.service.TemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public ResponseEntity<List<TemplateResponse>> listar() {
        return ResponseEntity.ok(templateService.listarTodos());
    }

    @GetMapping("/ativos")
    public ResponseEntity<List<TemplateResponse>> listarAtivos() {
        return ResponseEntity.ok(templateService.listarAtivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(templateService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> criar(@Valid @RequestBody TemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> atualizar(@PathVariable Long id,
                                                       @Valid @RequestBody TemplateRequest request) {
        return ResponseEntity.ok(templateService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        templateService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
