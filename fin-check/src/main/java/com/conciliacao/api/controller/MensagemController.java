package com.conciliacao.api.controller;

import com.conciliacao.api.dto.request.MensagemEnviarRequest;
import com.conciliacao.api.dto.request.MensagemGerarRequest;
import com.conciliacao.api.dto.response.AuditoriaResumoResponse;
import com.conciliacao.api.dto.response.MensagemResponse;
import com.conciliacao.api.dto.response.RecebimentoResumoResponse;
import com.conciliacao.api.service.AuditoriaService;
import com.conciliacao.api.service.MensagemService;
import com.conciliacao.api.service.RecebimentoService;
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
    private final AuditoriaService auditoriaService;
    private final RecebimentoService recebimentoService;

    @PostMapping("/gerar")
    public ResponseEntity<Map<String, Object>> gerar(@Valid @RequestBody MensagemGerarRequest request) {
        String texto = mensagemService.gerar(request);

        AuditoriaResumoResponse resumoAuditoria = auditoriaService.resumo(request.estabelecimentoId(), request.dataInicio(), request.dataFim());
        RecebimentoResumoResponse resumoRecebimento = recebimentoService.resumo(request.estabelecimentoId(), request.dataInicio(), request.dataFim());

        return ResponseEntity.ok(Map.of("mensagem", texto, "resumoAuditoria", resumoAuditoria, "resumoRecebimento", resumoRecebimento));
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
