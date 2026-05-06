-- Templates now manage their own variables independently. The same placeholder
-- name may appear in multiple templates and also mirror a global catalog entry —
-- duplicates across templates and between template-specific and global rows are
-- intentional and expected. Only ordem uniqueness within a template is kept.

ALTER TABLE template_variaveis DROP CONSTRAINT IF EXISTS uq_tv_template_chave;
DROP INDEX IF EXISTS uq_tv_chave_global;
