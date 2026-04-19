package com.conciliacao.api.service;

import com.conciliacao.api.dto.request.MensagemEnviarRequest;
import com.conciliacao.api.dto.request.MensagemGerarRequest;
import com.conciliacao.api.dto.response.AuditoriaResumoResponse;
import com.conciliacao.api.dto.response.MensagemResponse;
import com.conciliacao.api.dto.response.RecebimentoResumoResponse;
import com.conciliacao.api.entity.Cliente;
import com.conciliacao.api.entity.Estabelecimento;
import com.conciliacao.api.entity.MensagemEnviada;
import com.conciliacao.api.repository.MensagemEnviadaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    private final MensagemEnviadaRepository mensagemEnviadaRepository;

    @Value("${mensagem.template}")
    private String templateFixo;

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
        String nome = cliente.getNomeFantasia() != null ? cliente.getNomeFantasia() : cliente.getRazaoSocial();
        return templateFixo
            .replace("{nomeFantasia}", nome)
            .replace("{dataInicio}", req.dataInicio().format(FMT))
            .replace("{dataFim}", req.dataFim().format(FMT))
            .replace("{estabelecimento}", est.getDescricao())
            .replace("{totalTransacoes}", String.valueOf(auditoria.totalTransacoes()))
            .replace("{cobradoAMais}", formatarValor(auditoria.totalCobradoAMais()))
            .replace("{cobradoAMenos}", formatarValor(auditoria.totalCobradoAMenos()))
            .replace("{totalRecebido}", formatarValor(recebimentos.totalRecebido()))
            .replace("{totalDescontado}", formatarValor(recebimentos.totalDescontado()));
    }

    @Transactional
    public MensagemResponse enviar(MensagemEnviarRequest request) {
        Cliente cliente = clienteService.buscarEntidade(request.clienteId());

        // Envia via WhatsApp e persiste no histórico
        MensagemEnviada mensagem = whatsAppService.enviar(cliente, request.conteudo(), "template");

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
        return new MensagemResponse(
            m.getId(), m.getCliente().getId(), m.getConteudo(),
            m.getModoGeracao(), m.getMetaMessageId(), m.getStatusEntrega(), m.getEnviadoEm()
        );
    }
}
