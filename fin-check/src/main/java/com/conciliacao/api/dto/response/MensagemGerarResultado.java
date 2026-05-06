package com.conciliacao.api.dto.response;

import java.util.Map;

/**
 * Resultado completo da operação de geração de mensagem ({@code POST /api/mensagens/gerar}).
 *
 * <h3>Fluxo dos parâmetros de template (ponta a ponta)</h3>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │  BACKEND  —  /gerar                                                         │
 * │  MensagemService.gerarComTemplate()                                         │
 * │    1. Busca o Template (metaId, variaveis ordenadas por `ordem`)             │
 * │    2. Constrói Map&lt;chave, valor&gt; com dados reais do cliente/período:         │
 * │         { "nomeFantasia" → "Empresa X", "cobradoAMais" → "1.200,00", ... }  │
 * │    3. Substitui {chave} no conteúdo textual do template                     │
 * │    4. Devolve conteúdo renderizado + templateParametros neste record        │
 * │                                                                              │
 * │  MensagemController.gerar()                                                 │
 * │    Serializa templateParametros no JSON de resposta                         │
 * └─────────────────┬───────────────────────────────────────────────────────────┘
 *                   │  response JSON: { mensagem, resumoAuditoria,
 *                   │                   resumoRecebimento, templateParametros }
 *                   ▼
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │  FRONTEND                                                                    │
 * │  onGerar()  →  armazena templateParametros em ResultadoItem (estado React) │
 * │  onEnviar() →  reenvia templateParametros no corpo de POST /enviar          │
 * └─────────────────┬───────────────────────────────────────────────────────────┘
 *                   │  request JSON: { clienteId, conteudo, templateId,
 *                   │                  templateParametros: { chave → valor } }
 *                   ▼
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │  BACKEND  —  /enviar                                                        │
 * │  WhatsAppService.buildTemplatePayload()                                      │
 * │    1. Carrega template_variaveis ordenadas por `ordem`                      │
 * │    2. Para cada variavel, busca o valor em templateParametros pelo chave    │
 * │    3. Monta o array positional da Meta API:                                 │
 * │         {{1}} = variavel com ordem=1                                        │
 * │         {{2}} = variavel com ordem=2, etc.                                  │
 * │    4. Envia payload: components[body][parameters] → Meta Cloud API          │
 * └─────────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p><b>Nota de performance:</b> {@code resumoAuditoria} e {@code resumoRecebimento} são
 * incluídos aqui porque já foram calculados durante a geração — evita uma segunda
 * consulta ao banco no controller.
 *
 * @param conteudo            Texto renderizado com todos os placeholders substituídos.
 * @param templateParametros  Mapa ordenado {@code chave → valor} para envio posicional
 *                            à Meta API. {@code null} no modo IA.
 * @param resumoAuditoria     Resumo de auditoria do período (calculado uma única vez).
 * @param resumoRecebimento   Resumo de recebimentos do período (calculado uma única vez).
 */
public record MensagemGerarResultado(
    String conteudo,
    Map<String, String> templateParametros,
    AuditoriaResumoResponse resumoAuditoria,
    RecebimentoResumoResponse resumoRecebimento
) {}
