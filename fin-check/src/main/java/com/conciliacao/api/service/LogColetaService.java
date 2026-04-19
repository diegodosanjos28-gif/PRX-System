package com.conciliacao.api.service;

import com.conciliacao.api.entity.LogColeta;
import com.conciliacao.api.repository.LogColetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LogColetaService {

    private final LogColetaRepository logColetaRepository;

    @Transactional(readOnly = true)
    public List<LogColeta> consultar(UUID estabelecimentoId, String status) {
        if (estabelecimentoId != null && status != null) {
            return logColetaRepository.findByEstabelecimentoIdAndStatusOrderByExecutadoEmDesc(estabelecimentoId, status);
        }
        if (estabelecimentoId != null) {
            return logColetaRepository.findByEstabelecimentoIdOrderByExecutadoEmDesc(estabelecimentoId);
        }
        if (status != null) {
            return logColetaRepository.findByStatusOrderByExecutadoEmDesc(status);
        }
        return logColetaRepository.findAll();
    }
}
