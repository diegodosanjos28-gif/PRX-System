CREATE TABLE resumo_coleta (
    id                  UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    estabelecimento_id  UUID           NOT NULL REFERENCES estabelecimentos (id),
    tipo                VARCHAR(30)    NOT NULL CHECK (tipo IN ('conciliacao_taxas', 'recebimentos')),
    data_inicio         DATE,
    data_fim            DATE,
    total_registros     INTEGER,
    valor_bruto_total   NUMERIC(18,4),
    valor_liquido_total NUMERIC(18,6),
    total_taxas         NUMERIC(15,4),
    totalizadores_json  TEXT,
    coletado_em         TIMESTAMP
);

CREATE INDEX idx_resumo_estabelecimento ON resumo_coleta (estabelecimento_id);
CREATE INDEX idx_resumo_tipo            ON resumo_coleta (tipo);
