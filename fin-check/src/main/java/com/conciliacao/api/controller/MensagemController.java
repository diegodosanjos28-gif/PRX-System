package com.conciliacao.api.controller;

import com.conciliacao.api.dto.request.MensagemEnviarRequest;
import com.conciliacao.api.dto.request.MensagemGerarRequest;
import com.conciliacao.api.dto.request.MensagemGerarTodosRequest;
import com.conciliacao.api.dto.response.MensagemBulkResultado;
import com.conciliacao.api.dto.response.MensagemGerarResultado;
import com.conciliacao.api.dto.response.MensagemResponse;
import com.conciliacao.api.service.MensagemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoints de geração e envio de mensagens WhatsApp.
 *
 * <p>Este controller é intencionalmente fino: toda a lógica de negócio
 * (geração de texto, parâmetros de template, envio via Meta API) está
 * encapsulada em {@link MensagemService}.
 */
@RestController
@RequestMapping("/api/mensagens")
@RequiredArgsConstructor
public class MensagemController {

    private final MensagemService mensagemService;

    /**
     * Gera a mensagem sem enviar — retorna texto renderizado, resumos financeiros e os
     * {@code templateParametros} que o frontend deve armazenar e repassar ao {@code /enviar}.
     */
    @PostMapping("/gerar")
    public ResponseEntity<Map<String, Object>> gerar(@Valid @RequestBody MensagemGerarRequest request) {
        // resumoAuditoria e resumoRecebimento já estão no resultado — calculados uma única vez no service
        MensagemGerarResultado resultado = mensagemService.gerar(request);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mensagem",           resultado.conteudo());
        body.put("resumoAuditoria",    resultado.resumoAuditoria());
        body.put("resumoRecebimento",  resultado.resumoRecebimento());
        body.put("templateParametros", resultado.templateParametros());
        return ResponseEntity.ok(body);
    }

    /** Gera e envia para todos os clientes/estabelecimentos ativos sem revisão individual. */
    @PostMapping("/gerar-todos")
    public ResponseEntity<MensagemBulkResultado> gerarTodos(@Valid @RequestBody MensagemGerarTodosRequest request) {
        return ResponseEntity.ok(mensagemService.gerarParaTodos(request));
    }

    /**
     * Envia a mensagem revisada pelo operador.
     *
     * <p>O campo {@code templateParametros} deve conter o mapa retornado pelo {@code /gerar}
     * para que os parâmetros posicionais sejam corretamente preenchidos na Meta API.
     */
    @PostMapping("/enviar")
    public ResponseEntity<MensagemResponse> enviar(@Valid @RequestBody MensagemEnviarRequest request) {
        return ResponseEntity.ok(mensagemService.enviar(request));
    }

    @GetMapping("/{clienteId}")
    public ResponseEntity<List<MensagemResponse>> historico(@PathVariable UUID clienteId) {
        return ResponseEntity.ok(mensagemService.historico(clienteId));
    }

    @GetMapping("/enviadas")
    public ResponseEntity<Page<MensagemResponse>> enviadas(
            @RequestParam UUID estabelecimentoId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("enviadoEm").descending());
        return ResponseEntity.ok(mensagemService.mensagensEnviadas(estabelecimentoId, pageable));
    }
}
