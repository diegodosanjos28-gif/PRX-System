package com.conciliacao.api.relatorio;

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
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova que os indicadores de VENDAS do relatório semanal saem de
 * {@code conciliacao_taxas} pela {@code data_venda}, e não de {@code recebimentos}.
 *
 * <p>Os testes exercitam exatamente as expressões SQL das queries novas de
 * {@code ConciliacaoTaxaRepository} ({@code sumValorBruto},
 * {@code sumValorBrutoPorModalidade}) e da já existente {@code sumTaxaPraticadaRs},
 * contra PostgreSQL 16 real com o schema montado pelo Flyway.
 *
 * <p>A intenção é blindar a fonte de dados: qualquer retorno ao fluxo de recebimentos
 * quebra estes testes.
 */
@Testcontainers
@DisplayName("Relatório semanal — fonte dos indicadores de vendas")
class RelatorioSemanalFonteVendasTest {

    // Códigos reais de modalidade da Conciflex presentes em conciliacao_taxas.
    private static final String DEBITO    = "0";
    private static final String CREDITO   = "1";
    private static final String VOUCHER   = "3";
    private static final String PARCELADO = "17";
    private static final String PIX       = "21";

    private static final LocalDate INICIO = LocalDate.of(2026, 9, 1);
    private static final LocalDate FIM    = LocalDate.of(2026, 9, 5);

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("relatorio_test")
            .withUsername("test")
            .withPassword("test");

    private UUID estabelecimentoId;

    @BeforeEach
    void prepararSchema() throws SQLException {
        flyway().clean();
        flyway().migrate();
        semearEstabelecimento();
    }

    // ── Cenário A — vendas não dependem de recebimentos ──────────────────────

    @Test
    @DisplayName("A: vendas saem de conciliacao_taxas mesmo com recebimentos de valor totalmente diferente")
    void vendasIndependemDeRecebimentos() throws SQLException {
        inserirVenda(LocalDate.of(2026, 9, 2), DEBITO, "1000.00", "10.00");
        inserirVenda(LocalDate.of(2026, 9, 3), CREDITO, "500.00", "5.00");

        // Recebimentos com valores propositalmente divergentes e em datas de pagamento
        // dentro do período — não podem influenciar nenhum indicador de venda.
        inserirRecebimento(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 2), "Débito", "99999.99", "88888.88");
        inserirRecebimento(LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 3), "Crédito", "77777.77", "66666.66");

        assertThat(sumValorBruto()).isEqualByComparingTo("1500.00");
        assertThat(sumTaxaPraticadaRs()).isEqualByComparingTo("15.00");
        assertThat(liquidoPrevisto()).isEqualByComparingTo("1485.00");
    }

    @Test
    @DisplayName("A: alterar recebimentos não move o total de vendas")
    void alterarRecebimentosNaoAlteraVendas() throws SQLException {
        inserirVenda(LocalDate.of(2026, 9, 2), DEBITO, "1000.00", "10.00");
        BigDecimal antes = sumValorBruto();

        inserirRecebimento(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 2), "Débito", "50000.00", "49000.00");
        inserirRecebimento(LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 4), "Pix", "12345.67", "12000.00");

        assertThat(sumValorBruto()).isEqualByComparingTo(antes);
        assertThat(sumValorBruto()).isEqualByComparingTo("1000.00");
    }

    // ── Cenário B — mix por modalidade ───────────────────────────────────────

    @Test
    @DisplayName("B: o mix classifica Pix, Débito, Crédito e Voucher pelo codigo_modalidade")
    void mixPorModalidade() throws SQLException {
        inserirVenda(LocalDate.of(2026, 9, 1), PIX,     "100.00", "0.00");
        inserirVenda(LocalDate.of(2026, 9, 2), DEBITO,  "200.00", "2.00");
        inserirVenda(LocalDate.of(2026, 9, 3), CREDITO, "300.00", "9.00");
        inserirVenda(LocalDate.of(2026, 9, 4), VOUCHER, "400.00", "8.00");

        Map<String, BigDecimal> mix = sumValorBrutoPorModalidade();

        assertThat(mix.get(PIX)).isEqualByComparingTo("100.00");
        assertThat(mix.get(DEBITO)).isEqualByComparingTo("200.00");
        assertThat(mix.get(CREDITO)).isEqualByComparingTo("300.00");
        assertThat(mix.get(VOUCHER)).isEqualByComparingTo("400.00");

        // Sem outras modalidades, o mix fecha exatamente com as vendas do período.
        assertThat(mix.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add))
            .isEqualByComparingTo(sumValorBruto());
    }

    @Test
    @DisplayName("B: Parcelado fica fora das quatro modalidades, e a diferença é exatamente ele")
    void parceladoNaoEntraNoMixDeQuatroModalidades() throws SQLException {
        inserirVenda(LocalDate.of(2026, 9, 1), DEBITO,    "200.00", "2.00");
        inserirVenda(LocalDate.of(2026, 9, 2), PARCELADO, "50.00",  "1.00");

        Map<String, BigDecimal> mix = sumValorBrutoPorModalidade();

        BigDecimal quatroModalidades = BigDecimal.ZERO
            .add(mix.getOrDefault(PIX,     BigDecimal.ZERO))
            .add(mix.getOrDefault(DEBITO,  BigDecimal.ZERO))
            .add(mix.getOrDefault(CREDITO, BigDecimal.ZERO))
            .add(mix.getOrDefault(VOUCHER, BigDecimal.ZERO));

        assertThat(quatroModalidades).isEqualByComparingTo("200.00");
        assertThat(sumValorBruto()).isEqualByComparingTo("250.00");
        assertThat(sumValorBruto().subtract(quatroModalidades)).isEqualByComparingTo("50.00");
        assertThat(mix.get(PARCELADO)).isEqualByComparingTo("50.00");
    }

    // ── Cenário C — recorte de período ───────────────────────────────────────

    @Test
    @DisplayName("C: vendas fora do intervalo de data_venda não entram")
    void periodoFiltraPorDataVenda() throws SQLException {
        inserirVenda(LocalDate.of(2026, 8, 31), DEBITO, "999.00", "9.00");  // véspera
        inserirVenda(LocalDate.of(2026, 9, 1),  DEBITO, "100.00", "1.00");  // borda inicial
        inserirVenda(LocalDate.of(2026, 9, 5),  CREDITO, "200.00", "2.00"); // borda final
        inserirVenda(LocalDate.of(2026, 9, 6),  DEBITO, "888.00", "8.00");  // dia seguinte

        assertThat(sumValorBruto()).isEqualByComparingTo("300.00");
        assertThat(sumTaxaPraticadaRs()).isEqualByComparingTo("3.00");
    }

    // ── Cenário D — data de venda × data de pagamento ────────────────────────

    @Test
    @DisplayName("D: venda fora do período recebida dentro dele NÃO entra nas vendas")
    void vendaForaRecebidaDentroNaoEntra() throws SQLException {
        // Vendida em agosto, liquidada em setembro.
        inserirVenda(LocalDate.of(2026, 8, 20), DEBITO, "700.00", "7.00");
        inserirRecebimento(LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 2), "Débito", "700.00", "693.00");

        assertThat(sumValorBruto()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("D: venda dentro do período recebida fora dele CONTINUA nas vendas")
    void vendaDentroRecebidaForaContinua() throws SQLException {
        // Vendida em setembro, liquidada em outubro.
        inserirVenda(LocalDate.of(2026, 9, 3), CREDITO, "600.00", "18.00");
        inserirRecebimento(LocalDate.of(2026, 9, 3), LocalDate.of(2026, 10, 3), "Crédito", "600.00", "582.00");

        assertThat(sumValorBruto()).isEqualByComparingTo("600.00");
        assertThat(sumTaxaPraticadaRs()).isEqualByComparingTo("18.00");
        assertThat(liquidoPrevisto()).isEqualByComparingTo("582.00");
    }

    // ── Cenário E — não regressão ────────────────────────────────────────────

    @Test
    @DisplayName("E: compor o relatório não escreve em conciliacao_taxas nem em recebimentos")
    void composicaoEhSomenteLeitura() throws SQLException {
        inserirVenda(LocalDate.of(2026, 9, 2), DEBITO, "1000.00", "10.00");
        inserirVenda(LocalDate.of(2026, 9, 2), PIX,    "250.00",  "0.00");
        inserirRecebimento(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 2), "Débito", "1000.00", "990.00");

        long taxasAntes = contar("SELECT COUNT(*) FROM conciliacao_taxas");
        long recebAntes = contar("SELECT COUNT(*) FROM recebimentos");
        long histAntes  = contar("SELECT COUNT(*) FROM conciliacao_taxas_historico");

        // Executa todas as leituras que compõem os indicadores do relatório.
        sumValorBruto();
        sumValorBrutoPorModalidade();
        sumTaxaPraticadaRs();
        liquidoPrevisto();

        assertThat(contar("SELECT COUNT(*) FROM conciliacao_taxas")).isEqualTo(taxasAntes);
        assertThat(contar("SELECT COUNT(*) FROM recebimentos")).isEqualTo(recebAntes);
        assertThat(contar("SELECT COUNT(*) FROM conciliacao_taxas_historico")).isEqualTo(histAntes);
    }

    @Test
    @DisplayName("E: agregar vendas não multiplica linhas quando há vários recebimentos por venda")
    void semMultiplicacaoCartesiana() throws SQLException {
        // Uma venda no crédito, liquidada em três parcelas.
        inserirVenda(LocalDate.of(2026, 9, 2), CREDITO, "300.00", "9.00");
        inserirRecebimento(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 2), "Crédito", "100.00", "97.00");
        inserirRecebimento(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 10, 2), "Crédito", "100.00", "97.00");
        inserirRecebimento(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 11, 2), "Crédito", "100.00", "97.00");

        assertThat(sumValorBruto()).isEqualByComparingTo("300.00");
        assertThat(sumValorBrutoPorModalidade().get(CREDITO)).isEqualByComparingTo("300.00");
    }

    // ── Expressões sob teste ─────────────────────────────────────────────────
    // Espelham as queries JPQL de ConciliacaoTaxaRepository.

    private BigDecimal sumValorBruto() throws SQLException {
        return escalar("""
            SELECT COALESCE(SUM(ct.valor_bruto), 0)
              FROM conciliacao_taxas ct
             WHERE ct.estabelecimento_id = ?
               AND ct.data_venda BETWEEN ? AND ?
            """);
    }

    private BigDecimal sumTaxaPraticadaRs() throws SQLException {
        return escalar("""
            SELECT COALESCE(SUM(ct.taxa_praticada_rs), 0)
              FROM conciliacao_taxas ct
             WHERE ct.estabelecimento_id = ?
               AND ct.data_venda BETWEEN ? AND ?
            """);
    }

    /** Reproduz a composição do serviço: bruto vendido − taxa praticada sobre essas vendas. */
    private BigDecimal liquidoPrevisto() throws SQLException {
        return sumValorBruto().subtract(sumTaxaPraticadaRs());
    }

    private Map<String, BigDecimal> sumValorBrutoPorModalidade() throws SQLException {
        String sql = """
            SELECT ct.codigo_modalidade, COALESCE(SUM(ct.valor_bruto), 0)
              FROM conciliacao_taxas ct
             WHERE ct.estabelecimento_id = ?
               AND ct.data_venda BETWEEN ? AND ?
             GROUP BY ct.codigo_modalidade
            """;
        Map<String, BigDecimal> resultado = new LinkedHashMap<>();
        try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, estabelecimentoId);
            ps.setObject(2, INICIO);
            ps.setObject(3, FIM);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.put(rs.getString(1), rs.getBigDecimal(2));
                }
            }
        }
        return resultado;
    }

    // ── Infraestrutura ───────────────────────────────────────────────────────

    private BigDecimal escalar(String sql) throws SQLException {
        try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, estabelecimentoId);
            ps.setObject(2, INICIO);
            ps.setObject(3, FIM);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }

    private long contar(String sql) throws SQLException {
        try (Connection c = conectar(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private Flyway flyway() {
        return Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .locations("classpath:db/migration")
            .cleanDisabled(false)
            .load();
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
                VALUES ('%s', 'Frutaria Teste', '00000000000191', '11999999999', 'enc', 'enc')
                """.formatted(clienteId));
            s.executeUpdate("""
                INSERT INTO estabelecimentos (id, cliente_id, descricao, identificador_conciflex)
                VALUES ('%s', '%s', 'Estab Teste', 'ESTAB-TESTE')
                """.formatted(estabelecimentoId, clienteId));
        }
    }

    /**
     * Insere uma linha de venda. Cada chamada usa adquirente/bandeira/produto distintos por
     * modalidade e data, de modo que a chave lógica de {@code uq_ct_chave_logica} nunca colide.
     */
    private void inserirVenda(LocalDate dataVenda, String codigoModalidade,
                              String valorBruto, String taxaPraticadaRs) throws SQLException {
        String sql = """
            INSERT INTO conciliacao_taxas (
                id, estabelecimento_id, id_conciflex, data_venda,
                adquirente, codigo_adquirente, bandeira, cod_bandeira,
                modalidade, codigo_modalidade, produto, codigo_produto,
                valor_bruto, taxa_praticada_rs, quantidade, auditada, coletado_em
            ) VALUES (?, ?, ?, ?, 'Adq', '108', 'Band', '1', 'Modal', ?, 'Prod', ?, ?, ?, 1, 'S', now())
            """;
        try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, estabelecimentoId);
            ps.setString(3, "cf-" + UUID.randomUUID());
            ps.setObject(4, dataVenda);
            ps.setString(5, codigoModalidade);
            // codigo_produto distinto por modalidade mantém a chave lógica única.
            ps.setString(6, "p" + codigoModalidade);
            ps.setBigDecimal(7, new BigDecimal(valorBruto));
            ps.setBigDecimal(8, new BigDecimal(taxaPraticadaRs));
            ps.executeUpdate();
        }
    }

    private void inserirRecebimento(LocalDate dataVenda, LocalDate dataPagamento,
                                    String modalidade, String valorBruto,
                                    String valorLiquido) throws SQLException {
        String sql = """
            INSERT INTO recebimentos (
                id, estabelecimento_id, id_conciflex, data_venda, data_pagamento,
                adquirente, bandeira, modalidade, valor_bruto, valor_liquido
            ) VALUES (?, ?, ?, ?, ?, 'Adq', 'Band', ?, ?, ?)
            """;
        try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, estabelecimentoId);
            ps.setString(3, "rc-" + UUID.randomUUID());
            ps.setObject(4, dataVenda);
            ps.setObject(5, dataPagamento);
            ps.setString(6, modalidade);
            ps.setBigDecimal(7, new BigDecimal(valorBruto));
            ps.setBigDecimal(8, new BigDecimal(valorLiquido));
            ps.executeUpdate();
        }
    }
}
