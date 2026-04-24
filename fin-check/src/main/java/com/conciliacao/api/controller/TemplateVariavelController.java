package com.conciliacao.api.controller;

import com.conciliacao.api.dto.request.TemplateVariavelRequest;
import com.conciliacao.api.dto.response.TemplateVariavelResponse;
import com.conciliacao.api.service.TemplateVariavelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/template-variaveis")
@RequiredArgsConstructor
public class TemplateVariavelController {

    private final TemplateVariavelService service;

    @GetMapping
    public ResponseEntity<List<TemplateVariavelResponse>> listarTodas() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/globais")
    public ResponseEntity<List<TemplateVariavelResponse>> listarGlobais() {
        return ResponseEntity.ok(service.listarGlobais());
    }

    @PostMapping
    public ResponseEntity<TemplateVariavelResponse> criar(@Valid @RequestBody TemplateVariavelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemplateVariavelResponse> atualizar(@PathVariable Long id,
                                                               @Valid @RequestBody TemplateVariavelRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
