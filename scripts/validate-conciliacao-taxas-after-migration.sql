-- ============================================================================
-- PÓS-VALIDAÇÃO — executar DEPOIS de aplicar a migration V25
-- ============================================================================
-- SOMENTE LEITURA. Nenhum comando altera dados ou estrutura.
--
-- Uso:
--   docker exec -i conciliacao-postgres psql -U conciliacao -d conciliacao \
--     -f - < scripts/validate-conciliacao-taxas-after-migration.sql
--
-- Compare a saída com a da pré-validação.
-- ============================================================================

\echo '=== 1. ZERO DUPLICIDADES (BLOQUEANTE SE > 0) ==='
SELECT COUNT(*) AS grupos_duplicados
FROM (
    SELECT 1
    FROM conciliacao_taxas
    GROUP BY estabelecimento_id, data_venda, codigo_adquirente, cod_bandeira,
             codigo_modalidade, codigo_produto, auditada
    HAVING COUNT(*) > 1
) d;

\echo ''
\echo '=== 2. UMA LINHA POR GRUPO LÓGICO (as três colunas devem ser IGUAIS) ==='
SELECT
    COUNT(*)                                                        AS linhas_operacionais,
    COUNT(DISTINCT (estabelecimento_id, data_venda, codigo_adquirente,
                    cod_bandeira, codigo_modalidade, codigo_produto,
                    auditada))                                      AS grupos_logicos,
    COUNT(DISTINCT id)                                              AS ids_distintos
FROM conciliacao_taxas;

\echo ''
\echo '=== 3. CONSERVAÇÃO DE LINHAS ==='
-- operacionais + arquivadas_pela_migration deve reproduzir o total da pré-validação [1].
SELECT
    (SELECT COUNT(*) FROM conciliacao_taxas)                        AS operacionais,
    (SELECT COUNT(*) FROM conciliacao_taxas_historico
     WHERE motivo_arquivamento = 'MIGRATION_DEDUPLICATION')         AS arquivadas_pela_migration,
    (SELECT COUNT(*) FROM conciliacao_taxas)
  + (SELECT COUNT(*) FROM conciliacao_taxas_historico
     WHERE motivo_arquivamento = 'MIGRATION_DEDUPLICATION')         AS total_reconstituido,
    (SELECT COUNT(*) FROM conciliacao_taxas_historico
     WHERE motivo_arquivamento = 'UPSERT_VALUE_CHANGE')             AS arquivadas_por_recoleta;

\echo ''
\echo '=== 4. SOMA FINANCEIRA VIGENTE ==='
-- Deve ser IDÊNTICA a soma_vigente_esperada da pré-validação [6].
SELECT
    COALESCE(SUM(valor_bruto), 0)                   AS soma_vigente_operacional,
    COALESCE(SUM(quantidade), 0)                    AS transacoes,
    COUNT(*)                                        AS linhas
FROM conciliacao_taxas;

\echo ''
\echo '=== 5. TOTAIS POR ESTABELECIMENTO E MÊS (comparar com pré-validação [7]) ==='
SELECT
    e.descricao,
    date_trunc('month', ct.data_venda)::date        AS mes,
    COUNT(*)                                        AS linhas,
    COALESCE(SUM(ct.valor_bruto), 0)                AS soma_vigente
FROM conciliacao_taxas ct
JOIN estabelecimentos e ON e.id = ct.estabelecimento_id
GROUP BY e.descricao, date_trunc('month', ct.data_venda)
ORDER BY mes DESC, soma_vigente DESC
LIMIT 20;

\echo ''
\echo '=== 6. NOVA CONSTRAINT ATIVA ==='
-- Esperado: uq_ct_chave_logica presente com NULLS NOT DISTINCT sobre as 7 colunas;
-- uq_ct_id_conciflex_estabelecimento ausente.
SELECT conname AS constraint_name, pg_get_constraintdef(oid) AS definicao
FROM pg_constraint
WHERE conrelid = 'public.conciliacao_taxas'::regclass
ORDER BY conname;

\echo ''
\echo '=== 7. ESTRUTURA DA TABELA HISTÓRICA ==='
SELECT
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema='public' AND table_name='conciliacao_taxas_historico')  AS colunas,
    (SELECT COUNT(*) FROM pg_indexes
     WHERE schemaname='public' AND tablename='conciliacao_taxas_historico')     AS indices,
    EXISTS (SELECT 1 FROM pg_constraint
            WHERE conname = 'ck_cth_motivo_arquivamento')                       AS check_motivo_presente;

\echo ''
\echo '=== 8. INTEGRIDADE REFERENCIAL ==='
SELECT
    (SELECT COUNT(*) FROM conciliacao_taxas_historico h
     WHERE NOT EXISTS (SELECT 1 FROM estabelecimentos e
                       WHERE e.id = h.estabelecimento_id))                      AS historico_orfao,
    (SELECT COUNT(*) FROM conciliacao_taxas ct
     WHERE NOT EXISTS (SELECT 1 FROM estabelecimentos e
                       WHERE e.id = ct.estabelecimento_id))                     AS operacional_orfao,
    (SELECT COUNT(*) FROM conciliacao_taxas_historico
     WHERE motivo_arquivamento NOT IN
           ('MIGRATION_DEDUPLICATION', 'UPSERT_VALUE_CHANGE'))                  AS motivos_invalidos;

\echo ''
\echo '=== 9. AMOSTRA — LINHA DO TEMPO DE UM GRUPO ARQUIVADO ==='
WITH alvo AS (
    SELECT estabelecimento_id, data_venda, codigo_adquirente, cod_bandeira,
           codigo_modalidade, codigo_produto, auditada
    FROM conciliacao_taxas_historico
    GROUP BY 1,2,3,4,5,6,7
    ORDER BY COUNT(*) DESC
    LIMIT 1
)
SELECT 'HISTORICO' AS origem, h.id_conciflex, h.valor_bruto, h.quantidade,
       h.coletado_em, h.motivo_arquivamento
FROM conciliacao_taxas_historico h JOIN alvo a USING
    (estabelecimento_id, data_venda, codigo_adquirente, cod_bandeira,
     codigo_modalidade, codigo_produto, auditada)
UNION ALL
SELECT 'VIGENTE', c.id_conciflex, c.valor_bruto, c.quantidade, c.coletado_em, '-'
FROM conciliacao_taxas c JOIN alvo a USING
    (estabelecimento_id, data_venda, codigo_adquirente, cod_bandeira,
     codigo_modalidade, codigo_produto, auditada)
ORDER BY origem DESC, coletado_em;

\echo ''
\echo '=== 10. MIGRATION REGISTRADA ==='
SELECT version, description, installed_on, success, execution_time || ' ms' AS duracao
FROM flyway_schema_history
WHERE version = '25';

\echo ''
\echo '=== 11. IDEMPOTÊNCIA DA PRÓXIMA COLETA (executar novamente após a 1a coleta) ==='
-- Coletas seguintes não devem aumentar linhas_operacionais sem novos grupos.
-- Registre o valor e compare após o coletor rodar.
SELECT
    COUNT(*)                                                    AS linhas_operacionais_agora,
    MAX(coletado_em)                                            AS ultima_coleta,
    (SELECT COUNT(*) FROM conciliacao_taxas_historico
     WHERE motivo_arquivamento = 'UPSERT_VALUE_CHANGE')         AS versoes_por_recoleta
FROM conciliacao_taxas;

\echo ''
\echo '============================================================'
\echo 'CRITÉRIOS DE SUCESSO:'
\echo '  [1] grupos_duplicados                = 0'
\echo '  [2] as tres colunas IGUAIS entre si'
\echo '  [3] total_reconstituido              = linhas_totais da pre-validacao [1]'
\echo '  [4] soma_vigente_operacional         = soma_vigente_esperada da pre-validacao [6]'
\echo '  [6] uq_ct_chave_logica presente, uq_ct_id_conciflex_estabelecimento ausente'
\echo '  [8] historico_orfao = operacional_orfao = motivos_invalidos = 0'
\echo '  [10] success = t'
\echo 'Se QUALQUER criterio falhar, execute o rollback documentado no relatorio.'
\echo '============================================================'
