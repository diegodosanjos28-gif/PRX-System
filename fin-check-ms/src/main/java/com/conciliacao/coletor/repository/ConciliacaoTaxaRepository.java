package com.conciliacao.coletor.repository;

import com.conciliacao.coletor.entity.ConciliacaoTaxa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface ConciliacaoTaxaRepository extends JpaRepository<ConciliacaoTaxa, UUID> {

    /**
     * UPSERT idempotente pela chave lógica de negócio, com preservação automática
     * da versão anterior no histórico quando houver alteração financeira real.
     *
     * <h2>Por que a chave mudou</h2>
     * A API Conciflex ({@code /conciliacao-taxas/buscar}) monta as linhas agregadas
     * no momento da consulta e atribui um {@code id_conciflex} novo a cada execução.
     * O UPSERT anterior conflitava por {@code (id_conciflex, estabelecimento_id)} e,
     * por isso, nunca encontrava conflito: cada recoleta inseria uma linha nova e a
     * tabela acumulava snapshots, inflando qualquer {@code SUM(valor_bruto)}.
     *
     * <p>A identidade real de um agrupamento é
     * {@code (estabelecimento_id, data_venda, codigo_adquirente, cod_bandeira,
     * codigo_modalidade, codigo_produto, auditada)} — constraint
     * {@code uq_ct_chave_logica}, criada pela migration V25.
     *
     * <h2>Anatomia da CTE</h2>
     * As três CTEs abaixo compõem um único statement. O PostgreSQL executa todas
     * sob o <b>mesmo snapshot</b>, então nenhuma enxerga os efeitos da outra sobre
     * as tabelas alvo. Isso é o que torna a preservação do histórico possível sem
     * um SELECT separado no service.
     *
     * <ol>
     *   <li><b>{@code antiga}</b> — lê a linha vigente da chave lógica. Por
     *       compartilhar o snapshot do statement, enxerga sempre o estado
     *       <i>anterior</i> ao UPSERT, independentemente da ordem de execução
     *       escolhida pelo planner.
     *
     *       <p><b>Sem {@code FOR UPDATE}, deliberadamente.</b> {@code FOR UPDATE}
     *       rompe a semântica de snapshot: em READ COMMITTED ele segue a cadeia
     *       de versões da linha e devolve a versão já modificada por
     *       {@code upserted} dentro do mesmo comando. O efeito é que
     *       {@code antiga} passaria a enxergar exatamente os valores novos, a
     *       comparação nunca acusaria diferença e <i>nenhuma</i> versão histórica
     *       seria gravada. Comportamento confirmado empiricamente em PostgreSQL 16
     *       antes desta implementação.</li>
     *
     *   <li><b>{@code upserted}</b> — o {@code INSERT ... ON CONFLICT DO UPDATE}
     *       propriamente dito. Atualiza todos os campos mutáveis, inclusive
     *       {@code id_conciflex} e {@code coletado_em}. Os sete campos da chave
     *       lógica não são tocados: são a identidade da linha.</li>
     *
     *   <li><b>INSERT final no histórico</b> — grava a versão anterior apenas
     *       quando {@code antiga} existe (não é uma linha nova) <i>e</i> ao menos
     *       um campo relevante mudou.</li>
     * </ol>
     *
     * <h2>Critério de "alteração real"</h2>
     * A comparação usa {@code IS DISTINCT FROM}, que trata {@code NULL} como um
     * valor comparável — {@code NULL <> NULL} devolve {@code NULL} (falso-ish) e
     * silenciaria mudanças de e para nulo.
     *
     * <p>Campos comparados:
     * <ul>
     *   <li><b>Financeiros:</b> valor_bruto, valor_desconto, taxa_praticada_rs,
     *       taxa_praticada_cadastrada_rs, taxa_contratada_rs,
     *       total_taxa_nao_contratada_rs, perda_rs, perda</li>
     *   <li><b>Quantitativos:</b> quantidade, percentual_taxa, taxa_contratada</li>
     *   <li><b>Descritivos:</b> adquirente, bandeira, modalidade, produto,
     *       codigo_empresa, estabelecimento_conciflex — uma reclassificação é uma
     *       alteração auditável (o mesmo {@code codigo_adquirente} '108' já
     *       apareceu como 'PagSeguro - 108' e 'PagSeguro | PagBank - 108')</li>
     * </ul>
     *
     * <p>Deliberadamente <b>fora</b> da comparação:
     * <ul>
     *   <li>{@code id_conciflex} — muda a cada coleta por construção da API;
     *       sozinho não representa alteração financeira</li>
     *   <li>{@code coletado_em} — metadado de coleta, muda sempre</li>
     * </ul>
     *
     * <p>Consequência prática: uma recoleta idêntica com novo {@code id_conciflex}
     * e novo {@code coletado_em} atualiza esses dois metadados na linha
     * operacional e <b>não</b> gera linha histórica.
     *
     * <h2>Atomicidade</h2>
     * Statement único sob {@code @Transactional}: ou a linha operacional é
     * atualizada e o histórico gravado, ou nada persiste. Não há janela em que o
     * histórico exista sem a atualização, nem o inverso.
     *
     * <h2>Concorrência</h2>
     * A garantia contra duplicidade é a constraint {@code uq_ct_chave_logica}
     * somada ao {@code ON CONFLICT}: transações concorrentes sobre a mesma chave
     * serializam no índice único e o resultado é sempre <b>uma única linha
     * vigente</b>. Nenhuma linha operacional se perde ou se duplica.
     *
     * <p>Limitação conhecida, restrita ao histórico: se duas transações
     * atualizarem a mesma chave em paralelo, ambas leram {@code antiga} sob seus
     * próprios snapshots e podem arquivar a mesma versão anterior — a versão
     * intermediária gravada pela primeira não é preservada. O estado vigente
     * permanece correto; apenas um degrau da linha do tempo fica ausente.
     *
     * <p>O risco é baixo na prática: o coletor processa um estabelecimento por
     * vez e a colisão exigiria uma coleta manual sobrepondo a agendada para o
     * mesmo estabelecimento, data de venda, adquirente, bandeira, modalidade e
     * produto. Eliminá-lo exigiria travar a linha antes do statement, o que
     * demandaria um SELECT separado no service — descartado por decisão de
     * arquitetura.
     */
    @Modifying
    @Transactional
    @Query(value = """
        WITH antiga AS (
            SELECT *
            FROM conciliacao_taxas
            WHERE estabelecimento_id  =                 :estabelecimentoId
              AND data_venda          IS NOT DISTINCT FROM CAST(:dataVenda AS DATE)
              AND codigo_adquirente   IS NOT DISTINCT FROM CAST(:codigoAdquirente AS VARCHAR)
              AND cod_bandeira        IS NOT DISTINCT FROM CAST(:codBandeira AS VARCHAR)
              AND codigo_modalidade   IS NOT DISTINCT FROM CAST(:codigoModalidade AS VARCHAR)
              AND codigo_produto      IS NOT DISTINCT FROM CAST(:codigoProduto AS VARCHAR)
              AND auditada            IS NOT DISTINCT FROM CAST(:auditada AS VARCHAR)
        ),
        upserted AS (
            INSERT INTO conciliacao_taxas (
                id, estabelecimento_id, id_conciflex, codigo_empresa,
                data_venda, adquirente, codigo_adquirente, bandeira, cod_bandeira,
                modalidade, codigo_modalidade, produto, codigo_produto,
                valor_bruto, valor_desconto, percentual_taxa, taxa_contratada,
                quantidade, taxa_praticada_rs, taxa_praticada_cadastrada_rs,
                taxa_contratada_rs, total_taxa_nao_contratada_rs, perda_rs, perda,
                auditada, estabelecimento_conciflex, coletado_em
            ) VALUES (
                :id, :estabelecimentoId, :idConciflex, :codigoEmpresa,
                CAST(:dataVenda AS DATE), :adquirente, CAST(:codigoAdquirente AS VARCHAR),
                :bandeira, CAST(:codBandeira AS VARCHAR),
                :modalidade, CAST(:codigoModalidade AS VARCHAR), :produto,
                CAST(:codigoProduto AS VARCHAR),
                :valorBruto, :valorDesconto, :percentualTaxa, :taxaContratada,
                :quantidade, :taxaPraticadaRs, :taxaPraticadaCadastradaRs,
                :taxaContratadaRs, :totalTaxaNaoContratadaRs, :perdaRs, :perda,
                CAST(:auditada AS VARCHAR), :estabelecimentoConciflex, :coletadoEm
            )
            ON CONFLICT (
                estabelecimento_id, data_venda, codigo_adquirente, cod_bandeira,
                codigo_modalidade, codigo_produto, auditada
            )
            DO UPDATE SET
                id_conciflex                 = EXCLUDED.id_conciflex,
                codigo_empresa               = EXCLUDED.codigo_empresa,
                adquirente                   = EXCLUDED.adquirente,
                bandeira                     = EXCLUDED.bandeira,
                modalidade                   = EXCLUDED.modalidade,
                produto                      = EXCLUDED.produto,
                valor_bruto                  = EXCLUDED.valor_bruto,
                valor_desconto               = EXCLUDED.valor_desconto,
                percentual_taxa              = EXCLUDED.percentual_taxa,
                taxa_contratada              = EXCLUDED.taxa_contratada,
                quantidade                   = EXCLUDED.quantidade,
                taxa_praticada_rs            = EXCLUDED.taxa_praticada_rs,
                taxa_praticada_cadastrada_rs = EXCLUDED.taxa_praticada_cadastrada_rs,
                taxa_contratada_rs           = EXCLUDED.taxa_contratada_rs,
                total_taxa_nao_contratada_rs = EXCLUDED.total_taxa_nao_contratada_rs,
                perda_rs                     = EXCLUDED.perda_rs,
                perda                        = EXCLUDED.perda,
                estabelecimento_conciflex    = EXCLUDED.estabelecimento_conciflex,
                coletado_em                  = EXCLUDED.coletado_em
            RETURNING id
        )
        INSERT INTO conciliacao_taxas_historico (
            conciliacao_taxa_id_original, estabelecimento_id, id_conciflex, codigo_empresa,
            data_venda, adquirente, codigo_adquirente, bandeira, cod_bandeira,
            modalidade, codigo_modalidade, produto, codigo_produto,
            valor_bruto, valor_desconto, percentual_taxa, taxa_contratada,
            quantidade, taxa_praticada_rs, taxa_praticada_cadastrada_rs,
            taxa_contratada_rs, total_taxa_nao_contratada_rs, perda_rs, perda,
            auditada, estabelecimento_conciflex, coletado_em, motivo_arquivamento
        )
        SELECT
            a.id, a.estabelecimento_id, a.id_conciflex, a.codigo_empresa,
            a.data_venda, a.adquirente, a.codigo_adquirente, a.bandeira, a.cod_bandeira,
            a.modalidade, a.codigo_modalidade, a.produto, a.codigo_produto,
            a.valor_bruto, a.valor_desconto, a.percentual_taxa, a.taxa_contratada,
            a.quantidade, a.taxa_praticada_rs, a.taxa_praticada_cadastrada_rs,
            a.taxa_contratada_rs, a.total_taxa_nao_contratada_rs, a.perda_rs, a.perda,
            a.auditada, a.estabelecimento_conciflex, a.coletado_em, 'UPSERT_VALUE_CHANGE'
        FROM antiga a
        WHERE EXISTS (SELECT 1 FROM upserted)
          AND (
                 a.valor_bruto                  IS DISTINCT FROM :valorBruto
              OR a.valor_desconto               IS DISTINCT FROM :valorDesconto
              OR a.percentual_taxa              IS DISTINCT FROM :percentualTaxa
              OR a.taxa_contratada              IS DISTINCT FROM :taxaContratada
              OR a.quantidade                   IS DISTINCT FROM :quantidade
              OR a.taxa_praticada_rs            IS DISTINCT FROM :taxaPraticadaRs
              OR a.taxa_praticada_cadastrada_rs IS DISTINCT FROM :taxaPraticadaCadastradaRs
              OR a.taxa_contratada_rs           IS DISTINCT FROM :taxaContratadaRs
              OR a.total_taxa_nao_contratada_rs IS DISTINCT FROM :totalTaxaNaoContratadaRs
              OR a.perda_rs                     IS DISTINCT FROM :perdaRs
              OR a.perda                        IS DISTINCT FROM :perda
              OR a.adquirente                   IS DISTINCT FROM CAST(:adquirente AS VARCHAR)
              OR a.bandeira                     IS DISTINCT FROM CAST(:bandeira AS VARCHAR)
              OR a.modalidade                   IS DISTINCT FROM CAST(:modalidade AS VARCHAR)
              OR a.produto                      IS DISTINCT FROM CAST(:produto AS VARCHAR)
              OR a.codigo_empresa               IS DISTINCT FROM CAST(:codigoEmpresa AS VARCHAR)
              OR a.estabelecimento_conciflex    IS DISTINCT FROM CAST(:estabelecimentoConciflex AS VARCHAR)
          )
        """, nativeQuery = true)
    void upsert(
        @Param("id")                        UUID id,
        @Param("estabelecimentoId")         UUID estabelecimentoId,
        @Param("idConciflex")               String idConciflex,
        @Param("codigoEmpresa")             String codigoEmpresa,
        @Param("dataVenda")                 LocalDate dataVenda,
        @Param("adquirente")                String adquirente,
        @Param("codigoAdquirente")          String codigoAdquirente,
        @Param("bandeira")                  String bandeira,
        @Param("codBandeira")               String codBandeira,
        @Param("modalidade")                String modalidade,
        @Param("codigoModalidade")          String codigoModalidade,
        @Param("produto")                   String produto,
        @Param("codigoProduto")             String codigoProduto,
        @Param("valorBruto")                BigDecimal valorBruto,
        @Param("valorDesconto")             BigDecimal valorDesconto,
        @Param("percentualTaxa")            BigDecimal percentualTaxa,
        @Param("taxaContratada")            BigDecimal taxaContratada,
        @Param("quantidade")                Integer quantidade,
        @Param("taxaPraticadaRs")           BigDecimal taxaPraticadaRs,
        @Param("taxaPraticadaCadastradaRs") BigDecimal taxaPraticadaCadastradaRs,
        @Param("taxaContratadaRs")          BigDecimal taxaContratadaRs,
        @Param("totalTaxaNaoContratadaRs")  BigDecimal totalTaxaNaoContratadaRs,
        @Param("perdaRs")                   BigDecimal perdaRs,
        @Param("perda")                     BigDecimal perda,
        @Param("auditada")                  String auditada,
        @Param("estabelecimentoConciflex")  String estabelecimentoConciflex,
        @Param("coletadoEm")                LocalDateTime coletadoEm
    );
}
