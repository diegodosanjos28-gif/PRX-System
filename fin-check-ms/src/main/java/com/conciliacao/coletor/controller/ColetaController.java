package com.conciliacao.coletor.controller;

import com.conciliacao.coletor.repository.ClienteRepository;
import com.conciliacao.coletor.service.ColetaAgendadaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoints manuais para disparar coleta sem aguardar o cron job.
 * Todos retornam HTTP 202 (Accepted) imediatamente — execução em background (@Async).
 */
@Slf4j
@RestController
@RequestMapping("/api/coleta")
@RequiredArgsConstructor
public class ColetaController {

    private final ColetaAgendadaService coletaAgendadaService;
    private final ClienteRepository     clienteRepository;

    /**
     * POST /api/coleta/iniciar
     * Dispara coleta para TODOS os clientes e estabelecimentos ativos.
     * Retorna 202 imediatamente — a coleta ocorre em background.
     */
    @PostMapping("/iniciar")
    public ResponseEntity<Map<String, String>> iniciarColetaGeral() {
        log.info("Coleta manual geral solicitada às {}", LocalDateTime.now());

        coletaAgendadaService.executarColetaManualGeral();

        return ResponseEntity.accepted().body(Map.of(
            "status",    "accepted",
            "mensagem",  "Coleta geral iniciada em background",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * POST /api/coleta/cliente/{clienteId}
     * Dispara coleta apenas para os estabelecimentos de um cliente específico.
     * Retorna 202 imediatamente — a coleta ocorre em background.
     * Retorna 404 se o cliente não existir ou estiver inativo.
     */
    @PostMapping("/cliente/{clienteId}")
    public ResponseEntity<Map<String, String>> iniciarColetaCliente(@PathVariable UUID clienteId) {
        log.info("Coleta manual solicitada para cliente {} às {}", clienteId, LocalDateTime.now());

        // Verifica existência e ativo antes de aceitar a requisição
        boolean clienteExisteEAtivo = clienteRepository.findById(clienteId)
            .map(c -> c.isAtivo())
            .orElse(false);

        if (!clienteExisteEAtivo) {
            return ResponseEntity.notFound().build();
        }

        coletaAgendadaService.executarColetaManualCliente(clienteId);

        return ResponseEntity.accepted().body(Map.of(
            "status",    "accepted",
            "mensagem",  "Coleta para cliente " + clienteId + " iniciada em background",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
