package com.conciliacao.api.service;

import com.conciliacao.api.dto.request.MensagemEnviarRequest;
import com.conciliacao.api.dto.request.MensagemGerarRequest;
import com.conciliacao.api.dto.request.MensagemGerarTodosRequest;
import com.conciliacao.api.dto.response.AuditoriaResumoResponse;
import com.conciliacao.api.dto.response.MensagemBulkResultado;
import com.conciliacao.api.dto.response.MensagemResponse;
import com.conciliacao.api.dto.response.RecebimentoResumoResponse;
import com.conciliacao.api.entity.Cliente;
import com.conciliacao.api.entity.Estabelecimento;
import com.conciliacao.api.entity.MensagemEnviada;
import com.conciliacao.api.entity.Template;
import com.conciliacao.api.repository.EstabelecimentoRepository;
import com.conciliacao.api.repository.MensagemEnviadaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Gera a mensagem sem salvar — o operador revisa antes de enviar
    public String gerar(MensagemGerarRequest request) {
        Cliente cliente = clienteService.buscarEntidade(request.clienteId());
        Estabelecimento est = estabelecimentoService.buscarEntidade(request.estabelecimentoId());

        AuditoriaResumoResponse auditoria = auditoriaService.resumo(
            est.getId(), request.dataInicio(), request.dataFim()
        );
        RecebimentoResumoResponse recebimentos = recebimentoService.resumo(
            est.getId(), request.dataInicio(), request.dataFim()
        );

        return switch (request.modo()) {
            case "ia"       -> gerarComIA(cliente, est, request, auditoria, recebimentos);
            case "template" -> gerarComTemplate(cliente, est, request, auditoria, recebimentos);
            default -> throw new IllegalArgumentException("Modo inválido: " + request.modo() + ". Use 'ia' ou 'template'");
        };
    }

    // Gera e envia para todos os clientes e estabelecimentos ativos
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
                    String conteudo = gerar(req);
                    whatsAppService.enviar(cliente, conteudo, request.modo(), est, template, templateNome);
                    enviados++;
                    log.info("Mensagem em lote enviada: cliente={}, est={}", cliente.getId(), est.getId());
                } catch (Exception e) {
                    String nomeCliente = cliente.getNomeFantasia() != null
                        ? cliente.getNomeFantasia() : cliente.getRazaoSocial();
                    String msg = nomeCliente + " / " + est.getDescricao() + ": " + e.getMessage();
                    errosDetalhados.add(msg);
                    log.error("Erro no envio em lote: {}", msg);
                }
            }
        }

        return new MensagemBulkResultado(total, enviados, errosDetalhados.size(), errosDetalhados);
    }

    @Transactional
    public MensagemResponse enviar(MensagemEnviarRequest request) {
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

        MensagemEnviada mensagem = whatsAppService.enviar(
            cliente, request.conteudo(), modo, estabelecimento, template, templateNome
        );

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

    private String gerarComIA(Cliente cliente, Estabelecimento est,
                               MensagemGerarRequest req,
                               AuditoriaResumoResponse auditoria,
                               RecebimentoResumoResponse recebimentos) {
        String prompt = montarPromptAuditoria(cliente, est, req, auditoria, recebimentos);
        log.info("Gerando mensagem via IA para cliente {}", cliente.getId());
        return anthropicService.gerarMensagem(prompt);
    }

    private String gerarComTemplate(Cliente cliente, Estabelecimento est,
                                     MensagemGerarRequest req,
                                     AuditoriaResumoResponse auditoria,
                                     RecebimentoResumoResponse recebimentos) {
        if (req.templateId() == null) {
            throw new IllegalArgumentException("templateId é obrigatório para o modo 'template'");
        }

        Template template = templateService.buscarEntidade(req.templateId());
        String nome = cliente.getNomeFantasia() != null ? cliente.getNomeFantasia() : cliente.getRazaoSocial();

        Map<String, String> valores = Map.of(
            "{nomeFantasia}",    nome,
            "{dataInicio}",      req.dataInicio().format(FMT),
            "{dataFim}",         req.dataFim().format(FMT),
            "{estabelecimento}", est.getDescricao(),
            "{totalTransacoes}", String.valueOf(auditoria.totalTransacoes()),
            "{cobradoAMais}",    formatarValor(auditoria.totalCobradoAMais()),
            "{cobradoAMenos}",   formatarValor(auditoria.totalCobradoAMenos()),
            "{totalRecebido}",   formatarValor(recebimentos.totalRecebido()),
            "{totalDescontado}", formatarValor(recebimentos.totalDescontado())
        );

        String conteudo = template.getConteudo();
        for (Map.Entry<String, String> entry : valores.entrySet()) {
            conteudo = conteudo.replace(entry.getKey(), entry.getValue());
        }
        return conteudo;
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
        return String.format("%.2f", valor).replace(".", ",");
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
