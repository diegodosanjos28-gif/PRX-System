package com.conciliacao.api.controller;

import com.conciliacao.api.dto.response.RecebimentoResumoResponse;
import com.conciliacao.api.service.RecebimentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/recebimentos")
@RequiredArgsConstructor
public class RecebimentoController {

    private final RecebimentoService recebimentoService;

    @GetMapping("/{estabelecimentoId}")
    public ResponseEntity<RecebimentoResumoResponse> resumo(
        @PathVariable UUID estabelecimentoId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
    ) {
        return ResponseEntity.ok(recebimentoService.resumo(estabelecimentoId, dataInicio, dataFim));
    }
}
