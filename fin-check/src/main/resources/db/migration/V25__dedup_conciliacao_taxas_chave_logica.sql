-- ============================================================================
-- V25 — Deduplicação de conciliacao_taxas pela chave lógica de negócio
-- ============================================================================
--
-- PROBLEMA
-- A API Conciflex (/conciliacao-taxas/buscar) retorna linhas AGREGADAS geradas
-- no momento da consulta. Cada execução atribui um id_conciflex NOVO para o
-- mesmo agrupamento de negócio. A constraint uq_ct_id_conciflex_estabelecimento
-- criada em V10 nunca entra em conflito, então o UPSERT do coletor sempre insere
-- uma nova linha. O resultado é o acúmulo de snapshots históricos na tabela
-- operacional, inflando qualquer SUM(valor_bruto).
--
-- SOLUÇÃO
-- 1. Criar conciliacao_taxas_historico para preservar TODAS as versões antigas.
-- 2. Arquivar as versões excedentes e manter na tabela operacional apenas a
--    versão mais recente de cada chave lógica.
-- 3. Substituir a constraint por uma baseada na chave lógica de negócio.
--
-- CHAVE LÓGICA
--   (estabelecimento_id, data_venda, codigo_adquirente, cod_bandeira,
--    codigo_modalidade, codigo_produto, auditada)
--
-- Usa CÓDIGOS e não nomes descritivos: o mesmo codigo_adquirente '108' aparece
-- na base como 'PagSeguro - 108' e 'PagSeguro | PagBank - 108'. O nome muda, o
-- código não.
--
-- 'auditada' INTEGRA a chave: existem grupos legítimos com auditada='S' e
-- auditada='N' simultaneamente para a mesma combinação, com valores financeiros
-- distintos. Não podem sobrescrever um ao outro.
--
-- NULLS NOT DISTINCT
-- As colunas da chave são NULLABLE no schema (embora os dados auditados tenham
-- zero NULLs). Em PostgreSQL, UNIQUE trata NULL como distinto de NULL, o que
-- tornaria a constraint inefetiva e faria o ON CONFLICT nunca casar para essas
-- linhas. NULLS NOT DISTINCT (PostgreSQL 15+) trata NULLs como iguais, sem
-- exigir COALESCE na chave nem alterar a nulabilidade das colunas — preservando
-- a compatibilidade com hibernate ddl-auto=validate nos dois módulos.
--
-- TRANSAÇÃO
-- Flyway executa esta migration dentro de uma única transação no PostgreSQL.
-- Qualquer RAISE EXCEPTION aborta tudo e reverte integralmente.
-- ============================================================================


-- ----------------------------------------------------------------------------
-- PASSO 0 — Guarda estrutural
-- Falha explicitamente se o schema não corresponder ao modelo auditado.
-- Nenhum IF NOT EXISTS é usado para mascarar divergências.
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    v_faltantes TEXT;
    v_colunas_esperadas TEXT[] := ARRAY[
        'id', 'estabelecimento_id', 'id_conciflex', 'codigo_empresa', 'data_venda',
        'adquirente', 'codigo_adquirente', 'bandeira', 'cod_bandeira',
        'modalidade', 'codigo_modalidade', 'produto', 'codigo_produto',
        'valor_bruto', 'valor_desconto', 'percentual_taxa', 'taxa_contratada',
        'quantidade', 'taxa_praticada_rs', 'taxa_praticada_cadastrada_rs',
        'taxa_contratada_rs', 'total_taxa_nao_contratada_rs', 'perda_rs', 'perda',
        'auditada', 'estabelecimento_conciflex', 'coletado_em'
    ];
BEGIN
    -- A tabela operacional precisa existir
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'conciliacao_taxas'
    ) THEN
        RAISE EXCEPTION 'V25 abortada: tabela conciliacao_taxas nao encontrada.';
    END IF;

    -- Todas as colunas do modelo auditado precisam existir
    SELECT string_agg(c, ', ') INTO v_faltantes
    FROM unnest(v_colunas_esperadas) AS c
    WHERE NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name   = 'conciliacao_taxas'
          AND column_name  = c
    );

    IF v_faltantes IS NOT NULL THEN
        RAISE EXCEPTION 'V25 abortada: colunas ausentes em conciliacao_taxas: %', v_faltantes;
    END IF;

    -- A constraint antiga precisa existir para ser substituída
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_ct_id_conciflex_estabelecimento'
          AND conrelid = 'public.conciliacao_taxas'::regclass
    ) THEN
        RAISE EXCEPTION
            'V25 abortada: constraint uq_ct_id_conciflex_estabelecimento nao encontrada. '
            'O schema divergiu do modelo auditado (esperado apos V10).';
    END IF;

    -- A nova constraint não pode pré-existir
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_ct_chave_logica'
          AND conrelid = 'public.conciliacao_taxas'::regclass
    ) THEN
        RAISE EXCEPTION 'V25 abortada: constraint uq_ct_chave_logica ja existe.';
    END IF;

    -- NULLS NOT DISTINCT exige PostgreSQL 15+
    IF current_setting('server_version_num')::int < 150000 THEN
        RAISE EXCEPTION
            'V25 abortada: PostgreSQL % detectado. NULLS NOT DISTINCT exige a versao 15 ou superior.',
            current_setting('server_version');
    END IF;
END $$;


-- ----------------------------------------------------------------------------
-- PASSO 1 — Métricas de entrada
-- Capturadas antes de qualquer modificação, para as validações do PASSO 6.
--
-- Tabela temporária com escopo de SESSÃO (ON COMMIT PRESERVE ROWS, o padrão) e
-- não de transação. ON COMMIT DROP quebraria a migration em qualquer executor
-- que confirme statement a statement — a tabela desapareceria antes do PASSO 6.
-- Ela é removida explicitamente ao final do PASSO 6.
--
-- O DROP IF EXISTS abaixo é higiene de sessão para o caso de uma tentativa
-- anterior ter abortado após criar a tabela; não mascara divergência de schema,
-- pois _v25_metricas não faz parte do modelo persistente.
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS _v25_metricas;

CREATE TEMP TABLE _v25_metricas (
    chave TEXT PRIMARY KEY,
    valor BIGINT NOT NULL
);

INSERT INTO _v25_metricas (chave, valor)
SELECT 'linhas_antes', COUNT(*) FROM conciliacao_taxas;

INSERT INTO _v25_metricas (chave, valor)
SELECT 'grupos_logicos_antes', COUNT(*)
FROM (
    SELECT 1
    FROM conciliacao_taxas
    GROUP BY estabelecimento_id, data_venda, codigo_adquirente, cod_bandeira,
             codigo_modalidade, codigo_produto, auditada
) g;

-- Soma do estado vigente reconstruído ANTES da migration (Validação C).
-- Multiplicada por 100 e arredondada para caber em BIGINT sem perda de centavos.
INSERT INTO _v25_metricas (chave, valor)
SELECT 'soma_vigente_antes_centavos',
       COALESCE(ROUND(SUM(valor_bruto) * 100)::BIGINT, 0)
FROM (
    SELECT DISTINCT ON (estabelecimento_id, data_venda, codigo_adquirente,
                        cod_bandeira, codigo_modalidade, codigo_produto, auditada)
           valor_bruto
    FROM conciliacao_taxas
    ORDER BY estabelecimento_id, data_venda, codigo_adquirente,
             cod_bandeira, codigo_modalidade, codigo_produto, auditada,
             coletado_em DESC NULLS LAST, id DESC
) v;


-- ----------------------------------------------------------------------------
-- PASSO 2 — Tabela histórica
-- Guarda todas as versões substituídas. NÃO recebe a constraint de chave lógica:
-- sua razão de existir é justamente permitir múltiplas versões da mesma chave.
-- ----------------------------------------------------------------------------
CREATE TABLE conciliacao_taxas_historico (
    id                           UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,

    -- Rastreabilidade: id que a linha possuía na tabela operacional.
    -- Sem FK: a linha original é removida da operacional ao ser arquivada.
    conciliacao_taxa_id_original UUID          NOT NULL,

    estabelecimento_id           UUID          NOT NULL REFERENCES estabelecimentos (id),
    id_conciflex                 VARCHAR(50),
    codigo_empresa               VARCHAR(50),
    data_venda                   DATE,
    adquirente                   VARCHAR(100),
    codigo_adquirente            VARCHAR(20),
    bandeira                     VARCHAR(100),
    cod_bandeira                 VARCHAR(20),
    modalidade                   VARCHAR(100),
    codigo_modalidade            VARCHAR(20),
    produto                      VARCHAR(100),
    codigo_produto               VARCHAR(20),
    valor_bruto                  NUMERIC(15,2),
    valor_desconto               NUMERIC(15,6),
    percentual_taxa              NUMERIC(8,4),
    taxa_contratada              NUMERIC(8,4),
    quantidade                   INTEGER,
    taxa_praticada_rs            NUMERIC(15,2),
    taxa_praticada_cadastrada_rs NUMERIC(15,2),
    taxa_contratada_rs           NUMERIC(15,2),
    total_taxa_nao_contratada_rs NUMERIC(15,2),
    perda_rs                     NUMERIC(15,2),
    perda                        NUMERIC(15,4),
    auditada                     VARCHAR(1),
    estabelecimento_conciflex    VARCHAR(255),
    coletado_em                  TIMESTAMP,

    arquivado_em                 TIMESTAMP     NOT NULL DEFAULT now(),

    -- Motivo modelado como domínio fechado, não texto livre:
    --   MIGRATION_DEDUPLICATION — versões excedentes saneadas por esta migration
    --   UPSERT_VALUE_CHANGE     — versão substituída por uma recoleta com valores alterados
    motivo_arquivamento          VARCHAR(40)   NOT NULL
        CONSTRAINT ck_cth_motivo_arquivamento
        CHECK (motivo_arquivamento IN ('MIGRATION_DEDUPLICATION', 'UPSERT_VALUE_CHANGE'))
);

-- Consulta do histórico por estabelecimento e período de venda
CREATE INDEX idx_cth_estabelecimento_data
    ON conciliacao_taxas_historico (estabelecimento_id, data_venda);

-- Reconstrução da linha do tempo de uma chave lógica específica
CREATE INDEX idx_cth_chave_logica
    ON conciliacao_taxas_historico (estabelecimento_id, data_venda, codigo_adquirente,
                                    cod_bandeira, codigo_modalidade, codigo_produto, auditada);

-- Auditoria por janela de arquivamento
CREATE INDEX idx_cth_arquivado_em
    ON conciliacao_taxas_historico (arquivado_em);


-- ----------------------------------------------------------------------------
-- PASSO 3 — Arquivar e remover as versões excedentes
--
-- Executado como UM ÚNICO statement. As CTEs do PostgreSQL compartilham o mesmo
-- snapshot, então o INSERT enxerga as linhas na íntegra mesmo que o DELETE as
-- remova no mesmo comando. A cópia precede logicamente a remoção — nunca o
-- inverso.
--
-- Desempate: coletado_em DESC NULLS LAST, id DESC.
-- NULLS LAST trata coletado_em nulo como a versão mais antiga.
-- ----------------------------------------------------------------------------
WITH ranked AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY estabelecimento_id, data_venda, codigo_adquirente,
                         cod_bandeira, codigo_modalidade, codigo_produto, auditada
            ORDER BY coletado_em DESC NULLS LAST, id DESC
        ) AS rn
    FROM conciliacao_taxas
),
excedentes AS (
    SELECT id FROM ranked WHERE rn > 1
),
arquivadas AS (
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
        ct.id, ct.estabelecimento_id, ct.id_conciflex, ct.codigo_empresa,
        ct.data_venda, ct.adquirente, ct.codigo_adquirente, ct.bandeira, ct.cod_bandeira,
        ct.modalidade, ct.codigo_modalidade, ct.produto, ct.codigo_produto,
        ct.valor_bruto, ct.valor_desconto, ct.percentual_taxa, ct.taxa_contratada,
        ct.quantidade, ct.taxa_praticada_rs, ct.taxa_praticada_cadastrada_rs,
        ct.taxa_contratada_rs, ct.total_taxa_nao_contratada_rs, ct.perda_rs, ct.perda,
        ct.auditada, ct.estabelecimento_conciflex, ct.coletado_em, 'MIGRATION_DEDUPLICATION'
    FROM conciliacao_taxas ct
    JOIN excedentes e ON e.id = ct.id
    RETURNING 1
)
DELETE FROM conciliacao_taxas
WHERE id IN (SELECT id FROM excedentes);

-- Quantidade arquivada por ESTA migration (a tabela pode receber outras linhas
-- depois, então a contagem é filtrada pelo motivo).
INSERT INTO _v25_metricas (chave, valor)
SELECT 'linhas_arquivadas', COUNT(*)
FROM conciliacao_taxas_historico
WHERE motivo_arquivamento = 'MIGRATION_DEDUPLICATION';


-- ----------------------------------------------------------------------------
-- PASSO 4 — Substituir a constraint
-- Criada somente após o saneamento: com duplicidades presentes, falharia.
-- ----------------------------------------------------------------------------
ALTER TABLE conciliacao_taxas
    DROP CONSTRAINT uq_ct_id_conciflex_estabelecimento;

ALTER TABLE conciliacao_taxas
    ADD CONSTRAINT uq_ct_chave_logica
    UNIQUE NULLS NOT DISTINCT (
        estabelecimento_id,
        data_venda,
        codigo_adquirente,
        cod_bandeira,
        codigo_modalidade,
        codigo_produto,
        auditada
    );

-- idx_ct_id_conciflex (criado em V3) é preservado: id_conciflex continua sendo
-- um campo de rastreabilidade consultável, apenas deixou de ser chave de negócio.


-- ----------------------------------------------------------------------------
-- PASSO 5 — Métricas de saída
-- ----------------------------------------------------------------------------
INSERT INTO _v25_metricas (chave, valor)
SELECT 'linhas_depois', COUNT(*) FROM conciliacao_taxas;

INSERT INTO _v25_metricas (chave, valor)
SELECT 'soma_vigente_depois_centavos',
       COALESCE(ROUND(SUM(valor_bruto) * 100)::BIGINT, 0)
FROM conciliacao_taxas;


-- ----------------------------------------------------------------------------
-- PASSO 6 — Validações
-- Qualquer falha aborta a transação e reverte a migration por completo.
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    v_linhas_antes        BIGINT;
    v_linhas_depois       BIGINT;
    v_grupos_antes        BIGINT;
    v_arquivadas          BIGINT;
    v_soma_antes          BIGINT;
    v_soma_depois         BIGINT;
    v_grupos_depois       BIGINT;
    v_duplicidades        BIGINT;
    v_orfas               BIGINT;
BEGIN
    SELECT valor INTO v_linhas_antes  FROM _v25_metricas WHERE chave = 'linhas_antes';
    SELECT valor INTO v_linhas_depois FROM _v25_metricas WHERE chave = 'linhas_depois';
    SELECT valor INTO v_grupos_antes  FROM _v25_metricas WHERE chave = 'grupos_logicos_antes';
    SELECT valor INTO v_arquivadas    FROM _v25_metricas WHERE chave = 'linhas_arquivadas';
    SELECT valor INTO v_soma_antes    FROM _v25_metricas WHERE chave = 'soma_vigente_antes_centavos';
    SELECT valor INTO v_soma_depois   FROM _v25_metricas WHERE chave = 'soma_vigente_depois_centavos';

    -- VALIDAÇÃO A — uma linha por grupo lógico
    SELECT COUNT(*) INTO v_grupos_depois
    FROM (
        SELECT 1
        FROM conciliacao_taxas
        GROUP BY estabelecimento_id, data_venda, codigo_adquirente, cod_bandeira,
                 codigo_modalidade, codigo_produto, auditada
    ) g;

    IF v_linhas_depois <> v_grupos_depois THEN
        RAISE EXCEPTION
            'V25 Validacao A falhou: % linhas operacionais para % grupos logicos.',
            v_linhas_depois, v_grupos_depois;
    END IF;

    IF v_linhas_depois <> v_grupos_antes THEN
        RAISE EXCEPTION
            'V25 Validacao A falhou: % linhas apos o saneamento, mas % grupos logicos existiam antes.',
            v_linhas_depois, v_grupos_antes;
    END IF;

    -- VALIDAÇÃO B — nenhuma duplicidade remanescente
    SELECT COUNT(*) INTO v_duplicidades
    FROM (
        SELECT 1
        FROM conciliacao_taxas
        GROUP BY estabelecimento_id, data_venda, codigo_adquirente, cod_bandeira,
                 codigo_modalidade, codigo_produto, auditada
        HAVING COUNT(*) > 1
    ) d;

    IF v_duplicidades > 0 THEN
        RAISE EXCEPTION 'V25 Validacao B falhou: % grupos ainda duplicados.', v_duplicidades;
    END IF;

    -- VALIDAÇÃO C — a soma do estado vigente é preservada
    IF v_soma_antes <> v_soma_depois THEN
        RAISE EXCEPTION
            'V25 Validacao C falhou: soma vigente antes = % centavos, depois = % centavos (diferenca de % centavos).',
            v_soma_antes, v_soma_depois, (v_soma_depois - v_soma_antes);
    END IF;

    -- VALIDAÇÃO D — conservação de linhas
    IF v_linhas_antes <> (v_linhas_depois + v_arquivadas) THEN
        RAISE EXCEPTION
            'V25 Validacao D falhou: % linhas antes <> % operacionais + % arquivadas.',
            v_linhas_antes, v_linhas_depois, v_arquivadas;
    END IF;

    -- VALIDAÇÃO E — integridade referencial
    -- Nenhuma FK aponta para conciliacao_taxas.id (auditado). Resta validar que
    -- toda linha arquivada referencia um estabelecimento existente.
    SELECT COUNT(*) INTO v_orfas
    FROM conciliacao_taxas_historico h
    WHERE NOT EXISTS (
        SELECT 1 FROM estabelecimentos e WHERE e.id = h.estabelecimento_id
    );

    IF v_orfas > 0 THEN
        RAISE EXCEPTION 'V25 Validacao E falhou: % linhas historicas sem estabelecimento.', v_orfas;
    END IF;

    RAISE NOTICE
        'V25 concluida: % linhas -> % operacionais + % arquivadas | soma vigente preservada em % centavos.',
        v_linhas_antes, v_linhas_depois, v_arquivadas, v_soma_depois;
END $$;


-- ----------------------------------------------------------------------------
-- PASSO 7 — Limpeza
-- Removida somente após todas as validações passarem.
-- ----------------------------------------------------------------------------
DROP TABLE _v25_metricas;
