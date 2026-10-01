-- ============================================================================
-- V26 — Classe comercial do cliente implantado
-- ============================================================================
--
-- Classifica a relevância/rentabilidade comercial do cliente para a PRX. É uma
-- dimensão INDEPENDENTE da etapa da implantação e da saúde operacional: um
-- cliente pode ser Classe Prime e estar em intervenção urgente, ou Classe Bronze
-- e estar saudável. Nada aqui altera 'etapa', 'status' ou as demandas.
--
-- NOMENCLATURA
-- Os quatro valores são exatamente PRIME, GOLD, PLATIUM e BRONZE. 'PLATIUM' é a
-- grafia adotada pelo negócio e NÃO deve ser "corrigida" para 'PLATINUM'.
--
-- NULLABLE, DELIBERADAMENTE
-- Existem clientes já no Curral sem classe definida. A coluna é NULL-ável e esta
-- migration NÃO executa nenhum UPDATE: classificar comercialmente um cliente é
-- decisão de negócio e será feita pelo operador na tela "Editar Implantação".
-- Atribuir automaticamente qualquer classe aos registros existentes seria inventar
-- uma informação comercial que ninguém forneceu.
--
-- O CHECK aceita NULL — em PostgreSQL uma CHECK constraint só reprova quando a
-- expressão avalia FALSE; com NULL ela avalia NULL e a linha é aceita. Assim os
-- registros legados permanecem válidos sem qualquer tratamento especial.
-- ============================================================================

ALTER TABLE implantacoes_cliente
    ADD COLUMN classe VARCHAR(20) NULL;

ALTER TABLE implantacoes_cliente
    ADD CONSTRAINT chk_classe
    CHECK (classe IN ('PRIME', 'GOLD', 'PLATIUM', 'BRONZE'));

-- Setores do Curral são montados por classe; o índice serve a essa leitura.
CREATE INDEX idx_implantacao_classe ON implantacoes_cliente (classe);

COMMENT ON COLUMN implantacoes_cliente.classe IS
    'Classe comercial: PRIME, GOLD, PLATIUM ou BRONZE. NULL = ainda não classificado.';
