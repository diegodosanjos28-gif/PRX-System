-- The original global UNIQUE on chave prevents two templates from using the same
-- placeholder name (e.g. {nomeFantasia}). Replace it with two scoped constraints:
--
--  1. uq_tv_template_chave   — chave is unique within a given template
--  2. uq_tv_chave_global     — chave is unique among global/system variables (template_id IS NULL)
--                              implemented as a partial index because PostgreSQL treats NULLs as
--                              distinct in composite UNIQUE constraints, so option 1 alone
--                              would not protect the global catalog rows.

ALTER TABLE template_variaveis DROP CONSTRAINT template_variaveis_chave_key;

ALTER TABLE template_variaveis
    ADD CONSTRAINT uq_tv_template_chave UNIQUE (template_id, chave);

CREATE UNIQUE INDEX uq_tv_chave_global
    ON template_variaveis (chave)
    WHERE template_id IS NULL;
