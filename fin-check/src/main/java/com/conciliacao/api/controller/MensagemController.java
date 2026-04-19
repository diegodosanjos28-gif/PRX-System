package com.conciliacao.api.controller;

import com.conciliacao.api.dto.request.MensagemEnviarRequest;
import com.conciliacao.api.dto.request.MensagemGerarRequest;
import com.conciliacao.api.dto.response.MensagemResponse;
import com.conciliacao.api.service.MensagemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/mensagens")
@RequiredArgsConstructor
public class MensagemController {

    private final MensagemService mensagemService;

    @PostMapping("/gerar")
    public ResponseEntity<Map<String, String>> gerar(@Valid @RequestBody MensagemGerarRequest request) {
        String texto = mensagemService.gerar(request);
        return ResponseEntity.ok(Map.of("mensagem", texto));
    }

    @PostMapping("/enviar")
    public ResponseEntity<MensagemResponse> enviar(@Valid @RequestBody MensagemEnviarRequest request) {
        return ResponseEntity.ok(mensagemService.enviar(request));
    }

    @GetMapping("/{clienteId}")
    public ResponseEntity<List<MensagemResponse>> historico(@PathVariable UUID clienteId) {
        return ResponseEntity.ok(mensagemService.historico(clienteId));
    }
}
