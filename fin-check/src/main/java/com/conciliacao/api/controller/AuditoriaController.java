package com.conciliacao.api.controller;

import com.conciliacao.api.dto.response.AuditoriaResumoResponse;
import com.conciliacao.api.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @GetMapping("/{estabelecimentoId}")
    public ResponseEntity<AuditoriaResumoResponse> resumo(
        @PathVariable UUID estabelecimentoId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
    ) {
        return ResponseEntity.ok(auditoriaService.resumo(estabelecimentoId, dataInicio, dataFim));
    }
}
