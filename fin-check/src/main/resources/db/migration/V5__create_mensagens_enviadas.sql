CREATE TABLE mensagens_enviadas (
    id               UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    cliente_id       UUID         NOT NULL REFERENCES clientes (id),
    conteudo         TEXT         NOT NULL,
    modo_geracao     VARCHAR(10)  NOT NULL CHECK (modo_geracao IN ('ia', 'template')),
    meta_message_id  VARCHAR(100),
    status_entrega   VARCHAR(20)  CHECK (status_entrega IN ('sent', 'delivered', 'read', 'failed')),
    enviado_em       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_msg_cliente_id ON mensagens_enviadas (cliente_id);
CREATE INDEX idx_msg_meta_id    ON mensagens_enviadas (meta_message_id);
