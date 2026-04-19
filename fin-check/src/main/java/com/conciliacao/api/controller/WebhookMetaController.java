package com.conciliacao.api.controller;

import com.conciliacao.api.service.WhatsAppService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

@RestController
@RequestMapping("/api/webhook/meta")
@RequiredArgsConstructor
@Slf4j
public class WebhookMetaController {

    private final WhatsAppService whatsAppService;

    @Value("${meta.webhook-secret}")
    private String webhookSecret;

    // Verificação do webhook pela Meta (challenge)
    @GetMapping
    public ResponseEntity<String> verificar(
        @RequestParam("hub.mode") String mode,
        @RequestParam("hub.challenge") String challenge,
        @RequestParam("hub.verify_token") String verifyToken
    ) {
        if ("subscribe".equals(mode) && webhookSecret.equals(verifyToken)) {
            log.info("Webhook Meta verificado com sucesso");
            return ResponseEntity.ok(challenge);
        }
        log.warn("Verificação do webhook Meta falhou");
        return ResponseEntity.status(403).build();
    }

    // Recebe callbacks de status de entrega
    @PostMapping
    public ResponseEntity<Void> receber(
        @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
        HttpServletRequest httpRequest
    ) {
        try {
            byte[] bodyBytes = StreamUtils.copyToByteArray(httpRequest.getInputStream());
            String bodyStr = new String(bodyBytes, StandardCharsets.UTF_8);

            // Valida assinatura HMAC-SHA256 para garantir que veio da Meta
            if (!validarAssinatura(bodyBytes, signature)) {
                log.warn("Assinatura inválida no webhook Meta");
                return ResponseEntity.status(401).build();
            }

            processarPayload(bodyStr);
        } catch (Exception e) {
            log.error("Erro ao processar webhook Meta: {}", e.getMessage(), e);
        }
        // A Meta espera sempre 200 para confirmar recebimento
        return ResponseEntity.ok().build();
    }

    private boolean validarAssinatura(byte[] body, String signature) {
        if (signature == null || !signature.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computedHash = mac.doFinal(body);
            String computedHex = "sha256=" + HexFormat.of().formatHex(computedHash);
            return computedHex.equals(signature);
        } catch (Exception e) {
            log.error("Erro ao validar assinatura webhook: {}", e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void processarPayload(String body) {
        // Processa atualizações de status de mensagens enviadas
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> payload = mapper.readValue(body, Map.class);

            var entries = (java.util.List<Map<String, Object>>) payload.get("entry");
            if (entries == null) return;

            for (Map<String, Object> entry : entries) {
                var changes = (java.util.List<Map<String, Object>>) entry.get("changes");
                if (changes == null) continue;

                for (Map<String, Object> change : changes) {
                    var value = (Map<String, Object>) change.get("value");
                    if (value == null) continue;

                    var statuses = (java.util.List<Map<String, Object>>) value.get("statuses");
                    if (statuses == null) continue;

                    for (Map<String, Object> status : statuses) {
                        String msgId = (String) status.get("id");
                        String msgStatus = (String) status.get("status");
                        if (msgId != null && msgStatus != null) {
                            whatsAppService.atualizarStatus(msgId, msgStatus);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Erro ao parsear payload do webhook: {}", e.getMessage());
        }
    }
}
