package com.conciliacao.api.cliente;

import com.conciliacao.api.exception.ConflictException;
import com.conciliacao.api.exception.ResourceNotFoundException;
import com.conciliacao.api.service.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exclusão definitiva de cliente ("Despachar" do Curral).
 *
 * <p>Roda contra PostgreSQL 16 real com o schema completo do Flyway — <b>inclusive a
 * V25</b>, que cria {@code conciliacao_taxas_historico}. Essa tabela não existe em
 * schemas anteriores, e um teste que não a exercite deixaria passar uma implementação
 * que quebra em produção por violação de FK.
 *
 * <p>Cada teste semeia DOIS clientes com a árvore completa de dados. O cliente B existe
 * para provar o que mais importa numa operação destrutiva: que despachar A não encosta
 * em nada de B.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Exclusão definitiva de cliente — Despachar")
class ClienteExclusaoDefinitivaTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("despacho_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired private ClienteService clienteService;
    @Autowired private JdbcTemplate jdbc;

    private UUID clienteA, estabA, implantacaoA;
    private UUID clienteB, estabB;

    @BeforeEach
    void semear() {
        jdbc.execute("""
            TRUNCATE conciliacao_taxas_historico, conciliacao_taxas, recebimentos,
                     logs_coleta, resumo_coleta, mensagens_enviadas,
                     implantacao_demandas, implantacoes_cliente,
                     estabelecimentos, clientes CASCADE
            """);

        clienteA = UUID.randomUUID();
        estabA   = UUID.randomUUID();
        implantacaoA = criarClienteCompleto(clienteA, estabA, "Cliente A", "11111111111111", "curral");

        clienteB = UUID.randomUUID();
        estabB   = UUID.randomUUID();
        criarClienteCompleto(clienteB, estabB, "Cliente B", "22222222222222", "curral");
    }

    // ── Exclusão do alvo ─────────────────────────────────────────────────────

    @Test
    @DisplayName("cliente do Curral é excluído com toda a sua árvore de dados")
    void excluiClienteCompleto() {
        clienteService.excluirDefinitivamente(clienteA);

        assertThat(contar("clientes", "id", clienteA)).isZero();
        assertThat(contar("estabelecimentos", "cliente_id", clienteA)).isZero();
        assertThat(contar("implantacoes_cliente", "cliente_id", clienteA)).isZero();
        assertThat(contarPorEstab("conciliacao_taxas", estabA)).isZero();
        assertThat(contarPorEstab("conciliacao_taxas_historico", estabA)).isZero();
        assertThat(contarPorEstab("recebimentos", estabA)).isZero();
        assertThat(contarPorEstab("logs_coleta", estabA)).isZero();
        assertThat(contarPorEstab("resumo_coleta", estabA)).isZero();
        assertThat(contar("mensagens_enviadas", "cliente_id", clienteA)).isZero();
    }

    @Test
    @DisplayName("demandas da implantação saem junto, pela cascata que já existia")
    void excluiDemandas() {
        assertThat(contarDemandas(implantacaoA)).isEqualTo(2);

        clienteService.excluirDefinitivamente(clienteA);

        assertThat(contarDemandas(implantacaoA)).isZero();
    }

    @Test
    @DisplayName("o histórico de conciliação criado pela V25 é removido")
    void excluiHistoricoV25() {
        assertThat(contarPorEstab("conciliacao_taxas_historico", estabA)).isEqualTo(1);

        clienteService.excluirDefinitivamente(clienteA);

        assertThat(contarPorEstab("conciliacao_taxas_historico", estabA)).isZero();
    }

    // ── Não contaminação ─────────────────────────────────────────────────────

    @Test
    @DisplayName("cliente B permanece integralmente intacto")
    void naoAfetaOutroCliente() {
        clienteService.excluirDefinitivamente(clienteA);

        assertThat(contar("clientes", "id", clienteB)).isEqualTo(1);
        assertThat(contar("estabelecimentos", "cliente_id", clienteB)).isEqualTo(1);
        assertThat(contar("implantacoes_cliente", "cliente_id", clienteB)).isEqualTo(1);
        assertThat(contarPorEstab("conciliacao_taxas", estabB)).isEqualTo(1);
        assertThat(contarPorEstab("conciliacao_taxas_historico", estabB)).isEqualTo(1);
        assertThat(contarPorEstab("recebimentos", estabB)).isEqualTo(1);
        assertThat(contarPorEstab("logs_coleta", estabB)).isEqualTo(1);
        assertThat(contarPorEstab("resumo_coleta", estabB)).isEqualTo(1);
        assertThat(contar("mensagens_enviadas", "cliente_id", clienteB)).isEqualTo(1);
    }

    @Test
    @DisplayName("dados globais — templates, variáveis e usuários — permanecem intactos")
    void naoAfetaDadosGlobais() {
        long templates  = jdbc.queryForObject("SELECT COUNT(*) FROM templates", Long.class);
        long variaveis  = jdbc.queryForObject("SELECT COUNT(*) FROM template_variaveis", Long.class);
        long usuarios   = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);

        clienteService.excluirDefinitivamente(clienteA);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM templates", Long.class)).isEqualTo(templates);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM template_variaveis", Long.class)).isEqualTo(variaveis);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class)).isEqualTo(usuarios);
        assertThat(usuarios).isPositive();
    }

    // ── Recusas ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cliente inexistente é recusado com 404 e nada é removido")
    void clienteInexistente() {
        long antes = jdbc.queryForObject("SELECT COUNT(*) FROM clientes", Long.class);

        assertThatThrownBy(() -> clienteService.excluirDefinitivamente(UUID.randomUUID()))
            .isInstanceOf(ResourceNotFoundException.class);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM clientes", Long.class)).isEqualTo(antes);
    }

    @Test
    @DisplayName("cliente fora do Curral é recusado e NADA é removido")
    void clienteForaDoCurral() {
        jdbc.update("UPDATE implantacoes_cliente SET etapa = 'corrida', status = 'fluindo' "
                  + "WHERE cliente_id = ?", clienteA);

        assertThatThrownBy(() -> clienteService.excluirDefinitivamente(clienteA))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Curral");

        // A árvore inteira de A continua de pé.
        assertThat(contar("clientes", "id", clienteA)).isEqualTo(1);
        assertThat(contar("estabelecimentos", "cliente_id", clienteA)).isEqualTo(1);
        assertThat(contarPorEstab("conciliacao_taxas", estabA)).isEqualTo(1);
        assertThat(contarPorEstab("conciliacao_taxas_historico", estabA)).isEqualTo(1);
        assertThat(contarPorEstab("recebimentos", estabA)).isEqualTo(1);
        assertThat(contarDemandas(implantacaoA)).isEqualTo(2);
    }

    @Test
    @DisplayName("cliente sem implantação é recusado e NADA é removido")
    void clienteSemImplantacao() {
        jdbc.update("DELETE FROM implantacoes_cliente WHERE cliente_id = ?", clienteA);

        assertThatThrownBy(() -> clienteService.excluirDefinitivamente(clienteA))
            .isInstanceOf(ConflictException.class);

        assertThat(contar("clientes", "id", clienteA)).isEqualTo(1);
        assertThat(contarPorEstab("recebimentos", estabA)).isEqualTo(1);
    }

    // ── Atomicidade ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("falha no meio da operação reverte tudo — sem exclusão parcial")
    void rollbackIntegral() {
        // Uma FK extra para o estabelecimento de A, que o service não conhece: o passo 8
        // (DELETE estabelecimentos) viola a constraint e a transação inteira é revertida.
        jdbc.execute("""
            CREATE TABLE teste_bloqueio_fk (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                estabelecimento_id UUID NOT NULL REFERENCES estabelecimentos (id)
            )
            """);
        jdbc.update("INSERT INTO teste_bloqueio_fk (estabelecimento_id) VALUES (?)", estabA);

        try {
            assertThatThrownBy(() -> clienteService.excluirDefinitivamente(clienteA))
                .isInstanceOf(Exception.class);

            // NADA pode ter sido removido — nem as folhas apagadas nos primeiros passos.
            assertThat(contar("clientes", "id", clienteA)).isEqualTo(1);
            assertThat(contar("estabelecimentos", "cliente_id", clienteA)).isEqualTo(1);
            assertThat(contarPorEstab("conciliacao_taxas", estabA)).isEqualTo(1);
            assertThat(contarPorEstab("conciliacao_taxas_historico", estabA)).isEqualTo(1);
            assertThat(contarPorEstab("recebimentos", estabA)).isEqualTo(1);
            assertThat(contarPorEstab("logs_coleta", estabA)).isEqualTo(1);
            assertThat(contarPorEstab("resumo_coleta", estabA)).isEqualTo(1);
            assertThat(contar("mensagens_enviadas", "cliente_id", clienteA)).isEqualTo(1);
            assertThat(contarDemandas(implantacaoA)).isEqualTo(2);
        } finally {
            jdbc.execute("DROP TABLE teste_bloqueio_fk");
        }
    }

    @Test
    @DisplayName("despachar A e depois B esvazia o Curral sem efeito colateral")
    void despachosSucessivos() {
        clienteService.excluirDefinitivamente(clienteA);
        clienteService.excluirDefinitivamente(clienteB);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM clientes", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM estabelecimentos", Long.class)).isZero();
        assertThat(jdbc.queryForObject(
            "SELECT COUNT(*) FROM conciliacao_taxas_historico", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class)).isPositive();
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    /** Cria cliente + estabelecimento + implantação com 2 demandas + 1 linha em cada tabela filha. */
    private UUID criarClienteCompleto(UUID clienteId, UUID estabId, String nome,
                                      String cnpj, String etapa) {
        jdbc.update("""
            INSERT INTO clientes (id, razao_social, nome_fantasia, cnpj, whatsapp,
                                  conciflex_login, conciflex_senha)
            VALUES (?, ?, ?, ?, '11999999999', 'enc', 'enc')
            """, clienteId, nome + " LTDA", nome, cnpj);

        jdbc.update("""
            INSERT INTO estabelecimentos (id, cliente_id, descricao, identificador_conciflex)
            VALUES (?, ?, ?, ?)
            """, estabId, clienteId, nome + " Matriz", "EST-" + cnpj);

        UUID implantacaoId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO implantacoes_cliente (id, cliente_id, etapa, status, adquirentes)
            VALUES (?, ?, ?, ?, '[]'::jsonb)
            """, implantacaoId, clienteId, etapa, "curral".equals(etapa) ? null : "fluindo");

        jdbc.update("""
            INSERT INTO implantacao_demandas (implantacao_id, descricao, concluida, tipo)
            VALUES (?, 'Demanda aberta', false, 'curral')
            """, implantacaoId);
        jdbc.update("""
            INSERT INTO implantacao_demandas (implantacao_id, descricao, concluida, tipo)
            VALUES (?, 'ROCK concluido', true, 'curral')
            """, implantacaoId);

        jdbc.update("""
            INSERT INTO conciliacao_taxas (estabelecimento_id, id_conciflex, data_venda,
                                           codigo_adquirente, cod_bandeira, codigo_modalidade,
                                           codigo_produto, auditada, valor_bruto)
            VALUES (?, ?, DATE '2026-09-01', '108', '1', '0', 'p0', 'S', 100.00)
            """, estabId, "ct-" + cnpj);

        jdbc.update("""
            INSERT INTO conciliacao_taxas_historico (conciliacao_taxa_id_original, estabelecimento_id,
                                                     data_venda, valor_bruto, motivo_arquivamento)
            VALUES (?, ?, DATE '2026-09-01', 90.00, 'UPSERT_VALUE_CHANGE')
            """, UUID.randomUUID(), estabId);

        jdbc.update("""
            INSERT INTO recebimentos (estabelecimento_id, id_conciflex, data_pagamento, valor_bruto)
            VALUES (?, ?, DATE '2026-09-02', 100.00)
            """, estabId, "rc-" + cnpj);

        jdbc.update("""
            INSERT INTO logs_coleta (estabelecimento_id, status) VALUES (?, 'success')
            """, estabId);

        jdbc.update("""
            INSERT INTO resumo_coleta (estabelecimento_id, tipo) VALUES (?, 'recebimentos')
            """, estabId);

        jdbc.update("""
            INSERT INTO mensagens_enviadas (cliente_id, estabelecimento_id, conteudo, modo_geracao)
            VALUES (?, ?, 'mensagem de teste', 'template')
            """, clienteId, estabId);

        return implantacaoId;
    }

    private long contar(String tabela, String coluna, UUID valor) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM " + tabela + " WHERE " + coluna + " = ?", Long.class, valor);
    }

    private long contarPorEstab(String tabela, UUID estabId) {
        return contar(tabela, "estabelecimento_id", estabId);
    }

    private long contarDemandas(UUID implantacaoId) {
        return contar("implantacao_demandas", "implantacao_id", implantacaoId);
    }
}
