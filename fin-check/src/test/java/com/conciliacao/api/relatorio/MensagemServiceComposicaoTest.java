package com.conciliacao.api.relatorio;

import com.conciliacao.api.dto.request.MensagemGerarRequest;
import com.conciliacao.api.dto.response.AuditoriaResumoResponse;
import com.conciliacao.api.dto.response.RecebimentoResumoResponse;
import com.conciliacao.api.entity.Cliente;
import com.conciliacao.api.entity.Estabelecimento;
import com.conciliacao.api.entity.Template;
import com.conciliacao.api.repository.ConciliacaoTaxaRepository;
import com.conciliacao.api.repository.EstabelecimentoRepository;
import com.conciliacao.api.repository.MensagemEnviadaRepository;
import com.conciliacao.api.service.AnthropicService;
import com.conciliacao.api.service.AuditoriaService;
import com.conciliacao.api.service.ClienteService;
import com.conciliacao.api.service.EstabelecimentoService;
import com.conciliacao.api.service.MensagemService;
import com.conciliacao.api.service.RecebimentoService;
import com.conciliacao.api.service.TemplateService;
import com.conciliacao.api.service.WhatsAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Prova a COMPOSIÇÃO dos indicadores do relatório semanal: qual variável de template
 * recebe qual número, e de qual fonte esse número veio.
 *
 * <p>Complementa {@link RelatorioSemanalFonteVendasTest}, que valida as queries contra
 * PostgreSQL real. Aqui as fontes são simuladas com valores deliberadamente distintos
 * entre vendas e recebimentos, de modo que qualquer troca de origem apareça como falha.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Relatório semanal — composição dos indicadores")
class MensagemServiceComposicaoTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 9, 1);
    private static final LocalDate FIM    = LocalDate.of(2026, 9, 5);

    @Mock private AuditoriaService auditoriaService;
    @Mock private RecebimentoService recebimentoService;
    @Mock private AnthropicService anthropicService;
    @Mock private WhatsAppService whatsAppService;
    @Mock private ClienteService clienteService;
    @Mock private EstabelecimentoService estabelecimentoService;
    @Mock private TemplateService templateService;
    @Mock private EstabelecimentoRepository estabelecimentoRepository;
    @Mock private MensagemEnviadaRepository mensagemEnviadaRepository;
    @Mock private ConciliacaoTaxaRepository conciliacaoTaxaRepository;

    @InjectMocks private MensagemService mensagemService;

    private UUID clienteId;
    private UUID estabelecimentoId;

    @BeforeEach
    void preparar() {
        clienteId = UUID.randomUUID();
        estabelecimentoId = UUID.randomUUID();

        Cliente cliente = new Cliente();
        cliente.setId(clienteId);
        cliente.setRazaoSocial("Frutaria Rincão LTDA");
        cliente.setNomeFantasia("Frutaria Rincão");
        cliente.setWhatsapp("11999999999");

        Estabelecimento est = new Estabelecimento();
        est.setId(estabelecimentoId);
        est.setDescricao("Matriz");

        Template template = new Template();
        template.setId(1L);
        template.setNome("Relatorio Semanal");
        template.setConteudo(
            "vendas={totalValorBruto} media={mediaVendas} liquido={liquidoPrevisto} "
          + "taxas={totalTaxaPraticadaRS} pix={totalPix} debito={totalDebito} "
          + "credito={totalCredito} voucher={totalVouchers}");

        when(clienteService.buscarEntidade(clienteId)).thenReturn(cliente);
        when(estabelecimentoService.buscarEntidade(estabelecimentoId)).thenReturn(est);
        when(templateService.buscarEntidade(1L)).thenReturn(template);

        when(auditoriaService.resumo(eq(estabelecimentoId), any(), any()))
            .thenReturn(new AuditoriaResumoResponse(
                10L, new BigDecimal("1.00"), new BigDecimal("-2.00"), List.of(), List.of()));

        // Recebimentos com valores grandes e completamente divergentes das vendas.
        // Nenhum indicador de venda pode encostar nesses números.
        when(recebimentoService.resumo(eq(estabelecimentoId), any(), any()))
            .thenReturn(new RecebimentoResumoResponse(
                new BigDecimal("56940.38"),   // totalRecebido  (líquido liquidado)
                new BigDecimal("619.91"),     // totalDescontado
                List.of(),
                List.of()));
    }

    /** Vendas: 1.000 débito + 2.000 crédito + 500 pix + 300 voucher + 200 parcelado = 4.000. */
    private void stubVendas() {
        when(conciliacaoTaxaRepository.sumValorBruto(estabelecimentoId, INICIO, FIM))
            .thenReturn(new BigDecimal("4000.00"));
        when(conciliacaoTaxaRepository.sumValorBrutoPorModalidade(estabelecimentoId, INICIO, FIM))
            .thenReturn(List.of(
                new Object[]{"0",  new BigDecimal("1000.00")},
                new Object[]{"1",  new BigDecimal("2000.00")},
                new Object[]{"21", new BigDecimal("500.00")},
                new Object[]{"3",  new BigDecimal("300.00")},
                new Object[]{"17", new BigDecimal("200.00")}
            ));
        when(conciliacaoTaxaRepository.sumTaxaPraticadaRs(estabelecimentoId, INICIO, FIM))
            .thenReturn(new BigDecimal("120.00"));
        when(conciliacaoTaxaRepository.findMaioresTaxasByCliente(eq(clienteId), any(), any(), any()))
            .thenReturn(List.of());
    }

    private Map<String, String> gerar() {
        return mensagemService.gerar(new MensagemGerarRequest(
            clienteId, estabelecimentoId, INICIO, FIM, "template", 1L)).templateParametros();
    }

    @Test
    @DisplayName("Vendas da semana vem de conciliacao_taxas, não do total de recebimentos")
    void vendasVemDeConciliacaoTaxas() {
        stubVendas();

        Map<String, String> v = gerar();

        assertThat(v.get("totalValorBruto")).isEqualTo("4.000,00");
        assertThat(v.get("totalValorBruto")).isNotEqualTo("56.940,38");
        verify(conciliacaoTaxaRepository).sumValorBruto(estabelecimentoId, INICIO, FIM);
    }

    @Test
    @DisplayName("Média/dia é derivada das vendas, sobre os dias corridos do período")
    void mediaDiaDerivaDasVendas() {
        stubVendas();

        // 4.000,00 / 5 dias (01 a 05 inclusive) = 800,00
        assertThat(gerar().get("mediaVendas")).isEqualTo("800,00");
    }

    @Test
    @DisplayName("Líquido previsto = vendas − taxas praticadas, e não o líquido de recebimentos")
    void liquidoPrevistoDerivaDasVendas() {
        stubVendas();

        Map<String, String> v = gerar();

        // 4.000,00 − 120,00 = 3.880,00
        assertThat(v.get("liquidoPrevisto")).isEqualTo("3.880,00");
        assertThat(v.get("liquidoPrevisto")).isNotEqualTo("56.940,38");
    }

    @Test
    @DisplayName("O mix classifica as quatro modalidades pelos códigos reais da Conciflex")
    void mixPorCodigoDeModalidade() {
        stubVendas();

        Map<String, String> v = gerar();

        assertThat(v.get("totalDebito")).isEqualTo("1.000,00");
        assertThat(v.get("totalCredito")).isEqualTo("2.000,00");
        assertThat(v.get("totalPix")).isEqualTo("500,00");
        assertThat(v.get("totalVouchers")).isEqualTo("300,00");
    }

    @Test
    @DisplayName("DECISÃO 1: Parcelado (17) fica fora do mix e não vira categoria nova")
    void parceladoExcluidoDoRelatorio() {
        stubVendas();

        Map<String, String> v = gerar();

        // O valor de 200,00 do código 17 não pode aparecer em nenhuma das quatro linhas.
        assertThat(v.get("totalDebito")).isEqualTo("1.000,00");
        assertThat(v.get("totalCredito")).isEqualTo("2.000,00");   // não virou 2.200,00
        assertThat(v.get("totalPix")).isEqualTo("500,00");
        assertThat(v.get("totalVouchers")).isEqualTo("300,00");

        // Nenhuma variável de template carrega o Parcelado, sob qualquer nome.
        assertThat(v).doesNotContainValue("200,00");
        assertThat(v.keySet()).noneMatch(k -> k.toLowerCase().contains("parcel"));

        // O mix soma 3.800,00 contra 4.000,00 de vendas: a diferença é exatamente o
        // Parcelado, deliberadamente fora do relatório.
        assertThat(v.get("totalValorBruto")).isEqualTo("4.000,00");
    }

    @Test
    @DisplayName("DECISÃO 2: líquido previsto de 1.000,00 com 25,00 de taxa é 975,00")
    void formulaAprovadaDoLiquidoPrevisto() {
        when(conciliacaoTaxaRepository.sumValorBruto(estabelecimentoId, INICIO, FIM))
            .thenReturn(new BigDecimal("1000.00"));
        when(conciliacaoTaxaRepository.sumTaxaPraticadaRs(estabelecimentoId, INICIO, FIM))
            .thenReturn(new BigDecimal("25.00"));
        when(conciliacaoTaxaRepository.sumValorBrutoPorModalidade(estabelecimentoId, INICIO, FIM))
            .thenReturn(List.<Object[]>of(new Object[]{"0", new BigDecimal("1000.00")}));
        when(conciliacaoTaxaRepository.findMaioresTaxasByCliente(eq(clienteId), any(), any(), any()))
            .thenReturn(List.of());

        Map<String, String> v = gerar();

        assertThat(v.get("totalValorBruto")).isEqualTo("1.000,00");
        assertThat(v.get("totalTaxaPraticadaRS")).isEqualTo("25,00");
        assertThat(v.get("liquidoPrevisto")).isEqualTo("975,00");
    }

    @Test
    @DisplayName("Taxas continua vindo de sumTaxaPraticadaRs sobre conciliacao_taxas")
    void taxasPreservadas() {
        stubVendas();

        assertThat(gerar().get("totalTaxaPraticadaRS")).isEqualTo("120,00");
        verify(conciliacaoTaxaRepository).sumTaxaPraticadaRs(estabelecimentoId, INICIO, FIM);
    }

    @Test
    @DisplayName("Trocar completamente os recebimentos não move nenhum indicador de venda")
    void recebimentosNaoInfluenciamVendas() {
        stubVendas();
        Map<String, String> antes = gerar();

        when(recebimentoService.resumo(eq(estabelecimentoId), any(), any()))
            .thenReturn(new RecebimentoResumoResponse(
                new BigDecimal("999999.99"), new BigDecimal("88888.88"), List.of(), List.of()));

        Map<String, String> depois = gerar();

        assertThat(depois.get("totalValorBruto")).isEqualTo(antes.get("totalValorBruto"));
        assertThat(depois.get("mediaVendas")).isEqualTo(antes.get("mediaVendas"));
        assertThat(depois.get("liquidoPrevisto")).isEqualTo(antes.get("liquidoPrevisto"));
        assertThat(depois.get("totalPix")).isEqualTo(antes.get("totalPix"));
        assertThat(depois.get("totalDebito")).isEqualTo(antes.get("totalDebito"));
        assertThat(depois.get("totalCredito")).isEqualTo(antes.get("totalCredito"));
        assertThat(depois.get("totalVouchers")).isEqualTo(antes.get("totalVouchers"));
    }

    @Test
    @DisplayName("Gerar o relatório é leitura pura: nada é enviado nem persistido")
    void geracaoNaoEscreve() {
        stubVendas();

        gerar();

        verify(mensagemEnviadaRepository, never()).save(any());
        verify(conciliacaoTaxaRepository, never()).save(any());
        verifyNoInteractions(whatsAppService);
        verifyNoInteractions(anthropicService);
    }

    @Test
    @DisplayName("Período sem vendas produz zeros, sem herdar valores de recebimentos")
    void periodoSemVendas() {
        when(conciliacaoTaxaRepository.sumValorBruto(estabelecimentoId, INICIO, FIM))
            .thenReturn(BigDecimal.ZERO);
        when(conciliacaoTaxaRepository.sumValorBrutoPorModalidade(estabelecimentoId, INICIO, FIM))
            .thenReturn(List.of());
        when(conciliacaoTaxaRepository.sumTaxaPraticadaRs(estabelecimentoId, INICIO, FIM))
            .thenReturn(BigDecimal.ZERO);
        when(conciliacaoTaxaRepository.findMaioresTaxasByCliente(eq(clienteId), any(), any(), any()))
            .thenReturn(List.of());

        Map<String, String> v = gerar();

        assertThat(v.get("totalValorBruto")).isEqualTo("0,00");
        assertThat(v.get("liquidoPrevisto")).isEqualTo("0,00");
        assertThat(v.get("mediaVendas")).isEqualTo("0,00");
        assertThat(v.get("totalPix")).isEqualTo("0,00");
        assertThat(v.get("totalDebito")).isEqualTo("0,00");
        assertThat(v.get("totalCredito")).isEqualTo("0,00");
        assertThat(v.get("totalVouchers")).isEqualTo("0,00");
    }
}
