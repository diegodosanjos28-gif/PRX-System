package com.conciliacao.api.service;

import com.conciliacao.api.entity.Cliente;
import com.conciliacao.api.entity.Estabelecimento;
import com.conciliacao.api.entity.MensagemEnviada;
import com.conciliacao.api.entity.Template;
import com.conciliacao.api.entity.TemplateVariavel;
import com.conciliacao.api.exception.IntegrationException;
import com.conciliacao.api.repository.MensagemEnviadaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Integração com a Meta WhatsApp Cloud API para envio de mensagens e atualização de status.
 *
 * <h3>Dois modos de envio</h3>
 * <ul>
 *   <li><b>Texto livre</b> — usado quando o template não possui {@code metaId} cadastrado.
 *       Envia {@code type: "text"} com o conteúdo renderizado.</li>
 *   <li><b>Template registrado na Meta</b> — usado quando o template possui {@code metaId}.
 *       Envia {@code type: "template"} com parâmetros posicionais ({@code {{1}}}, {@code {{2}}}, ...)
 *       montados a partir do mapa {@code parametros} (ver {@link #buildTemplatePayload}).</li>
 * </ul>
 *
 * <h3>Origem dos parâmetros posicionais</h3>
 * <pre>
 *  MensagemService.gerarComTemplate()
 *    → constrói Map&lt;chave, valor&gt; com dados do cliente/período
 *    → retorna como MensagemGerarResultado.templateParametros
 *
 *  Frontend armazena templateParametros e reenvia em POST /enviar
 *
 *  MensagemService.enviar() recebe via MensagemEnviarRequest.templateParametros
 *    → repassa para WhatsAppService.enviar() → buildTemplatePayload()
 *
 *  buildTemplatePayload()
 *    → ordena template_variaveis por `ordem`
 *    → para cada variavel: parametros.get(chave) → posição {{N}} na Meta API
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppService {

    private final RestTemplate restTemplate;
    private final MensagemEnviadaRepository mensagemEnviadaRepository;

    @Value("${meta.access-token}")
    private String accessToken;

    @Value("${meta.phone-number-id}")
    private String phoneNumberId;

    @Value("${meta.base-url}")
    private String baseUrl;

    /**
     * Envia a mensagem via Meta Cloud API e persiste o registro no banco.
     *
     * @param parametros    mapa {@code chave → valor} dos parâmetros posicionais do template.
     *                      {@code null} para modo IA ou templates sem {@code metaId}.
     * @param tokenOverride token Meta informado pelo operador no momento do envio.
     *                      Quando {@code null} ou em branco usa o token configurado no servidor.
     */
    @Transactional
    public MensagemEnviada enviar(Cliente cliente, String conteudo, String modoGeracao,
                                   Estabelecimento estabelecimento, Template template,
                                   String templateNome, Map<String, String> parametros,
                                   String tokenOverride) {
        String tokenToUse = (tokenOverride != null && !tokenOverride.isBlank()) ? tokenOverride : accessToken;

        log.info("Preparando envio WhatsApp: numero={}, template={}, token={}",
            cliente.getWhatsapp(),
            template != null ? template.getNome() : "nenhum",
            tokenOverride != null && !tokenOverride.isBlank() ? "override do operador" : "configurado no servidor");

        String wamid = enviarViaApi(cliente.getWhatsapp(), conteudo, template, parametros, tokenToUse);

        MensagemEnviada mensagem = MensagemEnviada.builder()
            .cliente(cliente)
            .estabelecimento(estabelecimento)
            .template(template)
            .templateNome(templateNome)
            .conteudo(conteudo)
            .modoGeracao(modoGeracao)
            .metaMessageId(wamid)
            .statusEntrega("sent")
            .build();

        return mensagemEnviadaRepository.save(mensagem);
    }

    private String enviarViaApi(String numeroDestino, String conteudo, Template template,
                                 Map<String, String> parametros, String tokenToUse) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(tokenToUse);

            Map<String, Object> body;
            if (template != null && template.getMetaId() != null && !template.getMetaId().isBlank()) {
                log.info("Enviando como template registrado na Meta: metaId={}", template.getMetaId());
                body = buildTemplatePayload(numeroDestino, template, parametros);
            } else {
                log.info("Enviando como mensagem de texto livre para: {}", numeroDestino);
                body = Map.of(
                    "messaging_product", "whatsapp",
                    "to",   numeroDestino,
                    "type", "text",
                    "text", Map.of("body", conteudo)
                );
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            String url = baseUrl + "/" + phoneNumberId + "/messages";

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new IntegrationException("Meta API retornou resposta vazia");
            }

            var messages = (List<Map<String, Object>>) responseBody.get("messages");
            if (messages == null || messages.isEmpty()) {
                throw new IntegrationException("Meta API não retornou ID da mensagem");
            }

            String wamid = (String) messages.get(0).get("id");
            log.info("Mensagem WhatsApp enviada: numero={}, wamid={}", numeroDestino, wamid);
            return wamid;

        } catch (IntegrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem WhatsApp: {}", e.getMessage(), e);
            throw new IntegrationException("Falha no envio via WhatsApp: " + e.getMessage(), e);
        }
    }

    /**
     * Monta o payload de template registrado para a Meta Cloud API.
     *
     * <p>As variáveis são ordenadas pelo campo {@code ordem} de {@code template_variaveis},
     * estabelecendo a correspondência direta com os parâmetros posicionais do template Meta:
     * variável com {@code ordem=1} → {@code {{1}}}, {@code ordem=2} → {@code {{2}}}, etc.
     *
     * <p>Se {@code parametros} não contiver o valor para uma chave, o parâmetro é enviado como
     * {@code "*"} e um WARN é registrado — o envio prossegue para não bloquear operações em lote.
     *
     * @param parametros mapa {@code chave → valor} vindo de {@code MensagemEnviarRequest.templateParametros}.
     *                   Pode ser {@code null} (ex.: envio manual sem passar pelo /gerar).
     */
    private Map<String, Object> buildTemplatePayload(String numeroDestino, Template template,
                                                      Map<String, String> parametros) {
        List<TemplateVariavel> variaveis = template.getVariaveis().stream()
            .sorted(Comparator.comparingInt(TemplateVariavel::getOrdem))
            .toList();

        log.debug("Construindo payload do template '{}': {} variáveis ordenadas por ordem",
            template.getNome(), variaveis.size());

        Map<String, Object> templateBody = new LinkedHashMap<>();
        templateBody.put("name",     template.getMetaId());
        templateBody.put("language", Map.of("code", "pt_BR"));

        if (!variaveis.isEmpty()) {
            List<String> missing = variaveis.stream()
                .map(TemplateVariavel::getChave)
                .filter(k -> parametros == null || !parametros.containsKey(k))
                .toList();

            if (!missing.isEmpty()) {
                log.warn("Parâmetros ausentes para o template '{}', serão enviados como '*': {}",
                    template.getNome(), missing);
            }

            List<Map<String, String>> params = variaveis.stream()
                .map(v -> {
                    String valor = (parametros != null) ? parametros.getOrDefault(v.getChave(), "*") : "*";
                    log.debug("  {{{}}} (chave='{}', ordem={}) = '{}'",
                        v.getOrdem(), v.getChave(), v.getOrdem(), valor);
                    Map<String, String> param = new LinkedHashMap<>();
                    param.put("type", "text");
                    param.put("text", valor);
                    return param;
                })
                .toList();

            templateBody.put("components", List.of(
                Map.of("type", "body", "parameters", params)
            ));

            log.info("Payload template '{}' montado: {} parâmetros posicionais", template.getNome(), params.size());
        }

        return Map.of(
            "messaging_product", "whatsapp",
            "to",       numeroDestino,
            "type",     "template",
            "template", templateBody
        );
    }

    /**
     * Atualiza o status de entrega de uma mensagem a partir do webhook da Meta.
     *
     * @param metaMessageId wamid retornado pela Meta no envio.
     * @param novoStatus    status recebido no webhook (ex.: "delivered", "read", "failed").
     */
    @Transactional
    public void atualizarStatus(String metaMessageId, String novoStatus) {
        mensagemEnviadaRepository.findByMetaMessageId(metaMessageId).ifPresent(msg -> {
            msg.setStatusEntrega(novoStatus);
            mensagemEnviadaRepository.save(msg);
            log.info("Status da mensagem {} atualizado para: {}", metaMessageId, novoStatus);
        });
    }
}
