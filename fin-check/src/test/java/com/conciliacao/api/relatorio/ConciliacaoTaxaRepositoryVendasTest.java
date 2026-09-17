package com.conciliacao.api.relatorio;

import com.conciliacao.api.entity.Cliente;
import com.conciliacao.api.entity.ConciliacaoTaxa;
import com.conciliacao.api.entity.Estabelecimento;
import com.conciliacao.api.repository.ConciliacaoTaxaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Executa de verdade as queries JPQL novas de {@link ConciliacaoTaxaRepository} contra
 * PostgreSQL 16, com o schema montado pelo Flyway.
 *
 * <p>Prova que {@code sumValorBruto} e {@code sumValorBrutoPorModalidade} são JPQL válida,
 * que os parâmetros ligam corretamente e que o recorte por {@code data_venda} funciona —
 * garantias que os testes com mock não conseguem dar.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("ConciliacaoTaxaRepository — queries de vendas do relatório semanal")
class ConciliacaoTaxaRepositoryVendasTest {

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
            .withDatabaseName("repo_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired private ConciliacaoTaxaRepository repository;
    @Autowired private EntityManager em;

    private UUID estabelecimentoId;

    @BeforeEach
    void semear() {
        Cliente cliente = Cliente.builder()
            .razaoSocial("Frutaria Teste")
            .cnpj(String.valueOf(System.nanoTime()).substring(0, 14))
            .whatsapp("11999999999")
            .conciflex_login("enc")
            .conciflex_senha("enc")
            .build();
        em.persist(cliente);

        Estabelecimento est = Estabelecimento.builder()
            .cliente(cliente)
            .descricao("Matriz")
            .identificadorConciflex("ESTAB-" + System.nanoTime())
            .ativo(true)
            .build();
        em.persist(est);
        em.flush();

        estabelecimentoId = est.getId();
    }

    @Test
    @DisplayName("sumValorBruto soma apenas as vendas com data_venda dentro do período")
    void sumValorBrutoRespeitaPeriodo() {
        venda(LocalDate.of(2026, 8, 31), DEBITO,  "999.00", "9.00");
        venda(LocalDate.of(2026, 9, 1),  DEBITO,  "100.00", "1.00");
        venda(LocalDate.of(2026, 9, 5),  CREDITO, "200.00", "2.00");
        venda(LocalDate.of(2026, 9, 6),  PIX,     "888.00", "0.00");
        em.flush();

        assertThat(repository.sumValorBruto(estabelecimentoId, INICIO, FIM))
            .isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("sumValorBruto devolve zero, e não null, em período sem vendas")
    void sumValorBrutoSemVendas() {
        assertThat(repository.sumValorBruto(estabelecimentoId, INICIO, FIM))
            .isNotNull()
            .isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("sumValorBrutoPorModalidade agrupa pelos códigos reais da Conciflex")
    void agrupamentoPorModalidade() {
        venda(LocalDate.of(2026, 9, 1), PIX,       "100.00", "0.00");
        venda(LocalDate.of(2026, 9, 2), DEBITO,    "200.00", "2.00");
        venda(LocalDate.of(2026, 9, 2), CREDITO,   "300.00", "9.00");
        venda(LocalDate.of(2026, 9, 3), VOUCHER,   "400.00", "8.00");
        venda(LocalDate.of(2026, 9, 3), PARCELADO, "50.00",  "1.00");
        em.flush();

        Map<String, BigDecimal> mix = new HashMap<>();
        for (Object[] linha : repository.sumValorBrutoPorModalidade(estabelecimentoId, INICIO, FIM)) {
            mix.put((String) linha[0], (BigDecimal) linha[1]);
        }

        assertThat(mix.get(PIX)).isEqualByComparingTo("100.00");
        assertThat(mix.get(DEBITO)).isEqualByComparingTo("200.00");
        assertThat(mix.get(CREDITO)).isEqualByComparingTo("300.00");
        assertThat(mix.get(VOUCHER)).isEqualByComparingTo("400.00");
        assertThat(mix.get(PARCELADO)).isEqualByComparingTo("50.00");

        // O total confere com sumValorBruto: nenhuma linha é perdida ou contada duas vezes.
        assertThat(mix.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add))
            .isEqualByComparingTo(repository.sumValorBruto(estabelecimentoId, INICIO, FIM));
    }

    @Test
    @DisplayName("as duas queries ignoram vendas de outro estabelecimento")
    void isolamentoPorEstabelecimento() {
        venda(LocalDate.of(2026, 9, 2), DEBITO, "100.00", "1.00");
        em.flush();

        UUID outro = UUID.randomUUID();
        assertThat(repository.sumValorBruto(outro, INICIO, FIM)).isEqualByComparingTo("0");
        assertThat(repository.sumValorBrutoPorModalidade(outro, INICIO, FIM)).isEmpty();
    }

    @Test
    @DisplayName("líquido previsto = sumValorBruto − sumTaxaPraticadaRs, sobre o mesmo recorte")
    void liquidoPrevistoCoerenteComTaxas() {
        venda(LocalDate.of(2026, 9, 2), CREDITO, "1000.00", "30.00");
        venda(LocalDate.of(2026, 9, 3), DEBITO,  "500.00",  "5.00");
        em.flush();

        BigDecimal bruto = repository.sumValorBruto(estabelecimentoId, INICIO, FIM);
        BigDecimal taxas = repository.sumTaxaPraticadaRs(estabelecimentoId, INICIO, FIM);

        assertThat(bruto).isEqualByComparingTo("1500.00");
        assertThat(taxas).isEqualByComparingTo("35.00");
        assertThat(bruto.subtract(taxas)).isEqualByComparingTo("1465.00");
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private void venda(LocalDate dataVenda, String codigoModalidade,
                       String valorBruto, String taxaPraticadaRs) {
        ConciliacaoTaxa ct = new ConciliacaoTaxa();
        ct.setEstabelecimento(em.getReference(Estabelecimento.class, estabelecimentoId));
        ct.setIdConciflex("cf-" + UUID.randomUUID());
        ct.setDataVenda(dataVenda);
        ct.setAdquirente("Adq");
        ct.setCodigoAdquirente("108");
        ct.setBandeira("Band");
        ct.setCodBandeira("1");
        ct.setModalidade("Modal");
        ct.setCodigoModalidade(codigoModalidade);
        ct.setProduto("Prod");
        // codigo_produto distinto por modalidade mantém a chave lógica única.
        ct.setCodigoProduto("p" + codigoModalidade);
        ct.setValorBruto(new BigDecimal(valorBruto));
        ct.setTaxaPraticadaRs(new BigDecimal(taxaPraticadaRs));
        ct.setQuantidade(1);
        ct.setAuditada("S");
        ct.setColetadoEm(LocalDateTime.now());
        em.persist(ct);
    }
}
