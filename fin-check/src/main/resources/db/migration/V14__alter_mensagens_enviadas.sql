ALTER TABLE mensagens_enviadas
    ADD COLUMN estabelecimento_id UUID        REFERENCES estabelecimentos(id),
    ADD COLUMN template_id        BIGINT      REFERENCES templates(id),
    ADD COLUMN template_nome      VARCHAR(255),
    ADD COLUMN modo_geracao_real  VARCHAR(10);

CREATE INDEX idx_msg_estabelecimento_id ON mensagens_enviadas(estabelecimento_id);
