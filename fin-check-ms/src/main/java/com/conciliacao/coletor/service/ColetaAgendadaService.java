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
 * Responsável pelo agendamento e execução do job de coleta.
 * Itera sobre todos os clientes e estabelecimentos ativos.
 * Falha em um estabelecimento não interrompe o processamento dos demais.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ColetaAgendadaService {

    private final ClienteRepository       clienteRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final ColetorService          coletorService;

    @Value("${coleta.dias-retroativos:1}")
    private int diasRetroativos;

    @Value("${coleta.max-retries:3}")
    private int maxRetries;

    /**
     * Job agendado — executa no horário definido em COLETA_CRON (padrão: 17h diário).
     * Coleta dados de todos os clientes e estabelecimentos ativos.
     */
    @Scheduled(fixedDelay = 30)
    public void executarColetaAgendada() {
        log.info("=== Início do job de coleta agendada ===");
        LocalDate[] periodo = calcularPeriodo();
        LocalDate dataInicio = periodo[0];
        LocalDate dataFim    = periodo[1];

        log.info("Período de coleta: {} até {}", dataInicio, dataFim);

        List<Cliente> clientes = clienteRepository.findAllAtivosComEstabelecimentosAtivos();
        log.info("Clientes ativos encontrados: {}", clientes.size());

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
                    log.error("Estabelecimento '{}' falhou após {} tentativas. Continuando...",
                        estabelecimento.getIdentificadorConciflex(), maxRetries);
                }
            }
        }

        log.info("=== Job de coleta concluído: {}/{} estabelecimentos com sucesso, {} falhas ===",
            sucessos, totalEstabelecimentos, falhas);
    }

    /**
     * Dispara coleta assíncrona para todos os estabelecimentos ativos de todos os clientes.
     * Usado pelo endpoint manual POST /api/coleta/iniciar.
     */
    @Async
    public void executarColetaManualGeral() {
        log.info("Coleta manual geral iniciada");
        executarColetaAgendada();
    }

    /**
     * Dispara coleta assíncrona para todos os estabelecimentos ativos de um cliente específico.
     * Usado pelo endpoint manual POST /api/coleta/cliente/{clienteId}.
     */
    @Async
    public void executarColetaManualCliente(UUID clienteId) {
        log.info("Coleta manual iniciada para cliente {}", clienteId);
        LocalDate[] periodo = calcularPeriodo();
        LocalDate dataInicio = periodo[0];
        LocalDate dataFim    = periodo[1];

        List<Estabelecimento> estabelecimentos =
            estabelecimentoRepository.findAtivosComClienteByClienteId(clienteId);

        log.info("Estabelecimentos ativos do cliente {}: {}", clienteId, estabelecimentos.size());

        for (Estabelecimento est : estabelecimentos) {
            try {
                coletarComRetry(est, dataInicio, dataFim);
            } catch (Exception e) {
                log.error("Estabelecimento '{}' falhou. Continuando...",
                    est.getIdentificadorConciflex());
            }
        }
    }

    /**
     * Calcula o período de coleta com base em COLETA_DIAS_RETROATIVOS.
     * Exemplo: diasRetroativos=1 → coleta apenas D-1 (ontem).
     * Exemplo: diasRetroativos=30 → coleta últimos 30 dias.
     */
    private LocalDate[] calcularPeriodo() {
        LocalDate fim    = LocalDate.now().minusDays(1);                   // D-1
        LocalDate inicio = fim.minusDays(diasRetroativos - 1);
        return new LocalDate[]{inicio, fim};
    }

    /**
     * Executa a coleta com retry e backoff exponencial.
     * Tentativa 1: imediata
     * Tentativa 2: aguarda 1s
     * Tentativa 3: aguarda 2s
     * Após esgotar tentativas: relança a última exceção.
     */
    private void coletarComRetry(Estabelecimento estabelecimento, LocalDate inicio, LocalDate fim) {
        Exception ultimaExcecao = null;

        for (int tentativa = 1; tentativa <= maxRetries; tentativa++) {
            try {
                if (tentativa > 1) {
                    long esperaMs = (long) Math.pow(2, tentativa - 2) * 1000; // 1s, 2s, 4s...
                    log.info("Tentativa {} de {} para '{}' — aguardando {}ms",
                        tentativa, maxRetries, estabelecimento.getIdentificadorConciflex(), esperaMs);
                    Thread.sleep(esperaMs);
                }

                coletorService.coletar(estabelecimento, inicio, fim);
                return; // sucesso — sai do loop

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrompida durante retry", ie);
            } catch (Exception e) {
                ultimaExcecao = e;
                log.warn("Tentativa {}/{} falhou para '{}': {}",
                    tentativa, maxRetries, estabelecimento.getIdentificadorConciflex(), e.getMessage());
            }
        }

        throw new RuntimeException("Todas as tentativas esgotadas para '"
            + estabelecimento.getIdentificadorConciflex() + "'", ultimaExcecao);
    }
}
