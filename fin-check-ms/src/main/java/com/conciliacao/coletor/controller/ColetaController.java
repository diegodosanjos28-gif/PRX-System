package com.conciliacao.coletor.controller;

import com.conciliacao.coletor.repository.ClienteRepository;
import com.conciliacao.coletor.service.ColetaAgendadaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Manual endpoints to trigger collection without waiting for the cron job.
 * All endpoints return HTTP 202 immediately — execution runs in background via @Async.
 */
@Slf4j
@RestController
@RequestMapping("/api/coleta")
@RequiredArgsConstructor
public class ColetaController {

    private static final String KEY_STATUS    = "status";
    private static final String KEY_MENSAGEM  = "mensagem";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String STATUS_VALUE  = "accepted";

    private final ColetaAgendadaService coletaAgendadaService;
    private final ClienteRepository     clienteRepository;

    /**
     * POST /api/coleta/iniciar
     * Triggers collection for ALL active clients and establishments.
     * Returns 202 immediately — collection runs asynchronously.
     */
    @PostMapping("/iniciar")
    public ResponseEntity<Map<String, String>> iniciarColetaGeral() {
        log.info("Manual general collection requested at {}", LocalDateTime.now());
        coletaAgendadaService.executarColetaManualGeral();
        return ResponseEntity.accepted().body(Map.of(
            KEY_STATUS,    STATUS_VALUE,
            KEY_MENSAGEM,  "Coleta geral iniciada em background",
            KEY_TIMESTAMP, LocalDateTime.now().toString()
        ));
    }

    /**
     * POST /api/coleta/cliente/{clienteId}
     * Triggers collection for all establishments of a specific client.
     * Returns 202 immediately — collection runs asynchronously.
     * Returns 404 if the client does not exist or is inactive.
     */
    @PostMapping("/cliente/{clienteId}")
    public ResponseEntity<Map<String, String>> iniciarColetaClienteRange(@PathVariable UUID clienteId,
                                                                         @RequestParam(value = "dataInicio", defaultValue = "") LocalDate dataInicio,
                                                                         @RequestParam(value = "dataFim",  defaultValue = "") LocalDate dataFim) {

        log.info("Manual collection requested for client {} at {}", clienteId, LocalDateTime.now());

        boolean clienteExisteEAtivo = clienteRepository.findById(clienteId)
                .map(c -> c.isAtivo())
                .orElse(false);

        if (!clienteExisteEAtivo) {
            return ResponseEntity.notFound().build();
        }

        coletaAgendadaService.executarColetaManualCliente(clienteId, dataInicio, dataFim);
        return ResponseEntity.accepted().body(Map.of(
                KEY_STATUS,    STATUS_VALUE,
                KEY_MENSAGEM,  "Coleta para cliente " + clienteId + " iniciada em background",
                KEY_TIMESTAMP, LocalDateTime.now().toString()
        ));
    }
}
