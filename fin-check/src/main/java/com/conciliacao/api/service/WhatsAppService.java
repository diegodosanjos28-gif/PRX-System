package com.conciliacao.api.service;

import com.conciliacao.api.entity.Cliente;
import com.conciliacao.api.entity.Estabelecimento;
import com.conciliacao.api.entity.MensagemEnviada;
import com.conciliacao.api.entity.Template;
import com.conciliacao.api.exception.IntegrationException;
import com.conciliacao.api.repository.MensagemEnviadaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

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

    @Transactional
    public MensagemEnviada enviar(Cliente cliente, String conteudo, String modoGeracao,
                                   Estabelecimento estabelecimento, Template template,
                                   String templateNome) {
        String wamid = enviarViaApi(cliente.getWhatsapp(), conteudo);

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

    private String enviarViaApi(String numeroDestino, String conteudo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> body = Map.of(
            "messaging_product", "whatsapp",
            "to", numeroDestino,
            "type", "text",
            "text", Map.of("body", conteudo)
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        String url = baseUrl + "/" + phoneNumberId + "/messages";

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new IntegrationException("Meta API retornou resposta vazia");
            }

            var messages = (java.util.List<Map<String, Object>>) responseBody.get("messages");
            if (messages == null || messages.isEmpty()) {
                throw new IntegrationException("Meta API não retornou ID da mensagem");
            }

            String wamid = (String) messages.get(0).get("id");
            log.info("Mensagem WhatsApp enviada para {}, wamid: {}", numeroDestino, wamid);
            return wamid;

        } catch (IntegrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem WhatsApp: {}", e.getMessage(), e);
            throw new IntegrationException("Falha no envio via WhatsApp: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void atualizarStatus(String metaMessageId, String novoStatus) {
        mensagemEnviadaRepository.findByMetaMessageId(metaMessageId).ifPresent(msg -> {
            msg.setStatusEntrega(novoStatus);
            mensagemEnviadaRepository.save(msg);
            log.info("Status da mensagem {} atualizado para: {}", metaMessageId, novoStatus);
        });
    }
}
