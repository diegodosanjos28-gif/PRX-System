CREATE TABLE estabelecimentos (
    id                       UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    cliente_id               UUID         NOT NULL REFERENCES clientes (id) ON DELETE CASCADE,
    descricao                VARCHAR(255) NOT NULL,
    identificador_conciflex  VARCHAR(255) NOT NULL,
    ativo                    BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_estab_cliente_id ON estabelecimentos (cliente_id);
CREATE INDEX idx_estab_ativo      ON estabelecimentos (ativo);
