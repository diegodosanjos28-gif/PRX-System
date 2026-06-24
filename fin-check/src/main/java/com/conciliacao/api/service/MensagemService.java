package com.conciliacao.api.service;

import com.conciliacao.api.dto.request.MensagemEnviarRequest;
import com.conciliacao.api.dto.request.MensagemGerarRequest;
import com.conciliacao.api.dto.request.MensagemGerarTodosRequest;
import com.conciliacao.api.dto.response.AuditoriaResumoResponse;
import com.conciliacao.api.dto.response.MensagemBulkResultado;
import com.conciliacao.api.dto.response.MensagemGerarResultado;
import com.conciliacao.api.dto.response.MensagemResponse;
import com.conciliacao.api.dto.response.RecebimentoPorBandeira;
import com.conciliacao.api.dto.response.RecebimentoResponse;
import com.conciliacao.api.dto.response.RecebimentoResumoResponse;
import com.conciliacao.api.entity.Cliente;
import com.conciliacao.api.entity.ConciliacaoTaxa;
import com.conciliacao.api.entity.Estabelecimento;
import com.conciliacao.api.entity.MensagemEnviada;
import com.conciliacao.api.entity.Template;
import com.conciliacao.api.repository.ConciliacaoTaxaRepository;
import com.conciliacao.api.repository.EstabelecimentoRepository;
import com.conciliacao.api.repository.MensagemEnviadaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;

/**
 * Orquestra o ciclo de vida de uma mensagem: geração (via IA ou template) e envio via WhatsApp.
 *
 * <h3>Fluxo principal</h3>
 * <ol>
 *   <li>{@link #gerar} — gera o texto e os parâmetros do template sem persistir nada.</li>
 *   <li>O operador revisa o texto no frontend.</li>
 *   <li>{@link #enviar} — envia via Meta Cloud API e persiste o registro.</li>
 * </ol>
 *
 * <p>Para envios em lote sem revisão, use {@link #gerarParaTodos}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MensagemService {

    private final AuditoriaService auditoriaService;
    private final RecebimentoService recebimentoService;
    private final AnthropicService anthropicService;
    private final WhatsAppService whatsAppService;
    private final ClienteService clienteService;
    private final EstabelecimentoService estabelecimentoService;
    private final TemplateService templateService;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final MensagemEnviadaRepository mensagemEnviadaRepository;
    private final ConciliacaoTaxaRepository conciliacaoTaxaRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Gera a mensagem sem salvar — o operador revisa antes de enviar.
     *
     * <p>Os dados de auditoria e recebimento são calculados uma única vez e
     * retornados junto com o resultado para evitar consultas duplicadas ao banco.
     *
     * @param request dados do cliente, estabelecimento, período e modo de geração.
     * @return resultado com texto renderizado, parâmetros do template e resumos financeiros.
     */
    public MensagemGerarResultado gerar(MensagemGerarRequest request) {
        log.info("Iniciando geração de mensagem: clienteId={}, estabelecimentoId={}, modo={}, templateId={}",
            request.clienteId(), request.estabelecimentoId(), request.modo(), request.templateId());

        Cliente cliente = clienteService.buscarEntidade(request.clienteId());
        Estabelecimento est = estabelecimentoService.buscarEntidade(request.estabelecimentoId());

        AuditoriaResumoResponse auditoria = auditoriaService.resumo(est.getId(), request.dataInicio(), request.dataFim());
        RecebimentoResumoResponse recebimentos = recebimentoService.resumo(est.getId(), request.dataInicio(), request.dataFim());

        log.debug("Resumo financeiro carregado: totalTransacoes={}, cobradoAMais={}, totalRecebido={}",
            auditoria.totalTransacoes(), auditoria.totalCobradoAMais(), recebimentos.totalRecebido());

        MensagemGerarResultado resultado = switch (request.modo()) {
            case "ia"       -> gerarComIA(cliente, est, request, auditoria, recebimentos);
            case "template" -> gerarComTemplate(cliente, est, request, auditoria, recebimentos);
            default -> throw new IllegalArgumentException(
                "Modo inválido: " + request.modo() + ". Use 'ia' ou 'template'"
            );
        };

        log.info("Mensagem gerada: modo={}, templateParametros={}, tamanhoConteudo={}",
            request.modo(),
            resultado.templateParametros() != null ? resultado.templateParametros().keySet() : "null (modo IA)",
            resultado.conteudo().length());

        return resultado;
    }

    /**
     * Gera e envia mensagens para todos os clientes e estabelecimentos ativos sem revisão prévia.
     *
     * <p>Os {@code templateParametros} computados em {@link #gerar} são repassados diretamente ao
     * {@link WhatsAppService} para preenchimento posicional dos parâmetros Meta.
     *
     * @param request configurações do envio em lote.
     * @return resumo com totais de sucesso e falhas.
     */
    @Transactional
    public MensagemBulkResultado gerarParaTodos(MensagemGerarTodosRequest request) {
        if ("template".equals(request.modo()) && request.templateId() == null) {
            throw new IllegalArgumentException("templateId é obrigatório para o modo 'template'");
        }

        Template template = request.templateId() != null
            ? templateService.buscarEntidade(request.templateId())
            : null;
        String templateNome = template != null ? template.getNome() : null;

        List<Cliente> clientes = clienteService.listarEntidadesAtivas();
        log.info("Envio em lote iniciado: modo={}, templateId={}, totalClientes={}",
            request.modo(), request.templateId(), clientes.size());

        int total = 0, enviados = 0;
        List<String> errosDetalhados = new ArrayList<>();

        for (Cliente cliente : clientes) {
            List<Estabelecimento> ests = estabelecimentoRepository.findByClienteIdAndAtivoTrue(cliente.getId());
            for (Estabelecimento est : ests) {
                total++;
                try {
                    MensagemGerarRequest req = new MensagemGerarRequest(
                        cliente.getId(), est.getId(),
                        request.dataInicio(), request.dataFim(),
                        request.modo(), request.templateId()
                    );
                    MensagemGerarResultado resultado = gerar(req);
                    // templateParametros gerado em gerar() é repassado diretamente para o WhatsApp
                    whatsAppService.enviar(
                        cliente, resultado.conteudo(), request.modo(),
                        est, template, templateNome, resultado.templateParametros(),
                        request.metaAccessToken()
                    );
                    enviados++;
                    log.info("Envio em lote OK: clienteId={}, estId={}", cliente.getId(), est.getId());
                } catch (Exception e) {
                    String nomeCliente = cliente.getNomeFantasia() != null
                        ? cliente.getNomeFantasia() : cliente.getRazaoSocial();
                    String msg = nomeCliente + " / " + est.getDescricao() + ": " + e.getMessage();
                    errosDetalhados.add(msg);
                    log.error("Envio em lote FALHOU: {}", msg);
                }
            }
        }

        log.info("Envio em lote concluído: total={}, enviados={}, erros={}", total, enviados, errosDetalhados.size());
        return new MensagemBulkResultado(total, enviados, errosDetalhados.size(), errosDetalhados);
    }

    /**
     * Envia a mensagem revisada pelo operador via Meta Cloud API e persiste o registro.
     *
     * <p>{@code request.templateParametros()} deve conter o mapa {@code chave → valor}
     * retornado pelo {@code /gerar} e armazenado pelo frontend em {@code ResultadoItem}.
     * Esse mapa é repassado ao {@link WhatsAppService} que o transforma no array posicional
     * exigido pela Meta API ({@code components[body][parameters]}).
     *
     * @param request corpo da requisição com conteúdo revisado e parâmetros do template.
     * @return representação da mensagem persistida.
     */
    @Transactional
    public MensagemResponse enviar(MensagemEnviarRequest request) {
        log.info("Enviando mensagem: clienteId={}, templateId={}, modoGeracao={}, parametrosPresentes={}",
            request.clienteId(), request.templateId(), request.modoGeracao(),
            request.templateParametros() != null && !request.templateParametros().isEmpty());

        Cliente cliente = clienteService.buscarEntidade(request.clienteId());

        Estabelecimento estabelecimento = null;
        if (request.estabelecimentoId() != null) {
            estabelecimento = estabelecimentoService.buscarEntidade(request.estabelecimentoId());
        }

        Template template = null;
        if (request.templateId() != null) {
            template = templateService.buscarEntidade(request.templateId());
        }

        String modo = request.modoGeracao() != null ? request.modoGeracao() : "template";
        String templateNome = request.templateNome() != null
            ? request.templateNome()
            : (template != null ? template.getNome() : null);

        if (request.templateParametros() != null) {
            log.debug("templateParametros recebidos do frontend: {}", request.templateParametros().keySet());
        }

        MensagemEnviada mensagem = whatsAppService.enviar(
            cliente, request.conteudo(), modo, estabelecimento, template, templateNome,
            request.templateParametros(), request.metaAccessToken()
        );

        log.info("Mensagem enviada com sucesso: wamid={}, clienteId={}", mensagem.getMetaMessageId(), cliente.getId());
        return toResponse(mensagem);
    }

    @Transactional(readOnly = true)
    public List<MensagemResponse> historico(UUID clienteId) {
        clienteService.buscarEntidade(clienteId);
        return mensagemEnviadaRepository.findByClienteIdOrderByEnviadoEmDesc(clienteId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<MensagemResponse> mensagensEnviadas(UUID estabelecimentoId, Pageable pageable) {
        estabelecimentoService.buscarEntidade(estabelecimentoId);
        return mensagemEnviadaRepository
            .findByEstabelecimentoIdOrderByEnviadoEmDesc(estabelecimentoId, pageable)
            .map(this::toResponse);
    }

    // ── private: geração ────────────────────────────────────────────────────────

    private MensagemGerarResultado gerarComIA(Cliente cliente, Estabelecimento est,
                               MensagemGerarRequest req,
                               AuditoriaResumoResponse auditoria,
                               RecebimentoResumoResponse recebimentos) {
        log.info("Gerando mensagem via IA (Anthropic): clienteId={}", cliente.getId());
        String prompt = montarPromptAuditoria(cliente, est, req, auditoria, recebimentos);
        String conteudo = anthropicService.gerarMensagem(prompt);
        log.debug("Mensagem IA gerada: {} caracteres", conteudo.length());
        // templateParametros é null no modo IA — sem parâmetros posicionais para a Meta API
        return new MensagemGerarResultado(conteudo, null, auditoria, recebimentos);
    }

    /**
     * Gera mensagem substituindo os placeholders do template com dados reais do cliente/período.
     *
     * <p>O mapa {@code valores} usa as mesmas chaves cadastradas em {@code template_variaveis}
     * ({@code TemplateVariavel.chave}). Esse mapa é retornado como {@code templateParametros} e
     * seguirá o fluxo até {@code WhatsAppService.buildTemplatePayload()}, onde cada entrada é
     * mapeada para o parâmetro posicional correspondente ({@code {{1}}}, {@code {{2}}}, ...) de
     * acordo com o campo {@code ordem} de cada variável.
     */
    private MensagemGerarResultado gerarComTemplate(Cliente cliente, Estabelecimento est,
                                     MensagemGerarRequest req,
                                     AuditoriaResumoResponse auditoria,
                                     RecebimentoResumoResponse recebimentos) {
        if (req.templateId() == null) {
            throw new IllegalArgumentException("templateId é obrigatório para o modo 'template'");
        }

        Template template = templateService.buscarEntidade(req.templateId());
        log.info("Gerando mensagem via template: templateId={}, nome={}", template.getId(), template.getNome());

        String nomeFantasia = cliente.getNomeFantasia();
        String razaoSocial = cliente.getRazaoSocial();

        List<RecebimentoResponse> recs = recebimentos.recebimentos();

        // ── totalizadores por modalidade (computed in-memory from fetched recebimentos) ──
        BigDecimal totalValorBruto = somarValorBruto(recs);
        BigDecimal totalCredito    = somarPorModalidade(recs, "credit");
        BigDecimal totalDebito     = somarPorModalidade(recs, "debit");
        BigDecimal totalVouchers   = somarPorModalidade(recs, "voucher");
        BigDecimal totalPix        = somarPorModalidade(recs, "pix");
        long diasPeriodo = ChronoUnit.DAYS.between(req.dataInicio(), req.dataFim()) + 1;
        BigDecimal mediaVendas = diasPeriodo > 0
            ? totalValorBruto.divide(BigDecimal.valueOf(diasPeriodo), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        BigDecimal totalTaxaPraticadaRS = coalesceZero(
            conciliacaoTaxaRepository.sumTaxaPraticadaRs(est.getId(), req.dataInicio(), req.dataFim())
        );

        // ── operadoras ordenadas por volume total bruto ────────────────────
        List<Map.Entry<String, BigDecimal>> operadorasOrdenadas = recs.stream()
            .filter(r -> r.adquirente() != null)
            .collect(Collectors.groupingBy(
                RecebimentoResponse::adquirente,
                Collectors.reducing(BigDecimal.ZERO,
                    r -> r.valorBruto() != null ? r.valorBruto() : BigDecimal.ZERO,
                    BigDecimal::add)
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
            .toList();

        String bandeiraMaisPassada = recebimentos.porBandeira().stream()
            .max(Comparator.comparingLong(RecebimentoPorBandeira::quantidade))
            .map(RecebimentoPorBandeira::bandeira)
            .map(this::limparNomeOperadora)
            .orElse("-");

        // Chaves devem corresponder exatamente ao campo `chave` cadastrado em template_variaveis.
        // Esses valores serão repassados à Meta API como parâmetros posicionais {{1}}, {{2}}, etc.
        Map<String, String> valores = new LinkedHashMap<>();
        // ── variáveis originais ────────────────────────────────────────────
        valores.put("nomeFantasia",    nomeFantasia);
        valores.put("dataInicio",      req.dataInicio().format(FMT));
        valores.put("dataFim",         req.dataFim().format(FMT));
        valores.put("estabelecimento", est.getDescricao());
        valores.put("totalTransacoes", String.valueOf(auditoria.totalTransacoes()));
        valores.put("cobradoAMais",    formatarValor(auditoria.totalCobradoAMais()));
        valores.put("cobradoAMenos",   formatarValor(auditoria.totalCobradoAMenos()));
        valores.put("totalRecebido",   formatarValor(recebimentos.totalRecebido()));
        valores.put("totalDescontado", formatarValor(recebimentos.totalDescontado()));
        // ── novas variáveis ────────────────────────────────────────────────
        valores.put("cliente",               nomeFantasia != null && !nomeFantasia.isBlank() ? nomeFantasia : razaoSocial);
        valores.put("razaoSocial",           razaoSocial);
        valores.put("telefone",              cliente.getWhatsapp());
        valores.put("data",                  req.dataFim().format(FMT));
        valores.put("templateName",          template.getNome());
        valores.put("templateOperadorasQtd", String.valueOf(operadorasOrdenadas.size()));
        valores.put("totalValorBruto",       formatarValor(totalValorBruto));
        valores.put("totalTaxaPraticadaRS",  formatarValor(totalTaxaPraticadaRS));
        valores.put("liquidoPrevisto",       formatarValor(recebimentos.totalRecebido()));
        valores.put("totalCredito",          formatarValor(totalCredito));
        valores.put("totalDebito",           formatarValor(totalDebito));
        valores.put("totalVouchers",         formatarValor(totalVouchers));
        valores.put("totalPix",              formatarValor(totalPix));
        valores.put("mediaVendas",           formatarValor(mediaVendas));
        for (int i = 0; i < 4; i++) {
            String idx = String.valueOf(i + 1);
            if (i < operadorasOrdenadas.size()) {
                valores.put("operadora" + idx + "Nome",  limparNomeOperadora(operadorasOrdenadas.get(i).getKey()));
                valores.put("operadora" + idx + "Total", formatarValor(operadorasOrdenadas.get(i).getValue()));
            } else {
                valores.put("operadora" + idx + "Nome",  "-");
                valores.put("operadora" + idx + "Total", "0,00");
            }
        }
        valores.put("operadoraMaisUsada",  operadorasOrdenadas.isEmpty() ? "-" : limparNomeOperadora(operadorasOrdenadas.get(0).getKey()));
        valores.put("bandeiraMaisPassada", bandeiraMaisPassada);
        valores.put("totalCreditoVisa",    formatarValor(somarPorModalidadeEBandeira(recs, "credit", "visa")));
        valores.put("totalCreditoMaster",  formatarValor(somarPorModalidadeEBandeira(recs, "credit", "master")));
        valores.put("totalCreditoElo",     formatarValor(somarPorModalidadeEBandeira(recs, "credit", "elo")));
        valores.put("totalDebitoVisa",     formatarValor(somarPorModalidadeEBandeira(recs, "debit", "visa")));
        valores.put("totalDebitoMaster",   formatarValor(somarPorModalidadeEBandeira(recs, "debit", "master")));
        valores.put("totalDebitoElo",      formatarValor(somarPorModalidadeEBandeira(recs, "debit", "elo")));

        // Maior taxa praticada entre todos os estabelecimentos do cliente no período
        List<ConciliacaoTaxa> topTaxa = conciliacaoTaxaRepository.findMaioresTaxasByCliente(
            cliente.getId(), req.dataInicio(), req.dataFim(), PageRequest.of(0, 1));

        String maiorTaxaFmt      = "-";
        String operadoraMaisCara = "-";
        if (!topTaxa.isEmpty()) {
            ConciliacaoTaxa mt = topTaxa.get(0);
            if (mt.getPercentualTaxa() != null) {
                NumberFormat nf = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
                nf.setMinimumFractionDigits(2);
                nf.setMaximumFractionDigits(2);
                maiorTaxaFmt = nf.format(mt.getPercentualTaxa()) + "%";
            }
            String bandeira = mt.getBandeira();
            if (bandeira != null && !bandeira.isBlank()) {
                operadoraMaisCara = limparNomeOperadora(bandeira);
            }
        }
        valores.put("maiorTaxa",         maiorTaxaFmt);
        valores.put("operadoraMaisCara", operadoraMaisCara);

        log.debug("Parâmetros do template calculados: {} chaves", valores.size());

        String conteudo = template.getConteudo();
        for (Map.Entry<String, String> entry : valores.entrySet()) {
            conteudo = conteudo.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        log.info("Template renderizado: {} placeholders disponíveis, {} caracteres no resultado",
            valores.size(), conteudo.length());

        return new MensagemGerarResultado(conteudo, valores, auditoria, recebimentos);
    }

    private String montarPromptAuditoria(Cliente cliente, Estabelecimento est,
                                          MensagemGerarRequest req,
                                          AuditoriaResumoResponse auditoria,
                                          RecebimentoResumoResponse recebimentos) {
        String nome = cliente.getNomeFantasia() != null ? cliente.getNomeFantasia() : cliente.getRazaoSocial();
        return String.format("""
            Você é um assistente especializado em conciliação financeira. Gere uma mensagem profissional e amigável \
            para enviar ao cliente via WhatsApp com os resultados da auditoria de taxas de cartão.

            Cliente: %s
            Estabelecimento: %s
            Período analisado: %s a %s

            Auditoria de Taxas:
            - Total de transações analisadas: %d
            - Valor cobrado a mais (taxa acima da contratada): R$ %s
            - Valor cobrado a menos (taxa abaixo da contratada): R$ %s
            - Detalhamento por bandeira: %s

            Recebimentos no período:
            - Total recebido líquido: R$ %s
            - Total descontado em taxas: R$ %s

            A mensagem deve ser clara, objetiva e no máximo 3 parágrafos. Destaque os pontos mais relevantes.
            """,
            nome,
            est.getDescricao(),
            req.dataInicio().format(FMT),
            req.dataFim().format(FMT),
            auditoria.totalTransacoes(),
            formatarValor(auditoria.totalCobradoAMais()),
            formatarValor(auditoria.totalCobradoAMenos()),
            auditoria.porBandeira().toString(),
            formatarValor(recebimentos.totalRecebido()),
            formatarValor(recebimentos.totalDescontado())
        );
    }

    private String formatarValor(BigDecimal valor) {
        if (valor == null) return "0,00";
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(valor);
    }

    /** Remove sufixo de código " - NNN" e qualquer "%" do nome bruto de operadora/bandeira. */
    private String limparNomeOperadora(String raw) {
        if (raw == null || raw.isBlank()) return "-";
        int sep = raw.indexOf(" - ");
        String nome = sep > 0 ? raw.substring(0, sep) : raw;
        return nome.replace("%", "").trim();
    }

    private static BigDecimal coalesceZero(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private static BigDecimal somarValorBruto(List<RecebimentoResponse> recs) {
        return recs.stream()
            .map(r -> r.valorBruto() != null ? r.valorBruto() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Normalizes accented Portuguese strings for case-insensitive fragment matching. */
    private static String normalizar(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD)
            .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    }

    private static BigDecimal somarPorModalidade(List<RecebimentoResponse> recs, String frag) {
        return recs.stream()
            .filter(r -> normalizar(r.modalidade()).contains(frag))
            .map(r -> r.valorBruto() != null ? r.valorBruto() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal somarPorModalidadeEBandeira(List<RecebimentoResponse> recs,
                                                           String modalidadeFrag, String bandeiraFrag) {
        return recs.stream()
            .filter(r -> normalizar(r.modalidade()).contains(modalidadeFrag)
                      && normalizar(r.bandeira()).contains(bandeiraFrag))
            .map(r -> r.valorBruto() != null ? r.valorBruto() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private MensagemResponse toResponse(MensagemEnviada m) {
        String clienteNome = m.getCliente().getNomeFantasia() != null
            ? m.getCliente().getNomeFantasia()
            : m.getCliente().getRazaoSocial();
        return new MensagemResponse(
            m.getId(),
            m.getCliente().getId(),
            clienteNome,
            m.getConteudo(),
            m.getModoGeracao(),
            m.getMetaMessageId(),
            m.getStatusEntrega(),
            m.getEnviadoEm(),
            m.getEstabelecimento() != null ? m.getEstabelecimento().getId() : null,
            m.getTemplateNome()
        );
    }
}
