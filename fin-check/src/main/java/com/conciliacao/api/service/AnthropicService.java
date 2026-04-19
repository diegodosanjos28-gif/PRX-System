package com.conciliacao.api.service;

import com.conciliacao.api.exception.IntegrationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnthropicService {

    private final RestTemplate restTemplate;

    @Value("${anthropic.api-key}")
    private String apiKey;

    @Value("${anthropic.base-url}")
    private String baseUrl;

    @Value("${anthropic.model}")
    private String model;

    @Value("${anthropic.max-tokens}")
    private int maxTokens;

    public String gerarMensagem(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> body = Map.of(
            "model", model,
            "max_tokens", maxTokens,
            "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/v1/messages", request, Map.class
            );

            // Extrai o texto da resposta da API Anthropic
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
            if (content == null || content.isEmpty()) {
                throw new IntegrationException("Anthropic retornou resposta sem conteúdo");
            }

            return (String) content.get(0).get("text");

        } catch (IntegrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao chamar API Anthropic: {}", e.getMessage(), e);
            throw new IntegrationException("Falha na comunicação com Anthropic: " + e.getMessage(), e);
        }
    }
}
