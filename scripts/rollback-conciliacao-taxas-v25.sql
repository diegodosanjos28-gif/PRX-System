-- ============================================================================
-- ROLLBACK da migration V25
-- ============================================================================
-- NÃO EXECUTE SEM LER O RELATÓRIO. Este script restaura o estado anterior à
-- deduplicação, incluindo a reinserção das linhas arquivadas.
--
-- PRINCÍPIO: a tabela conciliacao_taxas_historico NUNCA é apagada. Ela permanece
-- após o rollback como registro de auditoria. Apenas o motivo das linhas
-- restauradas é marcado, para que uma reaplicação futura da V25 não as reprocesse
-- como se fossem versões novas.
--
-- ORDEM DE EXECUÇÃO COMPLETA DO ROLLBACK:
--   1. Parar o coletor          (docker compose stop coletor)
--   2. Reverter o código        (git revert / redeploy da imagem anterior)
--   3. Executar ESTE script
--   4. Remover o registro Flyway (PASSO 6, comentado)
--   5. Subir API e coletor com a versão anterior
--
-- SE PRETENDER REAPLICAR A V25 DEPOIS: veja o PASSO 7 ao final. A tabela
-- histórica e seus índices sobrevivem ao rollback e impedem a reaplicação até
-- serem renomeados — renomear apenas a tabela não é suficiente.
--
-- Executar dentro de uma transação explícita: em caso de erro, COMMIT nunca
-- acontece e o banco permanece intacto.
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- PASSO 0 — Guarda: só faz sentido se a V25 tiver sido aplicada
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_ct_chave_logica'
          AND conrelid = 'public.conciliacao_taxas'::regclass
    ) THEN
        RAISE EXCEPTION
            'Rollback abortado: uq_ct_chave_logica nao existe. A V25 nao esta aplicada.';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'conciliacao_taxas_historico'
    ) THEN
        RAISE EXCEPTION
            'Rollback abortado: conciliacao_taxas_historico nao existe. Estado inconsistente.';
    END IF;
END $$;


-- ----------------------------------------------------------------------------
-- PASSO 1 — Remover a constraint nova
-- Precisa sair antes da reinserção: as linhas restauradas violam a chave lógica
-- por definição (é exatamente por serem duplicadas que foram arquivadas).
-- ----------------------------------------------------------------------------
ALTER TABLE conciliacao_taxas
    DROP CONSTRAINT uq_ct_chave_logica;


-- ----------------------------------------------------------------------------
-- PASSO 2 — Restaurar as linhas arquivadas pela migration
--
-- Restaura APENAS motivo_arquivamento = 'MIGRATION_DEDUPLICATION'.
-- As linhas 'UPSERT_VALUE_CHANGE' são versões substituídas por recoletas
-- posteriores à V25 e não faziam parte da tabela operacional antes dela —
-- restaurá-las criaria dados que nunca existiram no estado original.
--
-- O id original é preservado (conciliacao_taxa_id_original), o que torna a
-- operação repetível: o ON CONFLICT DO NOTHING na PK evita duplicar caso o
-- script seja executado duas vezes.
-- ----------------------------------------------------------------------------
INSERT INTO conciliacao_taxas (
    id, estabelecimento_id, id_conciflex, codigo_empresa,
    data_venda, adquirente, codigo_adquirente, bandeira, cod_bandeira,
    modalidade, codigo_modalidade, produto, codigo_produto,
    valor_bruto, valor_desconto, percentual_taxa, taxa_contratada,
    quantidade, taxa_praticada_rs, taxa_praticada_cadastrada_rs,
    taxa_contratada_rs, total_taxa_nao_contratada_rs, perda_rs, perda,
    auditada, estabelecimento_conciflex, coletado_em
)
SELECT
    h.conciliacao_taxa_id_original, h.estabelecimento_id, h.id_conciflex, h.codigo_empresa,
    h.data_venda, h.adquirente, h.codigo_adquirente, h.bandeira, h.cod_bandeira,
    h.modalidade, h.codigo_modalidade, h.produto, h.codigo_produto,
    h.valor_bruto, h.valor_desconto, h.percentual_taxa, h.taxa_contratada,
    h.quantidade, h.taxa_praticada_rs, h.taxa_praticada_cadastrada_rs,
    h.taxa_contratada_rs, h.total_taxa_nao_contratada_rs, h.perda_rs, h.perda,
    h.auditada, h.estabelecimento_conciflex, h.coletado_em
FROM conciliacao_taxas_historico h
WHERE h.motivo_arquivamento = 'MIGRATION_DEDUPLICATION'
ON CONFLICT (id) DO NOTHING;


-- ----------------------------------------------------------------------------
-- PASSO 3 — Restaurar a constraint anterior
--
-- CONDICIONAL: a constraint antiga exige id_conciflex único por estabelecimento.
-- Se coletas posteriores à V25 tiverem reaproveitado um id_conciflex, ela não
-- pode ser recriada e o rollback falha aqui, de forma explícita.
--
-- Deixe este passo comentado se o objetivo for apenas remover a chave lógica
-- sem restabelecer a antiga (o coletor revertido insere sem ON CONFLICT válido,
-- comportamento idêntico ao anterior à V25).
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    v_conflitos BIGINT;
BEGIN
    SELECT COUNT(*) INTO v_conflitos
    FROM (
        SELECT 1 FROM conciliacao_taxas
        GROUP BY id_conciflex, estabelecimento_id
        HAVING COUNT(*) > 1
    ) c;

    IF v_conflitos > 0 THEN
        RAISE EXCEPTION
            'Rollback abortado no PASSO 3: % pares (id_conciflex, estabelecimento_id) duplicados. '
            'A constraint antiga nao pode ser recriada. Comente este bloco se deseja '
            'prosseguir sem restabelece-la.', v_conflitos;
    END IF;

    EXECUTE 'ALTER TABLE conciliacao_taxas
             ADD CONSTRAINT uq_ct_id_conciflex_estabelecimento
             UNIQUE (id_conciflex, estabelecimento_id)';
END $$;


-- ----------------------------------------------------------------------------
-- PASSO 4 — Marcar as linhas restauradas
--
-- Sem apagá-las: elas voltaram para a tabela operacional, mas o registro de que
-- foram arquivadas pela V25 permanece auditável. O motivo passa a indicar que a
-- linha foi devolvida, de modo que uma reaplicação da V25 as trate como dados
-- operacionais normais e não como histórico pré-existente.
-- ----------------------------------------------------------------------------
ALTER TABLE conciliacao_taxas_historico
    DROP CONSTRAINT ck_cth_motivo_arquivamento;

ALTER TABLE conciliacao_taxas_historico
    ADD CONSTRAINT ck_cth_motivo_arquivamento
    CHECK (motivo_arquivamento IN
           ('MIGRATION_DEDUPLICATION', 'UPSERT_VALUE_CHANGE', 'ROLLBACK_V25_RESTAURADO'));

UPDATE conciliacao_taxas_historico
SET motivo_arquivamento = 'ROLLBACK_V25_RESTAURADO'
WHERE motivo_arquivamento = 'MIGRATION_DEDUPLICATION';


-- ----------------------------------------------------------------------------
-- PASSO 5 — Validação do rollback
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    v_restauradas BIGINT;
    v_operacionais BIGINT;
BEGIN
    SELECT COUNT(*) INTO v_restauradas
    FROM conciliacao_taxas_historico
    WHERE motivo_arquivamento = 'ROLLBACK_V25_RESTAURADO';

    SELECT COUNT(*) INTO v_operacionais FROM conciliacao_taxas;

    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_ct_chave_logica'
          AND conrelid = 'public.conciliacao_taxas'::regclass
    ) THEN
        RAISE EXCEPTION 'Rollback incompleto: uq_ct_chave_logica ainda presente.';
    END IF;

    RAISE NOTICE
        'Rollback concluido: % linhas restauradas, % linhas operacionais. '
        'A tabela conciliacao_taxas_historico foi PRESERVADA.',
        v_restauradas, v_operacionais;
END $$;

COMMIT;

-- Em caso de erro em qualquer passo acima, execute:
--   ROLLBACK;


-- ============================================================================
-- PASSO 6 — Registro do Flyway (executar SEPARADAMENTE, após validar o acima)
-- ============================================================================
-- Sem isto, o Flyway considera a V25 aplicada e não a reexecutará. Com isto,
-- uma futura subida da aplicação tentará aplicá-la de novo — o que só é
-- desejável se o código também tiver sido revertido.
--
-- DELETE FROM flyway_schema_history WHERE version = '25';
--
-- A tabela conciliacao_taxas_historico permanece no banco. Removê-la exigiria
-- DROP TABLE, o que apagaria histórico financeiro — não é feito por este script
-- em nenhuma circunstância.
-- ============================================================================


-- ============================================================================
-- PASSO 7 — REAPLICAR A V25 APÓS UM ROLLBACK
-- ============================================================================
-- Só é necessário se, após o rollback, você pretende aplicar a V25 novamente.
-- Se o rollback for definitivo, ignore esta seção.
--
-- PROBLEMA
-- O rollback preserva conciliacao_taxas_historico. A V25 cria essa tabela e os
-- índices idx_cth_* com CREATE sem IF NOT EXISTS — deliberadamente, para nunca
-- escrever sobre uma estrutura preexistente que possa divergir do modelo
-- esperado. Uma nova tentativa falha com:
--
--   ERROR: relation "conciliacao_taxas_historico" already exists
--
-- A falha ocorre no PASSO 2 da V25, ANTES de qualquer operação destrutiva
-- (nenhuma linha foi arquivada ou removida ainda) e dentro da transação do
-- Flyway. Não há risco de perda de dados — apenas a reaplicação fica bloqueada.
--
-- ATENÇÃO: RENOMEAR SOMENTE A TABELA NÃO BASTA.
-- Os índices acompanham a tabela no RENAME e mantêm os nomes originais. A V25
-- avançaria além do CREATE TABLE e falharia logo adiante, ao recriá-los:
--
--   ERROR: relation "idx_cth_estabelecimento_data" already exists
--
-- Também sem risco de perda de dados (ainda dentro da transação, antes do
-- arquivamento), mas igualmente bloqueante. Comportamento verificado em
-- PostgreSQL 16.
--
-- SOLUÇÃO: RENOMEAR, NUNCA APAGAR
-- Preserve a tabela E os índices sob nomes com timestamp, todos com o MESMO
-- sufixo. Nomes fixos são proibidos aqui: um segundo ciclo de rollback
-- sobrescreveria o histórico preservado pelo primeiro.
--
-- Execute o bloco único abaixo — ele apenas RENOMEIA, não apaga nada — e depois
-- suba a aplicação para que o Flyway reaplique a V25:
--
--   DO $$
--   DECLARE
--       v_sufixo TEXT := to_char(now(), 'YYYYMMDD_HH24MISS');
--       v_idx    TEXT;
--   BEGIN
--       IF NOT EXISTS (
--           SELECT 1 FROM information_schema.tables
--           WHERE table_schema = 'public'
--             AND table_name   = 'conciliacao_taxas_historico'
--       ) THEN
--           RAISE NOTICE 'Nada a renomear: conciliacao_taxas_historico nao existe.';
--           RETURN;
--       END IF;
--
--       -- Índices primeiro, enquanto ainda pertencem à tabela de origem.
--       FOREACH v_idx IN ARRAY ARRAY['idx_cth_estabelecimento_data',
--                                    'idx_cth_chave_logica',
--                                    'idx_cth_arquivado_em']
--       LOOP
--           IF EXISTS (
--               SELECT 1 FROM pg_class c
--               JOIN pg_namespace n ON n.oid = c.relnamespace
--               WHERE c.relname = v_idx AND c.relkind = 'i' AND n.nspname = 'public'
--           ) THEN
--               EXECUTE format('ALTER INDEX %I RENAME TO %I',
--                              v_idx, v_idx || '_backup_' || v_sufixo);
--           END IF;
--       END LOOP;
--
--       EXECUTE format('ALTER TABLE conciliacao_taxas_historico RENAME TO %I',
--                      'conciliacao_taxas_historico_backup_' || v_sufixo);
--
--       RAISE NOTICE
--           'Historico preservado como conciliacao_taxas_historico_backup_% '
--           '(indices com o mesmo sufixo). A V25 pode ser reaplicada.', v_sufixo;
--   END $$;
--
-- O QUE NÃO PRECISA DE AÇÃO (ambos verificados em PostgreSQL 16)
--
--   ck_cth_motivo_arquivamento — nomes de constraint são únicos por TABELA, não
--   por schema. A V25 recria a sua sem colidir com a da tabela de backup.
--
--   Chave primária — seu nome é gerado automaticamente e o PostgreSQL
--   desambigua sozinho. Efeito esperado e inofensivo: a tabela recriada pela
--   V25 recebe a PK chamada conciliacao_taxas_historico_pkey1, enquanto
--   conciliacao_taxas_historico_pkey permanece com a tabela de backup.
--
-- APÓS A REAPLICAÇÃO
-- As tabelas *_backup_<timestamp> continuam consultáveis e devem ser mantidas
-- enquanto houver necessidade de auditoria. A consolidação com o novo histórico,
-- se desejada, é uma decisão de negócio — não a automatize aqui.
--
-- CONSULTA UNIFICADA (exemplo, somente leitura):
--   SELECT * FROM conciliacao_taxas_historico
--   UNION ALL
--   SELECT * FROM conciliacao_taxas_historico_backup_<timestamp>;
-- ============================================================================
