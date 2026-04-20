package com.conciliacao.coletor.service;

import com.conciliacao.coletor.entity.Cliente;
import com.conciliacao.coletor.entity.Estabelecimento;
import com.conciliacao.coletor.repository.ClienteRepository;
import com.conciliacao.coletor.repository.EstabelecimentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Drives the scheduled collection job and exposes manual triggers.
 * Iterates all active clients and establishments; a failure in one establishment
 * does not interrupt the others.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ColetaAgendadaService {

    private final ClienteRepository          clienteRepository;
    private final EstabelecimentoRepository  estabelecimentoRepository;
    private final ColetorService             coletorService;

    @Value("${coleta.dias-retroativos:1}")
    private int diasRetroativos;

    @Value("${coleta.max-retries:3}")
    private int maxRetries;

    /**
     * Scheduled job — runs at the time configured in COLETA_CRON (default: 17h daily).
     * Collects data for all active clients and establishments.
     */
    @Scheduled(cron = "${coleta.cron:0 0 17 * * *}")
    public void executarColetaAgendada() {
        log.info("=== Collection job started ===");
        LocalDate[] periodo  = calcularPeriodo();
        LocalDate dataInicio = periodo[0];
        LocalDate dataFim    = periodo[1];

        log.info("Collection period: {} to {}", dataInicio, dataFim);

        List<Cliente> clientes = clienteRepository.findAllAtivosComEstabelecimentosAtivos();
        log.info("Active clients found: {}", clientes.size());

        int totalEstabelecimentos = 0;
        int sucessos = 0;
        int falhas   = 0;

        for (Cliente cliente : clientes) {
            List<Estabelecimento> estabelecimentos = cliente.getEstabelecimentos().stream()
                .filter(Estabelecimento::isAtivo)
                .toList();

            for (Estabelecimento estabelecimento : estabelecimentos) {
                totalEstabelecimentos++;
                try {
                    coletarComRetry(estabelecimento, dataInicio, dataFim);
                    sucessos++;
                } catch (Exception e) {
                    falhas++;
                    log.error("Establishment '{}' failed after {} attempts. Continuing...",
                        estabelecimento.getIdentificadorConciflex(), maxRetries);
                }
            }
        }

        log.info("=== Collection job finished: {}/{} establishments succeeded, {} failed ===",
            sucessos, totalEstabelecimentos, falhas);
    }

    /**
     * Async manual trigger for all active clients and establishments.
     * Used by POST /api/coleta/iniciar.
     */
    @Async
    public void executarColetaManualGeral() {
        log.info("Manual general collection started");
        executarColetaAgendada();
    }

    /**
     * Async manual trigger for all active establishments of a specific client.
     * Used by POST /api/coleta/cliente/{clienteId}.
     */
    @Async
    public void executarColetaManualCliente(UUID clienteId) {
        log.info("Manual collection started for client {}", clienteId);
        LocalDate[] periodo  = calcularPeriodo();
        LocalDate dataInicio = periodo[0];
        LocalDate dataFim    = periodo[1];

        List<Estabelecimento> estabelecimentos =
            estabelecimentoRepository.findAtivosComClienteByClienteId(clienteId);

        log.info("Active establishments for client {}: {}", clienteId, estabelecimentos.size());

        for (Estabelecimento est : estabelecimentos) {
            try {
                coletarComRetry(est, dataInicio, dataFim);
            } catch (Exception e) {
                log.error("Establishment '{}' failed. Continuing...", est.getIdentificadorConciflex());
            }
        }
    }

    /**
     * Calculates the collection window based on COLETA_DIAS_RETROATIVOS.
     * diasRetroativos=1 → D-1 only; diasRetroativos=30 → last 30 days ending at D-1.
     */
    private LocalDate[] calcularPeriodo() {
        LocalDate fim    = LocalDate.now().minusDays(1);
        LocalDate inicio = fim.minusDays(diasRetroativos - 1);
        return new LocalDate[]{inicio, fim};
    }

    /**
     * Runs collection with exponential-backoff retries.
     * Attempt 1: immediate; attempt 2: waits 1s; attempt 3: waits 2s.
     * Re-throws the last exception after all attempts are exhausted.
     */
    private void coletarComRetry(Estabelecimento estabelecimento, LocalDate inicio, LocalDate fim) {
        Exception ultimaExcecao = null;

        for (int tentativa = 1; tentativa <= maxRetries; tentativa++) {
            try {
                if (tentativa > 1) {
                    long esperaMs = (long) Math.pow(2, tentativa - 2) * 1000;
                    log.info("Attempt {} of {} for '{}' — waiting {}ms",
                        tentativa, maxRetries, estabelecimento.getIdentificadorConciflex(), esperaMs);
                    Thread.sleep(esperaMs);
                }

                coletorService.coletar(estabelecimento, inicio, fim);
                return;

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrupted during retry", ie);
            } catch (Exception e) {
                ultimaExcecao = e;
                log.warn("Attempt {}/{} failed for '{}': {}",
                    tentativa, maxRetries, estabelecimento.getIdentificadorConciflex(), e.getMessage());
            }
        }

        throw new RuntimeException("All attempts exhausted for '"
            + estabelecimento.getIdentificadorConciflex() + "'", ultimaExcecao);
    }
}
