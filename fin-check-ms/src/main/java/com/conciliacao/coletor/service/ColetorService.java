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

import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Orchestrates the full collection cycle for a single establishment.
 * Responsible for: authentication, HTTP calls to Conciflex APIs, UPSERT persistence, and audit logging.
 *
 * Flow:
 * 1. Decrypt client credentials
 * 2. Open Playwright session and navigate the Conciflex login flow
 * 3. Extract session cookies and CSRF token
 * 4. POST /conciliacao-taxas/buscar and persist with UPSERT
 * 5. POST /recebimentos-operadoras/buscar and persist with UPSERT
 * 6. Persist totalizadores to resumo_coleta
 * 7. Write a logs_coleta entry with status 'success'
 *
 * On failure: writes a logs_coleta entry with the appropriate status and re-throws.
 *
 * @throws LoginConciflexException when credentials are rejected
 * @throws com.microsoft.playwright.TimeoutError when the configured timeout is exceeded
 * @throws RuntimeException for any other unexpected failure
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ColetorService {

    private static final String BASE_URL = "https://login.conciflex.com.br";

    // API endpoint paths
    private static final String PATH_CONCILIACAO_TAXAS = "/conciliacao-taxas/buscar";
    private static final String PATH_RECEBIMENTOS      = "/recebimentos-operadoras/buscar";

    // resumo_coleta.tipo values
    private static final String TIPO_CONCILIACAO_TAXAS = "conciliacao_taxas";
    private static final String TIPO_RECEBIMENTOS      = "recebimentos";

    // logs_coleta.status values
    static final String STATUS_SUCCESS      = "success";
    static final String STATUS_LOGIN_FAILED = "login_failed";
    static final String STATUS_TIMEOUT      = "timeout";
    static final String STATUS_ERROR        = "error";

    private final CryptoService             cryptoService;
    private final PlaywrightSessionService  playwrightSessionService;
    private final ConciliacaoTaxaRepository conciliacaoTaxaRepository;
    private final RecebimentoRepository     recebimentoRepository;
    private final ResumoColetaRepository    resumoColetaRepository;
    private final LogColetaRepository       logColetaRepository;
    private final ObjectMapper              objectMapper;

    public ColetaResultado coletar(Estabelecimento estabelecimento, LocalDate dataInicio, LocalDate dataFim) {
        Cliente cliente = estabelecimento.getCliente();
        String nomeEstabelecimento = estabelecimento.getIdentificadorConciflex();

        log.info("Starting collection for '{}' ({}), period {}/{}",
            nomeEstabelecimento, estabelecimento.getId(), dataInicio, dataFim);

        String loginDecriptado = cryptoService.decrypt(cliente.getConciflex_login());
        String senhaDecriptada  = cryptoService.decrypt(cliente.getConciflex_senha());

        if (loginDecriptado == null || senhaDecriptada == null) {
            LoginConciflexException ex = new LoginConciflexException(nomeEstabelecimento,
                "Failed to decrypt credentials — verify CRYPTO_SECRET_KEY");
            registrarLogFalha(estabelecimento, STATUS_LOGIN_FAILED, ex.getMessage());
            throw ex;
        }

        try {
            ConcifixSession session = playwrightSessionService.autenticar(
                loginDecriptado, senhaDecriptada, nomeEstabelecimento);

            RestTemplate restTemplate = new RestTemplate();

            int registrosTaxas = coletarTaxas(restTemplate, session, estabelecimento, dataInicio, dataFim);
            int registrosRecebimentos = coletarRecebimentos(restTemplate, session, estabelecimento, dataInicio, dataFim);

            int total = registrosTaxas + registrosRecebimentos;
            log.info("Collection complete for '{}': {} taxas + {} recebimentos = {} records",
                nomeEstabelecimento, registrosTaxas, registrosRecebimentos, total);

            registrarLogSucesso(estabelecimento, total);
            return new ColetaResultado(estabelecimento.getId(), registrosTaxas, registrosRecebimentos);

        } catch (LoginConciflexException e) {
            log.warn("Login failed for '{}': {}", nomeEstabelecimento, e.getMessage());
            registrarLogFalha(estabelecimento, STATUS_LOGIN_FAILED, e.getMessage());
            throw e;

        } catch (com.microsoft.playwright.TimeoutError e) {
            String msg = "Timeout collecting data for establishment '" + nomeEstabelecimento + "'";
            log.error(msg);
            registrarLogFalha(estabelecimento, STATUS_TIMEOUT, msg);
            throw e;

        } catch (Exception e) {
            String msg = "Unexpected error collecting '" + nomeEstabelecimento + "': " + e.getMessage();
            log.error(msg, e);
            registrarLogFalha(estabelecimento, STATUS_ERROR, msg);
            throw e;
        }
    }

    /** Fetches conciliacao-taxas, persists all items, and saves a summary; returns the item count. */
    private int coletarTaxas(RestTemplate restTemplate, ConcifixSession session,
                              Estabelecimento estabelecimento, LocalDate dataInicio, LocalDate dataFim) {
        ConciliacaoTaxaApiResponse resp = chamarApiConciflex(
            restTemplate, session, PATH_CONCILIACAO_TAXAS,
            dataInicio, dataFim, ConciliacaoTaxaApiResponse.class);

        if (resp.result() == null) return 0;

        int count = 0;
        for (ConciliacaoTaxaItem item : resp.result()) {
            upsertConciliacaoTaxa(item, estabelecimento);
            count++;
        }
        persistirResumoTaxas(estabelecimento, resp, dataInicio, dataFim, count);
        return count;
    }

    /** Fetches recebimentos-operadoras, persists all items, and saves a summary; returns the item count. */
    private int coletarRecebimentos(RestTemplate restTemplate, ConcifixSession session,
                                     Estabelecimento estabelecimento, LocalDate dataInicio, LocalDate dataFim) {
        RecebimentoApiResponse resp = chamarApiConciflex(
            restTemplate, session, PATH_RECEBIMENTOS,
            dataInicio, dataFim, RecebimentoApiResponse.class);

        if (resp.result() == null) return 0;

        int count = 0;
        for (RecebimentoItem item : resp.result()) {
            upsertRecebimento(item, estabelecimento);
            count++;
        }
        persistirResumoRecebimentos(estabelecimento, resp, dataInicio, dataFim, count);
        return count;
    }

    /**
     * Calls a Conciflex API endpoint using the session cookies extracted by Playwright.
     * Uses multipart/form-data as required by the API.
     * The XSRF-TOKEN must be URL-encoded when sent as a cookie header value.
     */
    private <T> T chamarApiConciflex(RestTemplate restTemplate, ConcifixSession session,
                                      String path, LocalDate dataInicio, LocalDate dataFim,
                                      Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Cookie",
            "cf_clearance=" + session.cfClearance() + "; " +
            "laravel_session=" + session.laravelSession() + "; " +
            "XSRF-TOKEN=" + URLEncoder.encode(session.xsrfToken(), StandardCharsets.UTF_8));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("_token",       session.csrfToken());
        body.add("data_inicial", dataInicio.toString());
        body.add("data_final",   dataFim.toString());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<T> response = restTemplate.postForEntity(BASE_URL + path, request, responseType);
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
        // parcela and totalParcelas: API returns Integer but the column is SMALLINT — convert for type safety
        Short parcela       = item.parcela()       != null ? item.parcela().shortValue()       : null;
        Short totalParcelas = item.totalParcelas()  != null ? item.totalParcelas().shortValue()  : null;

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
            parcela       != null ? parcela.intValue()       : null,
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
        ResumoColeta resumo = ResumoColeta.builder()
            .estabelecimento(est)
            .tipo(TIPO_CONCILIACAO_TAXAS)
            .dataInicio(inicio)
            .dataFim(fim)
            .totalRegistros(totalRegistros)
            .valorBrutoTotal(resp.totalValorBrutoAuditadas())
            .valorLiquidoTotal(resp.totalLiquido())
            .totalTaxas(resp.totalTaxas())
            .totalizadoresJson(serializarTotalizadores(resp))
            .coletadoEm(LocalDateTime.now())
            .build();
        resumoColetaRepository.save(resumo);
    }

    private void persistirResumoRecebimentos(Estabelecimento est, RecebimentoApiResponse resp,
                                              LocalDate inicio, LocalDate fim, int totalRegistros) {
        ResumoColeta resumo = ResumoColeta.builder()
            .estabelecimento(est)
            .tipo(TIPO_RECEBIMENTOS)
            .dataInicio(inicio)
            .dataFim(fim)
            .totalRegistros(totalRegistros)
            .valorBrutoTotal(resp.sum() != null ? resp.sum().setScale(4, RoundingMode.HALF_UP) : null)
            .valorLiquidoTotal(resp.sumValorLiquido())
            .totalTaxas(resp.sumCustoTaxa())
            .totalizadoresJson(serializarTotalizadores(resp))
            .coletadoEm(LocalDateTime.now())
            .build();
        resumoColetaRepository.save(resumo);
    }

    /** Serializes an object to JSON; logs a warning and returns an empty object string on failure. */
    private String serializarTotalizadores(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize totalizadores for {}: {}", obj.getClass().getSimpleName(), e.getMessage());
            return "{}";
        }
    }

    private void registrarLogSucesso(Estabelecimento est, int registros) {
        logColetaRepository.save(LogColeta.builder()
            .estabelecimento(est)
            .executadoEm(LocalDateTime.now())
            .status(STATUS_SUCCESS)
            .registrosColetados(registros)
            .build());
    }

    private void registrarLogFalha(Estabelecimento est, String status, String mensagem) {
        logColetaRepository.save(LogColeta.builder()
            .estabelecimento(est)
            .executadoEm(LocalDateTime.now())
            .status(status)
            .registrosColetados(0)
            .mensagemErro(mensagem)
            .build());
    }
}
