package com.conciliacao.api.controller;

import com.conciliacao.api.dto.response.ExperienciaClienteResponse;
import com.conciliacao.api.service.ExperienciaClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@RestController
@RequestMapping("/api/experiencia-cliente")
@RequiredArgsConstructor
public class ExperienciaClienteController {

    private final ExperienciaClienteService experienciaClienteService;

    @GetMapping("/{estabelecimentoId}")
    public ResponseEntity<ExperienciaClienteResponse> calcular(
            @PathVariable UUID estabelecimentoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicioComparacao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFimComparacao,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mesComparacao,
            @RequestParam(required = false) Integer anoComparacao
    ) {
        LocalDate inicio = resolverInicio(dataInicio, mes, ano);
        LocalDate fim    = resolverFim(dataFim, mes, ano);
        LocalDate inicioComp = resolverInicio(dataInicioComparacao, mesComparacao, anoComparacao);
        LocalDate fimComp    = resolverFim(dataFimComparacao, mesComparacao, anoComparacao);

        return ResponseEntity.ok(
                experienciaClienteService.calcular(estabelecimentoId, inicio, fim, inicioComp, fimComp));
    }

    private LocalDate resolverInicio(LocalDate data, Integer mes, Integer ano) {
        if (data != null) return data;
        if (mes != null && ano != null) return YearMonth.of(ano, mes).atDay(1);
        throw new IllegalArgumentException(
                "Informe dataInicio/dataFim ou mes/ano para o período principal, e dataInicioComparacao/dataFimComparacao ou mesComparacao/anoComparacao para o período de comparação");
    }

    private LocalDate resolverFim(LocalDate data, Integer mes, Integer ano) {
        if (data != null) return data;
        if (mes != null && ano != null) return YearMonth.of(ano, mes).atEndOfMonth();
        throw new IllegalArgumentException(
                "Informe dataInicio/dataFim ou mes/ano para o período principal, e dataInicioComparacao/dataFimComparacao ou mesComparacao/anoComparacao para o período de comparação");
    }
}
