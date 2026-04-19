CREATE TABLE conciliacao_taxas (
    id                           UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    estabelecimento_id           UUID           NOT NULL REFERENCES estabelecimentos (id),
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
    auditada                     CHAR(1),
    estabelecimento_conciflex    VARCHAR(255),
    coletado_em                  TIMESTAMP
);

CREATE INDEX idx_ct_estabelecimento_data ON conciliacao_taxas (estabelecimento_id, data_venda);
CREATE INDEX idx_ct_bandeira             ON conciliacao_taxas (bandeira);
CREATE INDEX idx_ct_id_conciflex        ON conciliacao_taxas (id_conciflex);
