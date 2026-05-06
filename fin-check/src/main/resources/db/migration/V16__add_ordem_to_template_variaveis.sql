-- Add column nullable first so the backfill can run before enforcing NOT NULL
ALTER TABLE template_variaveis ADD COLUMN ordem INT;

-- Assign sequential 1-based order within each template group.
-- Global variables (template_id IS NULL) are each treated as their own partition
-- by ROW_NUMBER, so they receive 1..N among themselves.
UPDATE template_variaveis tv
SET ordem = sub.rn
FROM (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY template_id ORDER BY id) AS rn
    FROM template_variaveis
) sub
WHERE tv.id = sub.id;

ALTER TABLE template_variaveis ALTER COLUMN ordem SET NOT NULL;

-- Unique constraint per (template_id, ordem).
-- PostgreSQL does NOT consider two NULLs equal in a unique constraint,
-- so global rows (template_id IS NULL) are not constrained against each other.
ALTER TABLE template_variaveis
    ADD CONSTRAINT uq_tv_template_ordem UNIQUE (template_id, ordem);
