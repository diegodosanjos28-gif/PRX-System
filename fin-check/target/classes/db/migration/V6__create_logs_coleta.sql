CREATE TABLE logs_coleta (
    id                    UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    estabelecimento_id    UUID         NOT NULL REFERENCES estabelecimentos (id),
    executado_em          TIMESTAMP    NOT NULL DEFAULT NOW(),
    status                VARCHAR(20)  NOT NULL CHECK (status IN ('success', 'login_failed', 'timeout', 'error')),
    registros_coletados   INTEGER,
    mensagem_erro         TEXT
);

CREATE INDEX idx_log_estabelecimento ON logs_coleta (estabelecimento_id);
CREATE INDEX idx_log_status          ON logs_coleta (status);
CREATE INDEX idx_log_executado_em    ON logs_coleta (executado_em DESC);
