-- Hibernate valida CHAR(n) como bpchar mas espera varchar — altera os três campos problemáticos
ALTER TABLE clientes            ALTER COLUMN cnpj               TYPE VARCHAR(14);
ALTER TABLE conciliacao_taxas   ALTER COLUMN auditada           TYPE VARCHAR(1);
ALTER TABLE recebimentos        ALTER COLUMN possui_taxa_minima TYPE VARCHAR(1);
