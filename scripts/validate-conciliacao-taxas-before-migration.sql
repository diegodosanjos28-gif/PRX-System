-- ============================================================================
-- PRÉ-VALIDAÇÃO — executar ANTES de aplicar a migration V25
-- ============================================================================
-- SOMENTE LEITURA. Nenhum comando altera dados ou estrutura.
--
-- Uso:
--   docker exec -i conciliacao-postgres psql -U conciliacao -d conciliacao \
--     -f - < scripts/validate-conciliacao-taxas-before-migration.sql
--
-- Guarde a saída: ela é a linha de base para comparar com a pós-validação.
-- ============================================================================

\echo '=== 1. VOLUME E DEDUPLICAÇÃO ESPERADA ==='
SELECT
    COUNT(*)                                                        AS linhas_totais,
    COUNT(DISTINCT (estabelecimento_id, data_venda, codigo_adquirente,
                    cod_bandeira, codigo_modalidade, codigo_produto,
                    auditada))                                      AS grupos_logicos,
    COUNT(*) - COUNT(DISTINCT (estabelecimento_id, data_venda, codigo_adquirente,
                    cod_bandeira, codigo_modalidade, codigo_produto,
                    auditada))                                      AS excedentes_a_arquivar,
    pg_size_pretty(pg_total_relation_size('conciliacao_taxas'))      AS tamanho_tabela
FROM conciliacao_taxas;

\echo ''
\echo '=== 2. DISTRIBUIÇÃO DE VERSÕES POR GRUPO ==='
SELECT versoes, COUNT(*) AS qtd_grupos
FROM (
    SELECT COUNT(*) AS versoes
    FROM conciliacao_taxas
    GROUP BY estabelecimento_id, data_venda, codigo_adquirente, cod_bandeira,
             codigo_modalidade, codigo_produto, auditada
) s
GROUP BY versoes
ORDER BY versoes;

\echo ''
\echo '=== 3. COLISÕES DENTRO DO MESMO LOTE (BLOQUEANTE SE > 0) ==='
-- Duas linhas do MESMO snapshot com a mesma chave lógica significariam que a
-- chave é incompleta e o saneamento fundiria registros financeiramente distintos.
SELECT COUNT(*) AS colisoes_no_mesmo_lote
FROM (
    SELECT 1
    FROM conciliacao_taxas
    GROUP BY estabelecimento_id, data_venda, codigo_adquirente, cod_bandeira,
             codigo_modalidade, codigo_produto, auditada, coletado_em
    HAVING COUNT(*) > 1
) c;

\echo ''
\echo '=== 4. NULOS E VAZIOS NOS CAMPOS DA CHAVE ==='
-- A constraint usa NULLS NOT DISTINCT, então nulos não a invalidam.
-- Ainda assim, valores inesperados aqui merecem investigação antes de prosseguir.
SELECT
    COUNT(*) FILTER (WHERE data_venda        IS NULL)                       AS data_venda_nula,
    COUNT(*) FILTER (WHERE codigo_adquirente IS NULL)                       AS cod_adquirente_nulo,
    COUNT(*) FILTER (WHERE cod_bandeira      IS NULL)                       AS cod_bandeira_nulo,
    COUNT(*) FILTER (WHERE codigo_modalidade IS NULL)                       AS cod_modalidade_nulo,
    COUNT(*) FILTER (WHERE codigo_produto    IS NULL)                       AS cod_produto_nulo,
    COUNT(*) FILTER (WHERE auditada          IS NULL)                       AS auditada_nula,
    COUNT(*) FILTER (WHERE TRIM(COALESCE(codigo_adquirente, '')) = '')      AS cod_adquirente_vazio,
    COUNT(*) FILTER (WHERE TRIM(COALESCE(cod_bandeira, ''))      = '')      AS cod_bandeira_vazio,
    COUNT(*) FILTER (WHERE TRIM(COALESCE(codigo_modalidade, '')) = '')      AS cod_modalidade_vazio,
    COUNT(*) FILTER (WHERE TRIM(COALESCE(codigo_produto, ''))    = '')      AS cod_produto_vazio,
    COUNT(*) FILTER (WHERE coletado_em       IS NULL)                       AS coletado_em_nulo
FROM conciliacao_taxas;

\echo ''
\echo '=== 5. GRUPOS COM auditada S E N SIMULTANEAMENTE ==='
-- Justifica a presença de auditada na chave lógica. Se > 0, removê-la da chave
-- causaria perda de dados.
SELECT COUNT(*) AS grupos_com_S_e_N
FROM (
    SELECT 1
    FROM conciliacao_taxas
    GROUP BY estabelecimento_id, data_venda, codigo_adquirente, cod_bandeira,
             codigo_modalidade, codigo_produto
    HAVING COUNT(DISTINCT auditada) > 1
) s;

\echo ''
\echo '=== 6. TOTAIS FINANCEIROS — BRUTO vs VIGENTE ==='
-- soma_bruta_atual é o valor inflado que os relatórios enxergam hoje.
-- soma_vigente_esperada é o que passarão a enxergar após a V25.
SELECT
    (SELECT COALESCE(SUM(valor_bruto), 0) FROM conciliacao_taxas)   AS soma_bruta_atual,
    (SELECT COALESCE(SUM(valor_bruto), 0) FROM (
        SELECT DISTINCT ON (estabelecimento_id, data_venda, codigo_adquirente,
                            cod_bandeira, codigo_modalidade, codigo_produto, auditada)
               valor_bruto
        FROM conciliacao_taxas
        ORDER BY estabelecimento_id, data_venda, codigo_adquirente, cod_bandeira,
                 codigo_modalidade, codigo_produto, auditada,
                 coletado_em DESC NULLS LAST, id DESC
    ) v)                                                            AS soma_vigente_esperada;

\echo ''
\echo '=== 7. TOTAIS POR ESTABELECIMENTO E MÊS (AMOSTRA) ==='
SELECT
    e.descricao,
    date_trunc('month', ct.data_venda)::date            AS mes,
    COUNT(*)                                            AS linhas,
    COALESCE(SUM(ct.valor_bruto), 0)                    AS soma_bruta_atual
FROM conciliacao_taxas ct
JOIN estabelecimentos e ON e.id = ct.estabelecimento_id
GROUP BY e.descricao, date_trunc('month', ct.data_venda)
ORDER BY mes DESC, soma_bruta_atual DESC
LIMIT 20;

\echo ''
\echo '=== 8. ESTRUTURA — CONSTRAINTS ==='
SELECT conname AS constraint_name, contype AS tipo,
       pg_get_constraintdef(oid) AS definicao
FROM pg_constraint
WHERE conrelid = 'public.conciliacao_taxas'::regclass
ORDER BY conname;

\echo ''
\echo '=== 9. ESTRUTURA — ÍNDICES ==='
SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public' AND tablename = 'conciliacao_taxas'
ORDER BY indexname;

\echo ''
\echo '=== 10. FOREIGN KEYS APONTANDO PARA conciliacao_taxas (ESPERADO: 0 LINHAS) ==='
SELECT tc.table_name AS tabela_origem, kcu.column_name AS coluna, tc.constraint_name
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage       kcu ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage ccu ON tc.constraint_name = ccu.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY' AND ccu.table_name = 'conciliacao_taxas';

\echo ''
\echo '=== 11. PRÉ-REQUISITOS DA MIGRATION ==='
SELECT
    current_setting('server_version')                                    AS versao_postgres,
    (current_setting('server_version_num')::int >= 150000)               AS suporta_nulls_not_distinct,
    EXISTS (SELECT 1 FROM pg_constraint
            WHERE conname = 'uq_ct_id_conciflex_estabelecimento'
              AND conrelid = 'public.conciliacao_taxas'::regclass)       AS constraint_antiga_presente,
    NOT EXISTS (SELECT 1 FROM pg_constraint
            WHERE conname = 'uq_ct_chave_logica'
              AND conrelid = 'public.conciliacao_taxas'::regclass)       AS constraint_nova_ausente,
    NOT EXISTS (SELECT 1 FROM information_schema.tables
            WHERE table_schema = 'public'
              AND table_name = 'conciliacao_taxas_historico')            AS historico_ainda_nao_existe;

\echo ''
\echo '=== 12. ÚLTIMA MIGRATION APLICADA (ESPERADO: 24) ==='
SELECT version, description, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 3;

\echo ''
\echo '============================================================'
\echo 'CRITÉRIOS PARA LIBERAR A MIGRATION:'
\echo '  [3]  colisoes_no_mesmo_lote            = 0'
\echo '  [10] nenhuma foreign key listada'
\echo '  [11] todas as colunas booleanas        = t'
\echo '  [12] ultima versao aplicada            = 24'
\echo 'Anote [1] e [6]: serão comparados na pós-validação.'
\echo '============================================================'
