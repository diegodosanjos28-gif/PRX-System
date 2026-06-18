package com.conciliacao.api.repository;

import com.conciliacao.api.entity.ImplantacaoDemanda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ImplantacaoDemandaRepository extends JpaRepository<ImplantacaoDemanda, UUID> {

    List<ImplantacaoDemanda> findByImplantacaoId(UUID implantacaoId);

    // Uma única query para calcular count e maior prioridade de todas as implantações
    // evitando N+1 na listagem do Curral.
    // r[0] = implantacaoId, r[1] = count (Long), r[2] = rank da maior prioridade (Long)
    @Query("""
        SELECT d.implantacao.id,
               COUNT(d),
               MAX(CASE d.prioridade
                   WHEN 'critica' THEN 4
                   WHEN 'alta'    THEN 3
                   WHEN 'media'   THEN 2
                   WHEN 'baixa'   THEN 1
                   ELSE 0 END)
        FROM ImplantacaoDemanda d
        WHERE d.concluida = false
        GROUP BY d.implantacao.id
    """)
    List<Object[]> findResumoAbertasPorImplantacao();
}
