CREATE TABLE template_variaveis (
    id           BIGSERIAL    PRIMARY KEY,
    chave        VARCHAR(100) NOT NULL UNIQUE,
    descricao    VARCHAR(255) NOT NULL,
    sistema_fixo BOOLEAN      NOT NULL DEFAULT false,
    template_id  BIGINT       REFERENCES templates(id) ON DELETE CASCADE
);

CREATE INDEX idx_tv_template_id ON template_variaveis(template_id);
