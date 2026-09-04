package com.conciliacao.api.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes da migration V25 — deduplicação de conciliacao_taxas pela chave lógica.
 *
 * <p>Roda contra PostgreSQL 16 real. H2 não implementa {@code UNIQUE NULLS NOT
 * DISTINCT} nem CTEs modificadoras de dados, ambos usados pela V25.
 *
 * <p>Estratégia: migrar até V24, semear cenários de duplicidade, aplicar a V25 e
 * verificar o resultado. Cada teste começa com o schema limpo.
 */
@Testcontainers
@DisplayName("V25 — deduplicação de conciliacao_taxas")
class V25DeduplicacaoMigrationTest {

    private static final String VERSAO_ANTERIOR = "24";
    private static final String VERSAO_ALVO     = "25";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("conciliacao_test")
            .withUsername("test")
            .withPassword("test");

    private UUID estabelecimentoId;

    @BeforeEach
    void limparSchema() throws SQLException {
        flyway().clean();
    }

    // ── Cenário 1: banco vazio ───────────────────────────────────────────────

    @Test
    @DisplayName("banco vazio: aplica a V25 e cria a nova constraint")
    void bancoVazio() throws SQLException {
        migrarAte(VERSAO_ALVO);

        assertThat(constraintExiste("uq_ct_chave_logica")).isTrue();
        assertThat(constraintExiste("uq_ct_id_conciflex_estabelecimento")).isFalse();
        assertThat(tabelaExiste("conciliacao_taxas_historico")).isTrue();
        assertThat(contar("SELECT COUNT(*) FROM conciliacao_taxas")).isZero();
        assertThat(contar("SELECT COUNT(*) FROM conciliacao_taxas_historico")).isZero();
    }

    // ── Cenário 2: banco populado com duplicidades ───────────────────────────

    @Test
    @DisplayName("banco populado: mantém apenas a versão mais recente e arquiva o resto")
    void bancoComDuplicidades() throws SQLException {
        migrarAte(VERSAO_ANTERIOR);
        semearEstabelecimento();

        // Mesma chave lógica, três coletas. A de 03/07 é a mais recente.
        inserirTaxa("id-01", data(1), "108", "159", "1", "10", "S",
                    new BigDecimal("100.00"), 10, hora(1));
        inserirTaxa("id-02", data(1), "108", "159", "1", "10", "S",
                    new BigDecimal("150.00"), 15, hora(2));
        inserirTaxa("id-03", data(1), "108", "159", "1", "10", "S",
                    new BigDecimal("175.50"), 18, hora(3));

        // Chave distinta — não deve ser tocada.
        inserirTaxa("id-04", data(2), "108", "160", "2", "20", "N",
                    new BigDecimal("42.00"), 4, hora(1));

        assertThat(contar("SELECT COUNT(*) FROM conciliacao_taxas")).isEqualTo(4);

        migrarAte(VERSAO_ALVO);

        // Operacional: uma linha por chave lógica
        assertThat(contar("SELECT COUNT(*) FROM conciliacao_taxas")).isEqualTo(2);

        // A versão preservada é a mais recente
        assertThat(valor("SELECT valor_bruto FROM conciliacao_taxas WHERE cod_bandeira = '159'"))
            .isEqualByComparingTo("175.50");
        assertThat(texto("SELECT id_conciflex FROM conciliacao_taxas WHERE cod_bandeira = '159'"))
            .isEqualTo("id-03");

        // Histórico: as duas versões substituídas, com o motivo da migration
        assertThat(contar("""
            SELECT COUNT(*) FROM conciliacao_taxas_historico
            WHERE motivo_arquivamento = 'MIGRATION_DEDUPLICATION'
            """)).isEqualTo(2);

        // Conservação de linhas (Validação D)
        assertThat(contar("SELECT COUNT(*) FROM conciliacao_taxas")
                 + contar("SELECT COUNT(*) FROM conciliacao_taxas_historico"))
            .isEqualTo(4);

        // Rastreabilidade preservada
        assertThat(contar("""
            SELECT COUNT(*) FROM conciliacao_taxas_historico
            WHERE id_conciflex IN ('id-01', 'id-02')
            """)).isEqualTo(2);
    }

    @Test
    @DisplayName("auditada S e N da mesma combinação sobrevivem separadamente")
    void auditadaSeparaGrupos() throws SQLException {
        migrarAte(VERSAO_ANTERIOR);
        semearEstabelecimento();

        inserirTaxa("aud-S", data(1), "108", "159", "1", "10", "S",
                    new BigDecimal("500.00"), 50, hora(1));
        inserirTaxa("aud-N", data(1), "108", "159", "1", "10", "N",
                    new BigDecimal("80.00"), 8, hora(1));

        migrarAte(VERSAO_ALVO);

        assertThat(contar("SELECT COUNT(*) FROM conciliacao_taxas")).isEqualTo(2);
        assertThat(contar("SELECT COUNT(*) FROM conciliacao_taxas_historico")).isZero();
        assertThat(valor("SELECT SUM(valor_bruto) FROM conciliacao_taxas"))
            .isEqualByComparingTo("580.00");
    }

    @Test
    @DisplayName("desempate usa coletado_em e trata NULL como a versão mais antiga")
    void desempateComColetadoEmNulo() throws SQLException {
        migrarAte(VERSAO_ANTERIOR);
        semearEstabelecimento();

        inserirTaxa("sem-data", data(1), "108", "159", "1", "10", "S",
                    new BigDecimal("10.00"), 1, null);
        inserirTaxa("com-data", data(1), "108", "159", "1", "10", "S",
                    new BigDecimal("20.00"), 2, hora(1));

        migrarAte(VERSAO_ALVO);

        assertThat(texto("SELECT id_conciflex FROM conciliacao_taxas")).isEqualTo("com-data");
        assertThat(texto("SELECT id_conciflex FROM conciliacao_taxas_historico"))
            .isEqualTo("sem-data");
    }

    @Test
    @DisplayName("a soma do estado vigente é preservada (Validação C)")
    void somaVigentePreservada() throws SQLException {
        migrarAte(VERSAO_ANTERIOR);
        semearEstabelecimento();

        inserirTaxa("a1", data(1), "108", "159", "1", "10", "S",
                    new BigDecimal("100.00"), 10, hora(1));
        inserirTaxa("a2", data(1), "108", "159", "1", "10", "S",
                    new BigDecimal("111.11"), 11, hora(2));
        inserirTaxa("b1", data(2), "126", "167", "3", "30", "N",
                    new BigDecimal("222.22"), 22, hora(1));

        // Soma bruta inflada antes do saneamento
        assertThat(valor("SELECT SUM(valor_bruto) FROM conciliacao_taxas"))
            .isEqualByComparingTo("433.33");

        migrarAte(VERSAO_ALVO);

        // Soma do estado vigente: 111.11 + 222.22
        assertThat(valor("SELECT SUM(valor_bruto) FROM conciliacao_taxas"))
            .isEqualByComparingTo("333.33");
    }

    // ── Cenário 3: nova constraint em vigor ──────────────────────────────────

    @Test
    @DisplayName("a nova constraint rejeita uma segunda linha com a mesma chave lógica")
    void constraintBloqueiaDuplicidade() throws SQLException {
        migrarAte(VERSAO_ALVO);
        semearEstabelecimento();

        inserirTaxa("x1", data(1), "108", "159", "1", "10", "S",
                    new BigDecimal("10.00"), 1, hora(1));

        assertThatThrownBy(() ->
            inserirTaxa("x2", data(1), "108", "159", "1", "10", "S",
                        new BigDecimal("20.00"), 2, hora(2))
        ).isInstanceOf(SQLException.class)
         .hasMessageContaining("uq_ct_chave_logica");
    }

    @Test
    @DisplayName("id_conciflex repetido deixa de ser bloqueado")
    void idConciflexRepetidoEhPermitido() throws SQLException {
        migrarAte(VERSAO_ALVO);
        semearEstabelecimento();

        // Mesmo id_conciflex, chaves lógicas distintas: agora é legítimo.
        inserirTaxa("mesmo-id", data(1), "108", "159", "1", "10", "S",
                    new BigDecimal("10.00"), 1, hora(1));
        inserirTaxa("mesmo-id", data(2), "108", "159", "1", "10", "S",
                    new BigDecimal("20.00"), 2, hora(1));

        assertThat(contar("SELECT COUNT(*) FROM conciliacao_taxas")).isEqualTo(2);
    }

    @Test
    @DisplayName("a tabela histórica aceita múltiplas versões da mesma chave lógica")
    void historicoAceitaMultiplasVersoes() throws SQLException {
        migrarAte(VERSAO_ANTERIOR);
        semearEstabelecimento();

        for (int i = 1; i <= 5; i++) {
            inserirTaxa("v" + i, data(1), "108", "159", "1", "10", "S",
                        new BigDecimal(i + ".00"), i, hora(i));
        }

        migrarAte(VERSAO_ALVO);

        assertThat(contar("SELECT COUNT(*) FROM conciliacao_taxas")).isEqualTo(1);
        assertThat(contar("SELECT COUNT(*) FROM conciliacao_taxas_historico")).isEqualTo(4);
    }

    // ── Infraestrutura ───────────────────────────────────────────────────────

    private Flyway flyway() {
        return flywayAte(null);
    }

    private Flyway flywayAte(String target) {
        var config = Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .locations("classpath:db/migration")
            .cleanDisabled(false);
        if (target != null) {
            config = config.target(target);
        }
        return config.load();
    }

    private void migrarAte(String versao) {
        flywayAte(versao).migrate();
    }

    private Connection conectar() throws SQLException {
        return DriverManager.getConnection(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private void semearEstabelecimento() throws SQLException {
        UUID clienteId = UUID.randomUUID();
        estabelecimentoId = UUID.randomUUID();

        try (Connection c = conectar(); Statement s = c.createStatement()) {
            s.executeUpdate("""
                INSERT INTO clientes (id, razao_social, cnpj, whatsapp, conciflex_login, conciflex_senha)
                VALUES ('%s', 'Cliente Teste', '00000000000191', '11999999999', 'enc', 'enc')
                """.formatted(clienteId));
            s.executeUpdate("""
                INSERT INTO estabelecimentos (id, cliente_id, descricao, identificador_conciflex)
                VALUES ('%s', '%s', 'Estab Teste', 'ESTAB-TESTE')
                """.formatted(estabelecimentoId, clienteId));
        }
    }

    private void inserirTaxa(String idConciflex, LocalDate dataVenda,
                             String codigoAdquirente, String codBandeira,
                             String codigoModalidade, String codigoProduto,
                             String auditada, BigDecimal valorBruto,
                             Integer quantidade, LocalDateTime coletadoEm) throws SQLException {
        String sql = """
            INSERT INTO conciliacao_taxas (
                id, estabelecimento_id, id_conciflex, data_venda,
                adquirente, codigo_adquirente, bandeira, cod_bandeira,
                modalidade, codigo_modalidade, produto, codigo_produto,
                valor_bruto, quantidade, auditada, coletado_em
            ) VALUES (?, ?, ?, ?, 'Adq', ?, 'Band', ?, 'Modal', ?, 'Prod', ?, ?, ?, ?, ?)
            """;
        try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, estabelecimentoId);
            ps.setString(3, idConciflex);
            ps.setObject(4, dataVenda);
            ps.setString(5, codigoAdquirente);
            ps.setString(6, codBandeira);
            ps.setString(7, codigoModalidade);
            ps.setString(8, codigoProduto);
            ps.setBigDecimal(9, valorBruto);
            ps.setObject(10, quantidade);
            ps.setString(11, auditada);
            ps.setTimestamp(12, coletadoEm != null ? Timestamp.valueOf(coletadoEm) : null);
            ps.executeUpdate();
        }
    }

    private long contar(String sql) throws SQLException {
        try (Connection c = conectar(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    private BigDecimal valor(String sql) throws SQLException {
        try (Connection c = conectar(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getBigDecimal(1) : null;
        }
    }

    private String texto(String sql) throws SQLException {
        try (Connection c = conectar(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private boolean constraintExiste(String nome) throws SQLException {
        return contar("SELECT COUNT(*) FROM pg_constraint WHERE conname = '" + nome + "'") > 0;
    }

    private boolean tabelaExiste(String nome) throws SQLException {
        return contar("""
            SELECT COUNT(*) FROM information_schema.tables
            WHERE table_schema = 'public' AND table_name = '%s'
            """.formatted(nome)) > 0;
    }

    private static LocalDate data(int dia) {
        return LocalDate.of(2026, 7, dia);
    }

    private static LocalDateTime hora(int h) {
        return LocalDateTime.of(2026, 7, 3, h, 0);
    }
}
