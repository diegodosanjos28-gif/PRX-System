package com.conciliacao.api.controller;

import com.conciliacao.api.entity.LogColeta;
import com.conciliacao.api.service.LogColetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogColetaController {

    private final LogColetaService logColetaService;

    @GetMapping("/coleta")
    public ResponseEntity<List<LogColeta>> consultar(
        @RequestParam(required = false) UUID estabelecimentoId,
        @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(logColetaService.consultar(estabelecimentoId, status));
    }
}
