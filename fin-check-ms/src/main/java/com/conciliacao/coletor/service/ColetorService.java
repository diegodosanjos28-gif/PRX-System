package com.conciliacao.coletor.service;

import com.conciliacao.coletor.dto.*;
import com.conciliacao.coletor.entity.*;
import com.conciliacao.coletor.exception.LoginConciflexException;
import com.conciliacao.coletor.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Orquestra o ciclo completo de coleta para um único estabelecimento.
 * Responsável por: autenticação, chamadas HTTP, persistência UPSERT e registro de log.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ColetorService {

    private static final String BASE_URL = "https://login.conciflex.com.br";

    private final CryptoService             cryptoService;
    private final PlaywrightSessionService  playwrightSessionService;
    private final ConciliacaoTaxaRepository conciliacaoTaxaRepository;
    private final RecebimentoRepository     recebimentoRepository;
    private final ResumoColetaRepository    resumoColetaRepository;
    private final LogColetaRepository       logColetaRepository;
    private final ObjectMapper              objectMapper;

    /**
     * Executa o ciclo completo de coleta para um estabelecimento.
     *
     * Fluxo:
     * 1. Descriptografa credenciais do cliente
     * 2. Abre sessão Playwright e navega pelo fluxo de login no Conciflex
     * 3. Extrai cookies de sessão e token CSRF
     * 4. Chama POST /conciliacao-taxas/buscar e persiste com UPSERT
     * 5. Chama POST /recebimentos-operadoras/buscar e persiste com UPSERT
     * 6. Persiste totalizadores em resumo_coleta
     * 7. Registra log_coleta com status 'success'
     *
     * Em caso de falha: registra log_coleta com status correto e relança a exceção.
     *
     * @throws LoginConciflexException quando as credenciais são rejeitadas
     * @throws com.microsoft.playwright.TimeoutError quando excede o timeout
     * @throws RuntimeException para qualquer outro erro
     */
    public ColetaResultado coletar(Estabelecimento estabelecimento, LocalDate dataInicio, LocalDate dataFim) {
        Cliente cliente = estabelecimento.getCliente();
        String nomeEstabelecimento = estabelecimento.getIdentificadorConciflex();

        log.info("Iniciando coleta para estabelecimento '{}' ({}), período {}/{}",
            nomeEstabelecimento, estabelecimento.getId(), dataInicio, dataFim);

        // Descriptografa credenciais antes de usar — nunca loga os valores
        String loginDecriptado = cryptoService.decrypt(cliente.getConciflex_login());
        String senhaDecriptada  = cryptoService.decrypt(cliente.getConciflex_senha());

        if (loginDecriptado == null || senhaDecriptada == null) {
            LoginConciflexException ex = new LoginConciflexException(nomeEstabelecimento,
                "Falha ao descriptografar credenciais — verifique a chave CRYPTO_SECRET_KEY");
            registrarLogFalha(estabelecimento, "login_failed", ex.getMessage());
            throw ex;
        }

        try {
            // Autenticação via Playwright — abre browser, navega pelo fluxo de login
            ConcifixSession session = playwrightSessionService.autenticar(
                loginDecriptado, senhaDecriptada, nomeEstabelecimento);

            RestTemplate restTemplate = new RestTemplate();

            // ─── Coleta de Conciliação de Taxas ──────────────────────────────────────
            ConciliacaoTaxaApiResponse taxasResponse = chamarApiConciflex(
                restTemplate, session, "/conciliacao-taxas/buscar",
                dataInicio, dataFim, ConciliacaoTaxaApiResponse.class);

            int registrosTaxas = 0;
            if (taxasResponse.result() != null) {
                for (ConciliacaoTaxaItem item : taxasResponse.result()) {
                    upsertConciliacaoTaxa(item, estabelecimento);
                    registrosTaxas++;
                }
                persistirResumoTaxas(estabelecimento, taxasResponse, dataInicio, dataFim, registrosTaxas);
            }

            // ─── Coleta de Recebimentos ───────────────────────────────────────────────
            RecebimentoApiResponse recebimentosResponse = chamarApiConciflex(
                restTemplate, session, "/recebimentos-operadoras/buscar",
                dataInicio, dataFim, RecebimentoApiResponse.class);

            int registrosRecebimentos = 0;
            if (recebimentosResponse.result() != null) {
                for (RecebimentoItem item : recebimentosResponse.result()) {
                    upsertRecebimento(item, estabelecimento);
                    registrosRecebimentos++;
                }
                persistirResumoRecebimentos(estabelecimento, recebimentosResponse, dataInicio, dataFim, registrosRecebimentos);
            }

            int total = registrosTaxas + registrosRecebimentos;
            log.info("Coleta concluída para '{}': {} taxas + {} recebimentos = {} registros",
                nomeEstabelecimento, registrosTaxas, registrosRecebimentos, total);

            // Registra log de sucesso
            registrarLogSucesso(estabelecimento, total);

            return new ColetaResultado(estabelecimento.getId(), registrosTaxas, registrosRecebimentos);

        } catch (LoginConciflexException e) {
            log.warn("Login falhou para '{}': {}", nomeEstabelecimento, e.getMessage());
            registrarLogFalha(estabelecimento, "login_failed", e.getMessage());
            throw e;

        } catch (com.microsoft.playwright.TimeoutError e) {
            String msg = "Timeout ao coletar dados do estabelecimento '" + nomeEstabelecimento + "'";
            log.error(msg);
            registrarLogFalha(estabelecimento, "timeout", msg);
            throw e;

        } catch (Exception e) {
            String msg = "Erro inesperado ao coletar '" + nomeEstabelecimento + "': " + e.getMessage();
            log.error(msg, e);
            registrarLogFalha(estabelecimento, "error", msg);
            throw e;
        }
    }

    /**
     * Chama um endpoint da API Conciflex usando os cookies de sessão extraídos pelo Playwright.
     * Usa multipart/form-data conforme exigido pela API.
     */
    private <T> T chamarApiConciflex(RestTemplate restTemplate, ConcifixSession session,
                                      String path, LocalDate dataInicio, LocalDate dataFim,
                                      Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Monta o header Cookie com os três cookies de sessão
        // O XSRF-TOKEN precisa ser re-encoded ao ser enviado no cookie header
        headers.set("Cookie",
            "cf_clearance=" + session.cfClearance() + "; " +
            "laravel_session=" + session.laravelSession() + "; " +
            "XSRF-TOKEN=" + URLEncoder.encode(session.xsrfToken(), StandardCharsets.UTF_8));

        // Body multipart com _token CSRF e o período de busca
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("_token",       session.csrfToken());
        body.add("data_inicial", dataInicio.toString()); // "2026-04-09"
        body.add("data_final",   dataFim.toString());     // "2026-04-18"

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<T> response = restTemplate.postForEntity(
            BASE_URL + path, request, responseType);

        return response.getBody();
    }

    @Transactional
    protected void upsertConciliacaoTaxa(ConciliacaoTaxaItem item, Estabelecimento est) {
        conciliacaoTaxaRepository.upsert(
            UUID.randomUUID(),
            est.getId(),
            item.id(),
            item.codigoEmpresa(),
            item.dataVenda(),
            item.adquirente(),
            item.codigoAdquirente(),
            item.bandeira(),
            item.codBandeira(),
            item.modalidade(),
            item.codigoModalidade(),
            item.produto(),
            item.codigoProduto(),
            item.valorBruto(),
            item.valorDesconto(),
            item.percentualTaxa(),
            item.taxaContratada(),
            item.quantidade(),
            item.taxaPraticadaRs(),
            item.taxaPraticadaCadastradaRs(),
            item.taxaContratadaRs(),
            item.totalTaxaNaoContratadaRs(),
            item.perdaRs(),
            item.perda(),
            item.auditada(),
            item.estabelecimento(),
            LocalDateTime.now()
        );
    }

    @Transactional
    protected void upsertRecebimento(RecebimentoItem item, Estabelecimento est) {
        // parcela e totalParcelas: a API retorna Integer, mas a coluna é SMALLINT
        Short parcela      = item.parcela()      != null ? item.parcela().shortValue()      : null;
        Short totalParcelas = item.totalParcelas() != null ? item.totalParcelas().shortValue() : null;

        recebimentoRepository.upsert(
            UUID.randomUUID(),
            est.getId(),
            item.id(),
            item.codTipoLancamento(),
            item.tipoLancamento(),
            item.codTipoPagamento(),
            item.tipoPagamento(),
            item.dataVenda(),
            item.dataPrevisao(),
            item.dataPagamento(),
            item.dataCancelamento(),
            item.adquirente(),
            item.codAdquirente(),
            item.bandeira(),
            item.codBandeira(),
            item.modalidade(),
            item.nsu(),
            item.autorizacao(),
            item.cartao(),
            item.numeroResumoVenda(),
            item.produto(),
            item.meioCaptura(),
            item.valorBruto(),
            item.taxaPercentual(),
            item.valorTaxa(),
            item.tarifaTransacao(),
            item.taxaAntecipacao(),
            item.valorTaxaAntecipacao(),
            item.outrasDespesas(),
            item.valorLiquido(),
            item.valorLiquidoSAntecipacao(),
            parcela != null ? parcela.intValue() : null,
            totalParcelas != null ? totalParcelas.intValue() : null,
            item.possuiTaxaMinima(),
            item.estabelecimento(),
            item.banco(),
            item.agencia(),
            item.contaCorrente(),
            item.statusConciliacao(),
            item.codigoOperadoraAjuste(),
            item.descAjuste(),
            item.classificacaoAjuste(),
            item.autorizador(),
            item.dataProcessamento(),
            item.horaProcessamento(),
            item.nomeArquivo(),
            item.observacoes(),
            LocalDateTime.now()
        );
    }

    private void persistirResumoTaxas(Estabelecimento est, ConciliacaoTaxaApiResponse resp,
                                       LocalDate inicio, LocalDate fim, int totalRegistros) {
        String totalizadoresJson = serializarTotalizadores(resp);
        ResumoColeta resumo = ResumoColeta.builder()
            .estabelecimento(est)
            .tipo("conciliacao_taxas")
            .dataInicio(inicio)
            .dataFim(fim)
            .totalRegistros(totalRegistros)
            .valorBrutoTotal(resp.totalValorBrutoAuditadas())
            .valorLiquidoTotal(resp.totalLiquido())
            .totalTaxas(resp.totalTaxas())
            .totalizadoresJson(totalizadoresJson)
            .coletadoEm(LocalDateTime.now())
            .build();
        resumoColetaRepository.save(resumo);
    }

    private void persistirResumoRecebimentos(Estabelecimento est, RecebimentoApiResponse resp,
                                              LocalDate inicio, LocalDate fim, int totalRegistros) {
        String totalizadoresJson = serializarTotalizadores(resp);
        ResumoColeta resumo = ResumoColeta.builder()
            .estabelecimento(est)
            .tipo("recebimentos")
            .dataInicio(inicio)
            .dataFim(fim)
            .totalRegistros(totalRegistros)
            .valorBrutoTotal(resp.sum() != null ? resp.sum().setScale(4, java.math.RoundingMode.HALF_UP) : null)
            .valorLiquidoTotal(resp.sumValorLiquido())
            .totalTaxas(resp.sumCustoTaxa())
            .totalizadoresJson(totalizadoresJson)
            .coletadoEm(LocalDateTime.now())
            .build();
        resumoColetaRepository.save(resumo);
    }

    private String serializarTotalizadores(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private void registrarLogSucesso(Estabelecimento est, int registros) {
        LogColeta log = LogColeta.builder()
            .estabelecimento(est)
            .executadoEm(LocalDateTime.now())
            .status("success")
            .registrosColetados(registros)
            .build();
        logColetaRepository.save(log);
    }

    private void registrarLogFalha(Estabelecimento est, String status, String mensagem) {
        LogColeta logEntry = LogColeta.builder()
            .estabelecimento(est)
            .executadoEm(LocalDateTime.now())
            .status(status)
            .registrosColetados(0)
            .mensagemErro(mensagem)
            .build();
        logColetaRepository.save(logEntry);
    }
}
