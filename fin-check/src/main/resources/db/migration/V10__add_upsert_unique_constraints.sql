-- Constraints únicas necessárias para UPSERT idempotente no microserviço coletor
-- ON CONFLICT (id_conciflex, estabelecimento_id) requer estas constraints

ALTER TABLE conciliacao_taxas
    ADD CONSTRAINT uq_ct_id_conciflex_estabelecimento
    UNIQUE (id_conciflex, estabelecimento_id);

ALTER TABLE recebimentos
    ADD CONSTRAINT uq_rec_id_conciflex_estabelecimento
    UNIQUE (id_conciflex, estabelecimento_id);
