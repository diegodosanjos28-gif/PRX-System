package com.conciliacao.api.repository;

import com.conciliacao.api.entity.Cliente;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Operações da exclusão definitiva de um cliente ("Despachar", no Curral).
 *
 * <h2>Por que um repositório separado</h2>
 * Toda a SQL destrutiva do sistema fica reunida num único arquivo auditável, em vez de
 * espalhada pelos repositórios de cada domínio. Duas das tabelas alvo
 * ({@code resumo_coleta} e {@code conciliacao_taxas_historico}) sequer possuem entidade
 * JPA neste módulo, então as queries são nativas por necessidade.
 *
 * <h2>Escopo obrigatório</h2>
 * <b>Nenhuma query aqui pode existir sem filtro de propriedade.</b> Toda remoção é
 * restrita ao {@code cliente_id} alvo ou aos {@code estabelecimento_id} que pertencem a
 * ele — é o que impede exclusão cruzada entre clientes.
 *
 * <h2>Listas vazias</h2>
 * Os métodos que recebem {@code estabelecimentoIds} usam {@code IN (:ids)}, que é SQL
 * inválido com lista vazia. O service só os chama quando há ao menos um estabelecimento;
 * a exclusão por cliente tem método próprio, sem {@code IN}.
 *
 * <h2>Ordem</h2>
 * As FKs dos filhos de {@code estabelecimentos} são {@code NO ACTION} de propósito —
 * funcionam como rede de proteção contra exclusão acidental. Por isso a remoção precisa
 * ser explícita e de baixo para cima; a ordem é responsabilidade do service.
 */
public interface ClienteExclusaoRepository extends Repository<Cliente, UUID> {

    /** Etapa da implantação do cliente, quando existir. Usada para exigir {@code 'curral'}. */
    @Query(value = """
        SELECT etapa FROM implantacoes_cliente WHERE cliente_id = :clienteId
        """, nativeQuery = true)
    Optional<String> findEtapaImplantacao(@Param("clienteId") UUID clienteId);

    /** IDs dos estabelecimentos do cliente — base de escopo de todas as remoções abaixo. */
    @Query(value = """
        SELECT id FROM estabelecimentos WHERE cliente_id = :clienteId
        """, nativeQuery = true)
    List<UUID> findEstabelecimentoIds(@Param("clienteId") UUID clienteId);

    // ── Passo 1 — mensagens ──────────────────────────────────────────────────
    // mensagens_enviadas é o único filho com DUAS FKs para a árvore do cliente
    // (cliente_id e estabelecimento_id). Remover por apenas uma delas deixaria linhas
    // presas pela outra, bloqueando o passo 8. Daí os dois métodos.

    @Modifying
    @Query(value = """
        DELETE FROM mensagens_enviadas WHERE cliente_id = :clienteId
        """, nativeQuery = true)
    int deleteMensagensPorCliente(@Param("clienteId") UUID clienteId);

    @Modifying
    @Query(value = """
        DELETE FROM mensagens_enviadas WHERE estabelecimento_id IN (:estabelecimentoIds)
        """, nativeQuery = true)
    int deleteMensagensPorEstabelecimentos(@Param("estabelecimentoIds") List<UUID> estabelecimentoIds);

    // ── Passo 2 — histórico de conciliação (tabela criada pela V25) ──────────

    @Modifying
    @Query(value = """
        DELETE FROM conciliacao_taxas_historico WHERE estabelecimento_id IN (:estabelecimentoIds)
        """, nativeQuery = true)
    int deleteConciliacaoTaxasHistorico(@Param("estabelecimentoIds") List<UUID> estabelecimentoIds);

    // ── Passos 3 a 6 — dados operacionais dos estabelecimentos ───────────────

    @Modifying
    @Query(value = """
        DELETE FROM conciliacao_taxas WHERE estabelecimento_id IN (:estabelecimentoIds)
        """, nativeQuery = true)
    int deleteConciliacaoTaxas(@Param("estabelecimentoIds") List<UUID> estabelecimentoIds);

    @Modifying
    @Query(value = """
        DELETE FROM recebimentos WHERE estabelecimento_id IN (:estabelecimentoIds)
        """, nativeQuery = true)
    int deleteRecebimentos(@Param("estabelecimentoIds") List<UUID> estabelecimentoIds);

    @Modifying
    @Query(value = """
        DELETE FROM logs_coleta WHERE estabelecimento_id IN (:estabelecimentoIds)
        """, nativeQuery = true)
    int deleteLogsColeta(@Param("estabelecimentoIds") List<UUID> estabelecimentoIds);

    @Modifying
    @Query(value = """
        DELETE FROM resumo_coleta WHERE estabelecimento_id IN (:estabelecimentoIds)
        """, nativeQuery = true)
    int deleteResumoColeta(@Param("estabelecimentoIds") List<UUID> estabelecimentoIds);

    // ── Passo 7 — implantação ────────────────────────────────────────────────
    // implantacao_demandas sai por ON DELETE CASCADE da FK implantacao_id (V20).
    // É a cascata que o projeto já possui; não foi criada nem alterada aqui.

    @Modifying
    @Query(value = """
        DELETE FROM implantacoes_cliente WHERE cliente_id = :clienteId
        """, nativeQuery = true)
    int deleteImplantacao(@Param("clienteId") UUID clienteId);

    // ── Passos 8 e 9 — estabelecimentos e cliente ────────────────────────────

    @Modifying
    @Query(value = """
        DELETE FROM estabelecimentos WHERE cliente_id = :clienteId
        """, nativeQuery = true)
    int deleteEstabelecimentos(@Param("clienteId") UUID clienteId);

    @Modifying
    @Query(value = """
        DELETE FROM clientes WHERE id = :clienteId
        """, nativeQuery = true)
    int deleteCliente(@Param("clienteId") UUID clienteId);
}
