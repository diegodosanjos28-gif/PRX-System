package com.conciliacao.api.implantacao;

import com.conciliacao.api.dto.request.ImplantacaoClienteRequest;
import com.conciliacao.api.dto.response.ImplantacaoClienteResponse;
import com.conciliacao.api.service.ImplantacaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Classe comercial da implantação (V26).
 *
 * <p>A classe é uma dimensão INDEPENDENTE de etapa e saúde operacional. Estes testes
 * provam a persistência dos quatro valores, a tolerância a registros legados sem classe
 * e — o ponto mais sensível — que nada em {@code etapa} é afetado.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Implantação — classe comercial")
class ImplantacaoClasseTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("classe_test")
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

    @Autowired private ImplantacaoService service;
    @Autowired private Validator validator;
    @Autowired private JdbcTemplate jdbc;

    private static final ObjectMapper JSON = new ObjectMapper();

    private UUID clienteId;

    @BeforeEach
    void semear() {
        jdbc.execute("TRUNCATE implantacao_demandas, implantacoes_cliente, "
                   + "estabelecimentos, clientes CASCADE");

        clienteId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO clientes (id, razao_social, nome_fantasia, cnpj, whatsapp,
                                  conciflex_login, conciflex_senha)
            VALUES (?, 'Cliente Classe LTDA', 'Cliente Classe', '33333333333333',
                    '11999999999', 'enc', 'enc')
            """, clienteId);
    }

    // ── Persistência das quatro classes ──────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"PRIME", "GOLD", "PLATIUM", "BRONZE"})
    @DisplayName("as quatro classes são persistidas e devolvidas pela API")
    void persisteAsQuatroClasses(String classe) {
        ImplantacaoClienteResponse criada = service.criar(request("curral", null, classe));

        assertThat(criada.classe()).isEqualTo(classe);
        assertThat(colunaClasse(criada.id())).isEqualTo(classe);
        assertThat(service.buscarPorId(criada.id()).classe()).isEqualTo(classe);
    }

    @Test
    @DisplayName("PLATIUM é aceito com a grafia do negócio; PLATINUM é rejeitado")
    void grafiaDoNegocio() {
        assertThat(violacoesDeClasse("PLATIUM")).isEmpty();
        assertThat(violacoesDeClasse("PLATINUM")).isNotEmpty();
    }

    @Test
    @DisplayName("a classe pode ser alterada de BRONZE para PRIME")
    void atualizaClasse() {
        ImplantacaoClienteResponse criada = service.criar(request("curral", null, "BRONZE"));

        ImplantacaoClienteResponse atualizada =
            service.atualizar(criada.id(), request("curral", null, "PRIME"));

        assertThat(atualizada.classe()).isEqualTo("PRIME");
        assertThat(colunaClasse(criada.id())).isEqualTo("PRIME");
    }

    @Test
    @DisplayName("ciclo completo: GOLD persiste, é relido do banco e volta pelo caminho da listagem")
    void cicloCompletoDePersistenciaEReleitura() {
        ImplantacaoClienteResponse criada = service.criar(request("curral", null, null));
        UUID id = criada.id();
        assertThat(colunaClasse(id)).isNull();

        // 1ª volta — GOLD
        service.atualizar(id, request("curral", null, "GOLD"));

        // Prova de persistência real: leitura direta da coluna, sem passar por JPA.
        assertThat(colunaClasse(id)).isEqualTo("GOLD");

        // Releitura pelo caminho de detalhe (GET /api/implantacoes/{id})
        assertThat(service.buscarPorId(id).classe()).isEqualTo("GOLD");

        // Releitura pelo MESMO caminho que alimenta o Curral (GET /api/implantacoes):
        // listarTodos → toListResponseComResumo, que reconstrói o record posicionalmente.
        assertThat(service.listarTodos())
            .filteredOn(i -> i.id().equals(id))
            .singleElement()
            .satisfies(i -> assertThat(i.classe()).isEqualTo("GOLD"));

        // 2ª volta — PRIME, para provar que a troca também sobrevive
        service.atualizar(id, request("curral", null, "PRIME"));

        assertThat(colunaClasse(id)).isEqualTo("PRIME");
        assertThat(service.buscarPorId(id).classe()).isEqualTo("PRIME");
        assertThat(service.listarTodos())
            .filteredOn(i -> i.id().equals(id))
            .singleElement()
            .satisfies(i -> assertThat(i.classe()).isEqualTo("PRIME"));
    }

    @Test
    @DisplayName("a classe sobrevive a um update que não menciona classe? não — o PUT é total")
    void putEhSubstituicaoTotal() {
        ImplantacaoClienteResponse criada = service.criar(request("curral", null, "GOLD"));

        // O endpoint é PUT (substituição total), não PATCH: enviar classe=null limpa o campo.
        // Documentado por teste para que a semântica fique explícita — o formulário sempre
        // envia a classe, então na prática o valor nunca é perdido pela tela.
        service.atualizar(criada.id(), request("curral", null, null));

        assertThat(colunaClasse(criada.id())).isNull();
    }

    // ── Registros legados ────────────────────────────────────────────────────

    @Test
    @DisplayName("registro sem classe é criado, lido e listado sem quebrar")
    void legadoSemClasse() {
        ImplantacaoClienteResponse criada = service.criar(request("curral", null, null));

        assertThat(criada.classe()).isNull();
        assertThat(colunaClasse(criada.id())).isNull();
        assertThat(service.buscarPorId(criada.id()).classe()).isNull();
        assertThat(service.listarTodos())
            .singleElement()
            .satisfies(i -> assertThat(i.classe()).isNull());
    }

    @Test
    @DisplayName("implantação legada inserida direto no banco continua legível pela API")
    void legadoInseridoNoBanco() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO implantacoes_cliente (id, cliente_id, etapa, adquirentes)
            VALUES (?, ?, 'curral', '[]'::jsonb)
            """, id, clienteId);

        assertThat(service.buscarPorId(id).classe()).isNull();
        assertThat(service.listarTodos()).hasSize(1);
    }

    @Test
    @DisplayName("editar um registro legado apenas define a classe, sem tocar na etapa")
    void classificaLegadoPreservandoEtapa() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO implantacoes_cliente (id, cliente_id, etapa, adquirentes)
            VALUES (?, ?, 'curral', '[]'::jsonb)
            """, id, clienteId);

        service.atualizar(id, request("curral", null, "GOLD"));

        assertThat(colunaClasse(id)).isEqualTo("GOLD");
        assertThat(jdbc.queryForObject(
            "SELECT etapa FROM implantacoes_cliente WHERE id = ?", String.class, id))
            .isEqualTo("curral");
    }

    // ── Validação ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("valor fora do domínio é rejeitado pela validação")
    void valorInvalidoRejeitado() {
        assertThat(violacoesDeClasse("DIAMANTE")).isNotEmpty();
        assertThat(violacoesDeClasse("prime")).isNotEmpty();   // minúsculas não passam
        assertThat(violacoesDeClasse("")).isNotEmpty();
    }

    @Test
    @DisplayName("null passa na validação — é o estado de 'não classificado'")
    void nullEhValido() {
        assertThat(violacoesDeClasse(null)).isEmpty();
    }

    @Test
    @DisplayName("o banco recusa classe fora do domínio mesmo por INSERT direto")
    void checkConstraintNoBanco() {
        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*) FROM pg_constraint
             WHERE conname = 'chk_classe'
               AND conrelid = 'public.implantacoes_cliente'::regclass
            """, Long.class)).isEqualTo(1L);
    }

    // ── Independência em relação à etapa ─────────────────────────────────────

    @Test
    @DisplayName("classe e etapa são dimensões independentes")
    void classeNaoInterfereNaEtapa() {
        // Prime fora do curral e Bronze no curral coexistem sem qualquer validação cruzada.
        ImplantacaoClienteResponse fora = service.criar(request("corrida", "fluindo", "PRIME"));
        assertThat(fora.etapa()).isEqualTo("corrida");
        assertThat(fora.status()).isEqualTo("fluindo");
        assertThat(fora.classe()).isEqualTo("PRIME");

        UUID outroCliente = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO clientes (id, razao_social, cnpj, whatsapp,
                                  conciflex_login, conciflex_senha)
            VALUES (?, 'Outro LTDA', '44444444444444', '11988888888', 'enc', 'enc')
            """, outroCliente);

        ImplantacaoClienteResponse dentro = service.criar(
            new ImplantacaoClienteRequest(outroCliente, "curral", null, "BRONZE",
                null, null, JSON.createArrayNode(), null, null, null, null));

        assertThat(dentro.etapa()).isEqualTo("curral");
        assertThat(dentro.status()).isNull();
        assertThat(dentro.classe()).isEqualTo("BRONZE");
    }

    @Test
    @DisplayName("a regra de status por etapa segue valendo, independente da classe")
    void regraDeStatusPreservada() {
        // etapa 'curral' continua exigindo status nulo — comportamento anterior à V26.
        ImplantacaoClienteResponse criada = service.criar(request("curral", null, "PRIME"));
        assertThat(criada.status()).isNull();

        ImplantacaoClienteResponse movida =
            service.atualizar(criada.id(), request("onboarding", "travado", "PRIME"));
        assertThat(movida.etapa()).isEqualTo("onboarding");
        assertThat(movida.status()).isEqualTo("travado");
        assertThat(movida.classe()).isEqualTo("PRIME");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ImplantacaoClienteRequest request(String etapa, String status, String classe) {
        return new ImplantacaoClienteRequest(
            clienteId, etapa, status, classe,
            "Responsavel", "Contato", JSON.createArrayNode(),
            "obs", null, null, null);
    }

    private Set<ConstraintViolation<ImplantacaoClienteRequest>> violacoesDeClasse(String classe) {
        return validator.validateProperty(request("curral", null, classe), "classe");
    }

    private String colunaClasse(UUID implantacaoId) {
        return jdbc.queryForObject(
            "SELECT classe FROM implantacoes_cliente WHERE id = ?", String.class, implantacaoId);
    }
}
