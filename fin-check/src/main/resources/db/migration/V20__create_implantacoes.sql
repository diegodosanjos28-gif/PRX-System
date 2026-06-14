-- ============================================================
-- V20 — Módulo Derby: Implantações de Clientes
-- Vinculado a clientes.id (NÃO a estabelecimentos.id).
-- Uma implantação por cliente (unique em cliente_id).
--
-- ATENÇÃO: Este arquivo foi sincronizado com a estrutura
-- já aplicada no banco em 2026-06-12 00:02:09.
-- Campos visuais (coat, silk, numero_pista) e concluida_em
-- serão adicionados em V21 via ALTER TABLE.
-- ============================================================

-- ============================================================
-- TABELA 1: implantacoes_cliente
-- Representa o processo de implantação de um cliente no PRX.
-- Cada linha é uma "corrida" — o cliente percorre as etapas
-- pre → corrida → onboarding e ao concluir vai para o curral.
-- ============================================================
CREATE TABLE implantacoes_cliente (

    id                  UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,

    -- Vínculo com o cliente (1 implantação por cliente)
    cliente_id          UUID        NOT NULL
        REFERENCES clientes (id) ON DELETE CASCADE,

    -- Etapa atual na pista Derby
    -- 'pre'        = Pré-Largada (coleta de dados, acessos, cadastro)
    -- 'corrida'    = Corrida de Implantação (cartas, terceiros)
    -- 'onboarding' = Reta final — apresentação do sistema
    -- 'curral'     = Concluído / arquivado
    etapa               VARCHAR(20) NOT NULL DEFAULT 'pre'
        CONSTRAINT chk_etapa CHECK (etapa IN ('pre', 'corrida', 'onboarding', 'curral')),

    -- Status operacional
    -- Quando etapa = 'curral': status DEVE ser NULL
    -- Quando etapa != 'curral': status obrigatório nos valores abaixo
    status              VARCHAR(20)
        CONSTRAINT chk_status CHECK (
            (etapa = 'curral'  AND status IS NULL)
            OR
            (etapa != 'curral' AND status IN ('fluindo', 'aguardando', 'travado'))
        ),

    -- Responsável interno da PRX pela implantação (ex: Bianca, Diego, Cesar)
    responsavel         VARCHAR(100),

    -- Nome/contato do lado do cliente
    dono_contato        VARCHAR(255),

    -- JSONB: lista de adquirentes vinculadas ao cliente
    -- Exemplo: ["Stone", "Cielo", "Alelo"]
    adquirentes         JSONB       NOT NULL DEFAULT '[]',

    -- Data em que o cliente chegou ao curral (conclusão)
    data_entrada_curral DATE,

    -- Data/hora em que a etapa atual foi iniciada
    etapa_iniciada_em   TIMESTAMP,

    -- Observações internas sobre a implantação
    observacoes         TEXT,

    -- JSONB: progresso dos checklists por etapa
    -- Estrutura: { "pre": [...microetapas], "corrida": [...], "onboarding": [...] }
    -- Cada microetapa: { "title": "...", "items": [{ "title": "...", "done": false }] }
    progress_json       JSONB,

    -- Último evento registrado (avanço de checklist, nova demanda, etc.)
    ultimo_movimento    TIMESTAMP,

    created_at          TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP   NOT NULL DEFAULT NOW()

);

-- 1 implantação por cliente (sem restrição parcial — histórico via campo curral)
ALTER TABLE implantacoes_cliente
    ADD CONSTRAINT uq_implantacao_cliente UNIQUE (cliente_id);

-- Índices de busca frequente
CREATE INDEX idx_implantacao_cliente_id  ON implantacoes_cliente (cliente_id);
CREATE INDEX idx_implantacao_etapa       ON implantacoes_cliente (etapa);
CREATE INDEX idx_implantacao_status      ON implantacoes_cliente (status);


-- ============================================================
-- TABELA 2: implantacao_demandas
-- Demandas extras vinculadas a uma implantação.
-- Equivale ao extraDemandas[] do Derby HTML persistido no banco.
-- tipo = 'pista'  → demanda da corrida principal
-- tipo = 'curral' → demanda pós-implantação (acompanhamento)
-- ============================================================
CREATE TABLE implantacao_demandas (

    id                  UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,

    -- Implantação à qual esta demanda pertence
    implantacao_id      UUID        NOT NULL
        REFERENCES implantacoes_cliente (id) ON DELETE CASCADE,

    -- Descrição da demanda (ex: "Cliente precisa assinar carta Stone")
    descricao           TEXT        NOT NULL,

    -- Controle de conclusão
    concluida           BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Adquirente relacionada, se aplicável (ex: "Stone", "Cielo")
    adquirente          VARCHAR(100),

    -- Nível de prioridade
    prioridade          VARCHAR(20) NOT NULL DEFAULT 'media'
        CONSTRAINT chk_prioridade CHECK (prioridade IN ('critica', 'alta', 'media', 'baixa')),

    -- Categoria da demanda
    -- 'pista'  = demanda ativa na corrida de implantação
    -- 'curral' = demanda de acompanhamento pós-implantação
    tipo                VARCHAR(20) NOT NULL DEFAULT 'pista'
        CONSTRAINT chk_tipo CHECK (tipo IN ('pista', 'curral')),

    created_at          TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP   NOT NULL DEFAULT NOW()

);

-- Índices para listagem e filtros operacionais
CREATE INDEX idx_demanda_implantacao_id          ON implantacao_demandas (implantacao_id);
CREATE INDEX idx_demanda_implantacao_concluida   ON implantacao_demandas (implantacao_id, concluida);
CREATE INDEX idx_demanda_concluida               ON implantacao_demandas (concluida);
CREATE INDEX idx_demanda_tipo                    ON implantacao_demandas (tipo);
