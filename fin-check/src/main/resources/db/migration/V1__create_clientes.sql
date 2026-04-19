CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE clientes (
    id               UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    razao_social     VARCHAR(255) NOT NULL,
    nome_fantasia    VARCHAR(255),
    cnpj             CHAR(14)     NOT NULL UNIQUE,
    whatsapp         VARCHAR(20)  NOT NULL,
    conciflex_login  TEXT         NOT NULL,
    conciflex_senha  TEXT         NOT NULL,
    ativo            BOOLEAN      NOT NULL DEFAULT TRUE,
    observacoes      TEXT,
    criado_em        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_clientes_cnpj  ON clientes (cnpj);
CREATE INDEX idx_clientes_ativo ON clientes (ativo);
