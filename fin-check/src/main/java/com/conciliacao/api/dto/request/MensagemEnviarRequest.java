package com.conciliacao.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Corpo da requisição {@code POST /api/mensagens/enviar}.
 *
 * <h3>Fluxo de {@code templateParametros}</h3>
 * <p>Este campo deve ser preenchido com o mapa retornado pelo {@code /gerar} e
 * armazenado pelo frontend em {@code ResultadoItem.templateParametros}.
 * Ele é repassado ao {@link com.conciliacao.api.service.WhatsAppService} para
 * construir o array posicional exigido pela Meta Cloud API ({@code {{1}}}, {@code {{2}}}, ...).
 *
 * <p>Ao ser {@code null} (modo IA ou template sem {@code metaId}), o WhatsApp envia
 * a mensagem como texto livre.
 *
 * @param clienteId          ID do cliente destinatário.
 * @param conteudo           Texto revisado pelo operador.
 * @param estabelecimentoId  ID do estabelecimento (nullable — associação para histórico).
 * @param templateId         ID do template usado (nullable — preservado no histórico).
 * @param templateNome       Nome do template (denormalizado para evitar JOIN no histórico).
 * @param modoGeracao        {@code "ia"} ou {@code "template"}; default {@code "template"}.
 * @param templateParametros Mapa ordenado {@code chave → valor} vindo do {@code /gerar}.
 *                           Nullable — ausente em modo IA ou texto livre.
 */
public record MensagemEnviarRequest(
    @NotNull  UUID                clienteId,
    @NotBlank String              conteudo,
    UUID                          estabelecimentoId,
    Long                          templateId,
    String                        templateNome,
    String                        modoGeracao,
    Map<String, String>           templateParametros
) {}
