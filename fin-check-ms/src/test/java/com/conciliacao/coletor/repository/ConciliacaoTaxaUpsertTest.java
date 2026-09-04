package com.conciliacao.coletor.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes do UPSERT de conciliacao_taxas pela chave lógica, com preservação
 * automática do histórico via CTE transacional.
 *
 * <p>Roda contra PostgreSQL 16 real: H2 não implementa {@code INSERT ... ON
 * CONFLICT}, CTEs modificadoras de dados nem {@code NULLS NOT DISTINCT}.
 *
 * <p>O schema é montado pelas migrations do módulo {@code fin-check}, que é seu
 * dono. O caminho por filesystem reflete a topologia real: em produção o
 * backend principal aplica as migrations e o coletor apenas valida o schema.
 *
 * <p>{@code NOT_SUPPORTED} desliga a transação de teste do Spring para que cada
 * chamada ao repositório confirme em sua própria transação, como em produção.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("UPSERT de conciliacao_taxas pela chave lógica")
class ConciliacaoTaxaUpsertTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("coletor_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled",   () -> "true");
        registry.add("spring.flyway.locations", () -> "filesystem:../fin-check/src/main/resources/db/migration");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired private ConciliacaoTaxaRepository repository;
    @Autowired private JdbcTemplate jdbc;

    private UUID estabelecimentoId;
    private UUID outroEstabelecimentoId;

    private static final LocalDate     DATA_VENDA  = LocalDate.of(2026, 7, 20);
    private static final LocalDateTime COLETA_1    = LocalDateTime.of(2026, 7, 21, 17, 0);
    private static final LocalDateTime COLETA_2    = LocalDateTime.of(2026, 7, 22, 17, 0);

    @BeforeEach
    void prepararBase() {
        jdbc.update("DELETE FROM conciliacao_taxas_historico");
        jdbc.update("DELETE FROM conciliacao_taxas");
        jdbc.update("DELETE FROM estabelecimentos");
        jdbc.update("DELETE FROM clientes");

        UUID clienteId = UUID.randomUUID();
        estabelecimentoId      = UUID.randomUUID();
        outroEstabelecimentoId = UUID.randomUUID();

        jdbc.update("""
            INSERT INTO clientes (id, razao_social, cnpj, whatsapp, conciflex_login, conciflex_senha)
            VALUES (?, 'Cliente Teste', '00000000000191', '11999999999', 'enc', 'enc')
            """, clienteId);
        jdbc.update("""
            INSERT INTO estabelecimentos (id, cliente_id, descricao, identificador_conciflex)
            VALUES (?, ?, 'Estab A', 'ESTAB-A')
            """, estabelecimentoId, clienteId);
        jdbc.update("""
            INSERT INTO estabelecimentos (id, cliente_id, descricao, identificador_conciflex)
            VALUES (?, ?, 'Estab B', 'ESTAB-B')
            """, outroEstabelecimentoId, clienteId);
    }

    // ── Teste 1 — primeira coleta ────────────────────────────────────────────

    @Test
    @DisplayName("registro inexistente é inserido, sem gerar histórico")
    void primeiraColeta() {
        upsert("conc-1", new BigDecimal("100.00"), 10, COLETA_1);

        assertThat(linhasOperacionais()).isEqualTo(1);
        assertThat(linhasHistorico()).isZero();
        assertThat(campoTexto("id_conciflex")).isEqualTo("conc-1");
        assertThat(campoValor("valor_bruto")).isEqualByComparingTo("100.00");
    }

    // ── Teste 2 — recoleta idêntica ──────────────────────────────────────────

    @Test
    @DisplayName("recoleta idêntica com novo id_conciflex atualiza metadados sem criar histórico")
    void recoletaIdentica() {
        upsert("conc-1", new BigDecimal("100.00"), 10, COLETA_1);
        upsert("conc-2", new BigDecimal("100.00"), 10, COLETA_2);

        assertThat(linhasOperacionais()).isEqualTo(1);

        // Nenhuma versão histórica: nada financeiro mudou
        assertThat(linhasHistorico()).isZero();

        // Metadados atualizados
        assertThat(campoTexto("id_conciflex")).isEqualTo("conc-2");
        assertThat(jdbc.queryForObject(
            "SELECT coletado_em FROM conciliacao_taxas", LocalDateTime.class))
            .isEqualTo(COLETA_2);
    }

    @Test
    @DisplayName("dez recoletas idênticas mantêm uma linha e histórico vazio")
    void recoletasRepetidasNaoAcumulam() {
        for (int i = 1; i <= 10; i++) {
            upsert("conc-" + i, new BigDecimal("250.75"), 25, COLETA_1.plusDays(i));
        }

        assertThat(linhasOperacionais()).isEqualTo(1);
        assertThat(linhasHistorico()).isZero();
        assertThat(campoValor("valor_bruto")).isEqualByComparingTo("250.75");
    }

    // ── Teste 3 — alteração de valor ─────────────────────────────────────────

    @Test
    @DisplayName("valor alterado preserva a versão anterior e atualiza a vigente")
    void valorAlterado() {
        upsert("conc-1", new BigDecimal("100.00"), 10, COLETA_1);
        upsert("conc-2", new BigDecimal("175.50"), 10, COLETA_2);

        assertThat(linhasOperacionais()).isEqualTo(1);
        assertThat(campoValor("valor_bruto")).isEqualByComparingTo("175.50");

        assertThat(linhasHistorico()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
            "SELECT valor_bruto FROM conciliacao_taxas_historico", BigDecimal.class))
            .isEqualByComparingTo("100.00");
        assertThat(jdbc.queryForObject(
            "SELECT motivo_arquivamento FROM conciliacao_taxas_historico", String.class))
            .isEqualTo("UPSERT_VALUE_CHANGE");
        assertThat(jdbc.queryForObject(
            "SELECT id_conciflex FROM conciliacao_taxas_historico", String.class))
            .isEqualTo("conc-1");
    }

    // ── Teste 4 — alteração de quantidade ────────────────────────────────────

    @Test
    @DisplayName("quantidade alterada preserva a versão anterior")
    void quantidadeAlterada() {
        upsert("conc-1", new BigDecimal("100.00"), 10, COLETA_1);
        upsert("conc-2", new BigDecimal("100.00"), 12, COLETA_2);

        assertThat(linhasOperacionais()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
            "SELECT quantidade FROM conciliacao_taxas", Integer.class)).isEqualTo(12);

        assertThat(linhasHistorico()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
            "SELECT quantidade FROM conciliacao_taxas_historico", Integer.class)).isEqualTo(10);
    }

    @Test
    @DisplayName("reclassificação descritiva também gera histórico")
    void nomeDescritivoAlterado() {
        upsertCompleto("conc-1", "PagSeguro - 108", "108", "Visa - 159", "159",
                       new BigDecimal("100.00"), 10, COLETA_1);
        upsertCompleto("conc-2", "PagSeguro | PagBank - 108", "108", "Visa - 159", "159",
                       new BigDecimal("100.00"), 10, COLETA_2);

        assertThat(linhasOperacionais()).isEqualTo(1);
        assertThat(campoTexto("adquirente")).isEqualTo("PagSeguro | PagBank - 108");

        assertThat(linhasHistorico()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
            "SELECT adquirente FROM conciliacao_taxas_historico", String.class))
            .isEqualTo("PagSeguro - 108");
    }

    // ── Teste 5 — auditada S e N coexistem ───────────────────────────────────

    @Test
    @DisplayName("auditada S e N da mesma combinação coexistem como linhas independentes")
    void auditadaSeparaGrupos() {
        upsertAuditada("conc-S", "S", new BigDecimal("500.00"), 50);
        upsertAuditada("conc-N", "N", new BigDecimal("80.00"), 8);

        assertThat(linhasOperacionais()).isEqualTo(2);
        assertThat(linhasHistorico()).isZero();
        assertThat(jdbc.queryForObject(
            "SELECT SUM(valor_bruto) FROM conciliacao_taxas", BigDecimal.class))
            .isEqualByComparingTo("580.00");

        // Atualizar uma não afeta a outra
        upsertAuditada("conc-S2", "S", new BigDecimal("600.00"), 60);

        assertThat(linhasOperacionais()).isEqualTo(2);
        assertThat(jdbc.queryForObject(
            "SELECT valor_bruto FROM conciliacao_taxas WHERE auditada = 'N'", BigDecimal.class))
            .isEqualByComparingTo("80.00");
        assertThat(jdbc.queryForObject(
            "SELECT valor_bruto FROM conciliacao_taxas WHERE auditada = 'S'", BigDecimal.class))
            .isEqualByComparingTo("600.00");
    }

    // ── Testes 6 e 7 — componentes da chave separam grupos ───────────────────

    @Test
    @DisplayName("produtos diferentes geram linhas diferentes")
    void produtosDiferentes() {
        upsertChave("p1", "108", "159", "1", "10", new BigDecimal("10.00"));
        upsertChave("p2", "108", "159", "1", "20", new BigDecimal("20.00"));

        assertThat(linhasOperacionais()).isEqualTo(2);
        assertThat(linhasHistorico()).isZero();
    }

    @Test
    @DisplayName("bandeiras diferentes geram linhas diferentes")
    void bandeirasDiferentes() {
        upsertChave("b1", "108", "159", "1", "10", new BigDecimal("10.00"));
        upsertChave("b2", "108", "160", "1", "10", new BigDecimal("20.00"));

        assertThat(linhasOperacionais()).isEqualTo(2);
        assertThat(linhasHistorico()).isZero();
    }

    @Test
    @DisplayName("modalidades e adquirentes diferentes geram linhas diferentes")
    void modalidadesEAdquirentesDiferentes() {
        upsertChave("m1", "108", "159", "1", "10", new BigDecimal("10.00"));
        upsertChave("m2", "108", "159", "2", "10", new BigDecimal("20.00"));
        upsertChave("a1", "126", "159", "1", "10", new BigDecimal("30.00"));

        assertThat(linhasOperacionais()).isEqualTo(3);
    }

    // ── Teste 8 — isolamento entre estabelecimentos ──────────────────────────

    @Test
    @DisplayName("a mesma chave em estabelecimentos diferentes não colide")
    void estabelecimentosDiferentes() {
        upsert("conc-A", new BigDecimal("100.00"), 10, COLETA_1);
        upsertEmEstabelecimento(outroEstabelecimentoId, "conc-B",
                                new BigDecimal("200.00"), 20, COLETA_1);

        assertThat(linhasOperacionais()).isEqualTo(2);
        assertThat(linhasHistorico()).isZero();
        assertThat(jdbc.queryForObject(
            "SELECT valor_bruto FROM conciliacao_taxas WHERE estabelecimento_id = ?",
            BigDecimal.class, estabelecimentoId)).isEqualByComparingTo("100.00");
        assertThat(jdbc.queryForObject(
            "SELECT valor_bruto FROM conciliacao_taxas WHERE estabelecimento_id = ?",
            BigDecimal.class, outroEstabelecimentoId)).isEqualByComparingTo("200.00");
    }

    // ── Teste 9 — concorrência ───────────────────────────────────────────────

    @Test
    @DisplayName("upserts concorrentes na mesma chave não geram duplicidade")
    void concorrencia() throws Exception {
        upsert("inicial", new BigDecimal("100.00"), 10, COLETA_1);

        int threads = 8;
        var barreira = new CyclicBarrier(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        try {
            List<Callable<Void>> tarefas = java.util.stream.IntStream.range(0, threads)
                .<Callable<Void>>mapToObj(i -> () -> {
                    barreira.await(10, TimeUnit.SECONDS);
                    upsert("conc-" + i, new BigDecimal("200.00").add(new BigDecimal(i)),
                           20 + i, COLETA_2.plusMinutes(i));
                    return null;
                })
                .toList();

            for (Future<Void> f : pool.invokeAll(tarefas, 60, TimeUnit.SECONDS)) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
        }

        // Invariante central, garantido pela constraint única + ON CONFLICT:
        // uma única linha vigente, sem duplicidade nem perda.
        assertThat(linhasOperacionais()).isEqualTo(1);

        // O valor vigente é um dos que as threads escreveram — nenhum lixo.
        BigDecimal vigente = campoValor("valor_bruto");
        assertThat(vigente).isBetween(new BigDecimal("200.00"), new BigDecimal("207.00"));

        // Cada thread que alterou valores arquivou a versão que enxergou sob seu
        // próprio snapshot. Sob concorrência, duas podem arquivar a mesma versão
        // anterior — por isso o limite é superior, não igualdade exata.
        assertThat(linhasHistorico()).isBetween(1L, (long) threads);

        // A versão inicial nunca se perde: ao menos uma thread a arquivou.
        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*) FROM conciliacao_taxas_historico WHERE id_conciflex = 'inicial'
            """, Long.class)).isGreaterThanOrEqualTo(1L);
    }

    // ── Teste 10 — relatório soma apenas o estado vigente ────────────────────

    @Test
    @DisplayName("após duas coletas o SUM operacional reflete apenas o estado vigente")
    void relatorioSomaApenasVigente() {
        upsertChave("c1-v1", "108", "159", "1", "10", new BigDecimal("100.00"));
        upsertChave("c2-v1", "108", "160", "1", "10", new BigDecimal("200.00"));

        assertThat(jdbc.queryForObject(
            "SELECT SUM(valor_bruto) FROM conciliacao_taxas", BigDecimal.class))
            .isEqualByComparingTo("300.00");

        // Segunda coleta: ambos os grupos com valores maiores
        upsertChave("c1-v2", "108", "159", "1", "10", new BigDecimal("150.00"));
        upsertChave("c2-v2", "108", "160", "1", "10", new BigDecimal("250.00"));

        // Soma vigente, não acumulada (o comportamento antigo daria 700.00)
        assertThat(jdbc.queryForObject(
            "SELECT SUM(valor_bruto) FROM conciliacao_taxas", BigDecimal.class))
            .isEqualByComparingTo("400.00");
        assertThat(linhasOperacionais()).isEqualTo(2);
        assertThat(linhasHistorico()).isEqualTo(2);
    }

    // ── Teste 11 — histórico consultável ─────────────────────────────────────

    @Test
    @DisplayName("a linha do tempo completa de uma chave permanece consultável")
    void historicoConsultavel() {
        upsert("v1", new BigDecimal("100.00"), 10, COLETA_1);
        upsert("v2", new BigDecimal("150.00"), 15, COLETA_2);
        upsert("v3", new BigDecimal("175.00"), 17, COLETA_2.plusDays(1));

        List<BigDecimal> versoes = jdbc.queryForList("""
            SELECT valor_bruto FROM conciliacao_taxas_historico
            WHERE estabelecimento_id = ? AND data_venda = ?
            ORDER BY coletado_em
            """, BigDecimal.class, estabelecimentoId, DATA_VENDA);

        assertThat(versoes).hasSize(2);
        assertThat(versoes.get(0)).isEqualByComparingTo("100.00");
        assertThat(versoes.get(1)).isEqualByComparingTo("150.00");
        assertThat(campoValor("valor_bruto")).isEqualByComparingTo("175.00");
    }

    // ── Auxiliares ───────────────────────────────────────────────────────────

    private void upsert(String idConciflex, BigDecimal valorBruto,
                        Integer quantidade, LocalDateTime coletadoEm) {
        upsertEmEstabelecimento(estabelecimentoId, idConciflex, valorBruto, quantidade, coletadoEm);
    }

    private void upsertEmEstabelecimento(UUID estabId, String idConciflex, BigDecimal valorBruto,
                                         Integer quantidade, LocalDateTime coletadoEm) {
        repository.upsert(
            UUID.randomUUID(), estabId, idConciflex, "EMP-1",
            DATA_VENDA, "Adq", "108", "Band", "159",
            "Modal", "1", "Prod", "10",
            valorBruto, new BigDecimal("1.000000"), new BigDecimal("2.5000"),
            new BigDecimal("2.0000"), quantidade,
            new BigDecimal("5.00"), new BigDecimal("4.00"), new BigDecimal("4.50"),
            new BigDecimal("0.50"), new BigDecimal("0.10"), new BigDecimal("0.0100"),
            "S", "ESTAB-A", coletadoEm
        );
    }

    private void upsertAuditada(String idConciflex, String auditada,
                                BigDecimal valorBruto, Integer quantidade) {
        repository.upsert(
            UUID.randomUUID(), estabelecimentoId, idConciflex, "EMP-1",
            DATA_VENDA, "Adq", "108", "Band", "159",
            "Modal", "1", "Prod", "10",
            valorBruto, null, null, null, quantidade,
            null, null, null, null, null, null,
            auditada, "ESTAB-A", COLETA_1
        );
    }

    private void upsertChave(String idConciflex, String codAdq, String codBand,
                             String codModal, String codProd, BigDecimal valorBruto) {
        repository.upsert(
            UUID.randomUUID(), estabelecimentoId, idConciflex, "EMP-1",
            DATA_VENDA, "Adq", codAdq, "Band", codBand,
            "Modal", codModal, "Prod", codProd,
            valorBruto, null, null, null, 1,
            null, null, null, null, null, null,
            "S", "ESTAB-A", COLETA_1
        );
    }

    private void upsertCompleto(String idConciflex, String adquirente, String codAdq,
                                String bandeira, String codBand, BigDecimal valorBruto,
                                Integer quantidade, LocalDateTime coletadoEm) {
        repository.upsert(
            UUID.randomUUID(), estabelecimentoId, idConciflex, "EMP-1",
            DATA_VENDA, adquirente, codAdq, bandeira, codBand,
            "Modal", "1", "Prod", "10",
            valorBruto, null, null, null, quantidade,
            null, null, null, null, null, null,
            "S", "ESTAB-A", coletadoEm
        );
    }

    private long linhasOperacionais() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM conciliacao_taxas", Long.class);
    }

    private long linhasHistorico() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM conciliacao_taxas_historico", Long.class);
    }

    private String campoTexto(String coluna) {
        return jdbc.queryForObject("SELECT " + coluna + " FROM conciliacao_taxas", String.class);
    }

    private BigDecimal campoValor(String coluna) {
        return jdbc.queryForObject("SELECT " + coluna + " FROM conciliacao_taxas", BigDecimal.class);
    }
}
